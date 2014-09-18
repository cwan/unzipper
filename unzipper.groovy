import jp.hishidama.zip.*

// 定数
CONSTS = [
	ENCODING : [
		// ZIPファイル名のエンコード（Windows日本語環境を想定）
		FILE_NAME : 'MS932',
		// ZIPファイルパスワードのエンコード（Windows日本語環境を想定）
		PASSWORD : 'MS932'
	]
]

init()

this.rules.each {
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
	
	this.expandedRules = new HashSet()

	def today = new Date()
	def dates = [ today, today.plus(1), today.minus(1), today.minus(2), today.minus(3) ]
	
	def matcher = null

	if ((matcher = (this.sourceZipFile.name =~ /.*(\d{8}).*/)).matches()) {
		dates.add Date.parse('yyyyMMdd', matcher[0][1])
	} else if ((matcher = (this.sourceZipFile.name =~ /.*(\d{6}).*/)).matches()) {
		dates.add Date.parse('yyMMdd', matcher[0][1])
	} else if ((matcher = (this.sourceZipFile.name =~ /.*(\d{4}).*/)).matches()) {
		dates.add Date.parse('yyyyMMdd', new Date().format('yyyy') + matcher[0][1])
	}

	this.rules.each { rule ->
		def ruleMatcher = rule =~ /.*(<.+>).*/

		if (ruleMatcher.matches()) {
			def format = ruleMatcher[0][1].replaceFirst(/<(.+)>/, { it[1] })
			println "format: $format"

			dates.each { date ->
				this.expandedRules.add rule.replaceFirst(/<.+>/, date.format(format))
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

	def zipFile = new ZipFile(this.sourceZipFile, CONSTS.ENCODING.FILE_NAME)

	try {

		// 解凍パスワード
		if (!password.empty) {
			zipFile.password = password.getBytes(CONSTS.ENCODING.PASSWORD)
		}

		// CRCチェック
		zipFile.checkCrc = true

		zipFile.entries.each { zipEntry ->
			def entryPath = zipEntry.name
			//println "entry: <$entryPath>"

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
