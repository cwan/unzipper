import jp.hishidama.zip.*

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

this.expandedRules.each {
	unzip it
}

/** 初期処理 */
void init() {
	this.sourceZipFile = new File(args[0])
	this.ruleFile = new File(args[1])

	println "Zip file: <${this.sourceZipFile.absolutePath}>"
	println "Rule file: <${this.ruleFile.absolutePath}>"

	assert (this.sourceZipFile.exists() && this.sourceZipFile.file),
			"File does not exist: <${this.sourceZipFile.absolutePath}>"

	this.rules = (this.ruleFile.exists() && this.ruleFile.file) ?
				this.ruleFile.readLines() : []

	println "Rules: ${this.rules}"

	this.rules.push ''

	expandRules()
}

/** パスワードルールの展開 */
void expandRules() {
	
	this.expandedRules = new LinkedHashSet()

	// 本日と前後の日付
	def today = new Date()
	def dates = [ today, today.plus(1), today.minus(1), today.minus(2), today.minus(3) ]
	
	// ファイル名に4,6,8桁の数字が含まれる場合、日付とみなす
	def matcher = this.sourceZipFile.name =~ /.*?(\d{8}|\d{6}|\d{4}).*?/

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

	println "expandedRules : ${this.expandedRules}"
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

	def zipFile = new ZipFile(this.sourceZipFile, C.ENC.FILE_NAME)

	try {

		// 解凍パスワード
		if (!password.empty) {
			zipFile.password = password.getBytes(C.ENC.PASSWORD)
		}

		// CRCチェック
		zipFile.checkCrc = true

		zipFile.entries.each { zipEntry ->
			def entryPath = zipEntry.name
			println "entry: <$entryPath>"

			// TODO
			//zipEntry.inputStream.eachByte(8192) { b ->

			//}
			// File#appendが使えるか
			//http://groovy.codehaus.org/groovy-jdk/java/io/File.html#append(java.io.InputStream)
		}

	} finally {
		zipFile.close()
	}
}
