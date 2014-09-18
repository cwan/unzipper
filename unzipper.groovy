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

/**
 * 初期処理
 */
void init() {
	this.sourceZipFile = new File(args[0])
	this.ruleFile = new File(args[1])

	println "Zip file: <${this.sourceZipFile.absolutePath}>"
	println "Rule file: <${this.ruleFile.absolutePath}>"

	if (!this.sourceZipFile.exists() || !this.sourceZipFile.file) {
		println "File does not exist: <${this.sourceZipFile.absolutePath}>"
		System.exit 1
	}

	this.rules = (this.ruleFile.exists() && this.ruleFile.file) ?
				this.ruleFile.readLines() : []

	println "Rules: ${this.rules}"

	this.rules.push ''
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
