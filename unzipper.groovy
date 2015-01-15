import jp.hishidama.zip.*
import javax.swing.JOptionPane

// 定数
C = [
	ENC : [
		// ZIPファイル名のエンコード（Windows日本語環境を想定）
		FILE_NAME : 'MS932',
		// ZIPファイルパスワードのエンコード（Windows日本語環境を想定）
		PASSWORD : 'MS932'
	]
]

init()

makeDestDir()
println "destDir : ${this.destDir.absolutePath}"

this.completed = false

this.expandedRules.find { password ->
	try {
		unzip password
		println "Succeed in unzipping: password=${password}"
		JOptionPane.showMessageDialog null, "Succeed in unzipping: password=${password}"
		
	} catch (e) {
		println "Failed in unzipping: password=${password}, message=${e.message}"
	}

	// 解凍が成功するまで繰り返す
	return this.completed
}

if (!this.completed) {
	// 全て失敗した場合、解凍先ディレクトリを削除
	this.destDir.deleteDir()
	JOptionPane.showMessageDialog null, "Could not unzip a file."
}

/** 初期処理 */
void init() {
	this.srcZipFile = new File(args[0])
	this.ruleFile = new File(args[1])

	println "Zip file: <${this.srcZipFile.absolutePath}>"
	println "Rule file: <${this.ruleFile.absolutePath}>"

	assert this.srcZipFile.file, "File does not exist: <${this.srcZipFile.absolutePath}>"

	this.rules = [ '' ]	// パスワード無し用のルール

	// ルールファイル読み込み
	if (this.ruleFile.file) {
		this.rules.addAll this.ruleFile.readLines()
	}

//	println "Rules: ${this.rules}"

	expandRules()
}

/** 展開先のディレクトリを作成する */
void makeDestDir() {

	def parentDir = this.srcZipFile.absoluteFile.parentFile
	this.baseName = this.srcZipFile.name.replaceFirst(~/\.[^\.]*$/, '')

	// zipファイルのベース名と同じディレクトリを作成する
	this.destDir = new File(parentDir, this.baseName)

	if (!this.destDir.exists() && !this.destDir.directory) {
		this.destDir.mkdir()
		return
	}

	this.destDir = null

	// 既存のディレクトリがある場合は、サフィックス付きのディレクトリを作成する
	(1..10).find {
		def dir = new File(parentDir, this.baseName + " (${it})")

		if (!dir.exists() && !dir.directory) {
			dir.mkdir()
			this.destDir = dir
			return true
		}
	}

	assert this.destDir != null, 'Cound not make a destination directory.'
}

/** パスワードルールの展開 */
void expandRules() {
	
	this.expandedRules = new LinkedHashSet()

	// 本日と前後の日付
	def today = new Date()
	def dates = [ today, today.plus(1), today.minus(1), today.minus(2), today.minus(3) ]
	
	// ファイル名に4,6,8桁の数字が含まれる場合、日付とみなす
	def matcher = this.srcZipFile.name =~ /.*?(\d{8}|\d{6}|\d{4}).*?/

	if (matcher.matches()) {
		def sDate = matcher[0][1]
		def targetDate = null

		if (sDate.length() == 8) {
			targetDate = Date.parse('yyyyMMdd', sDate)
		} else if (sDate.length() == 6) {
			targetDate = Date.parse('yyMMdd', sDate)
		} else if (sDate.length() == 4) {
			targetDate = Date.parse('MMdd', sDate)
			targetDate.set(year: new Date()[Calendar.YEAR])
		}

		if (targetDate != null) {
			dates.add targetDate
			dates.add targetDate.plus(1)
			dates.add targetDate.plus(2)
			dates.add targetDate.plus(3)
		}
	}

	this.rules.each { rule ->
		// ルール内に <～> が含まれる場合は、日付フォーマットとみなして置換する
		def ruleMatcher = rule =~ /.*(<.+>).*/

		if (ruleMatcher.matches()) {
			def fmt = ruleMatcher[0][1].replaceFirst(/<(.+)>/, { it[1] })

			dates.each { date ->
				this.expandedRules.add rule.replaceFirst(/<.+>/, date.format(fmt))
			}
		} else {
			this.expandedRules.add rule
		}
	}

//	println "expandedRules : ${this.expandedRules}"
}

/**
 * zipファイルを解凍する。
 *
 * @param password パスワード
 * @throws ZipCrcException CRC不一致にスローされる
 * @throws ZipException パスワード不一致等にスローされる
 * @throws IOException ファイル破損等にスローされる
 * @return 出力先ルートパス
 */
def unzip(password) {

	def zipFile = new ZipFile(this.srcZipFile, C.ENC.FILE_NAME)

	try {

		// 解凍パスワード
		if (!password.empty) {
			zipFile.password = password.getBytes(C.ENC.PASSWORD)
		}

		// CRCチェック
		zipFile.checkCrc = true

		// 最上位ディレクトリを作るかどうかチェック
		def needsTopDir = false

		zipFile.entries.find { e ->
			if (this.baseName != e.name.split('/')[0]) {
				needsTopDir = true
				return true
			}
		}

//		println "needsTopDir: $needsTopDir"

		// 展開処理
		zipFile.entries.each { e ->
			def path = e.name

			if (!needsTopDir) {
				path = path.substring(this.baseName.length() + 1)
			}

//			println "entryPath: <$path>"

			if (e.directory) {
				// ディレクトリエントリの場合
				if (!path.empty) new File(this.destDir, path).mkdirs()
				return
			}

			// ファイルエントリの場合
			def names = path.split('/')
			def fileName = names[names.size() - 1]
//			println "fileName: ${fileName}"

			if (names.size() > 1) {
				def dirName = names.take(names.size() - 1).join('/')
//				println "dirName: ${dirName}"
				new File(this.destDir, dirName).mkdirs()
			}

			def inStream = zipFile.getInputStream(e)

			try {
				def file = new File(this.destDir, path)
				file.append inStream
			} finally {
				if (inStream != null) inStream.close()
			}
		}

		this.completed = true

	} finally {
		zipFile.close()
	}
}
