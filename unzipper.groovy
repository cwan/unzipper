import jp.hishidama.zip.*

/** ZIPファイル名のエンコード（Windows日本語環境を想定） */
ZIP_FILE_NAME_ENCODING = 'MS932'

/** ZIPファイルパスワードのエンコード（Windows日本語環境を想定） */
ZIP_FILE_PASSWORD_ENCODING = 'MS932'


init()

_rules.each {
	unzip it
}

/**
 * 初期処理
 */
void init() {
	_sourceZipFile = new File(args[0])
	_ruleFile = new File(args[1])

	println "Zip file: <${_sourceZipFile.absolutePath}>"
	println "Rule file: <${_ruleFile.absolutePath}>"

	if (!_sourceZipFile.exists() || !_sourceZipFile.file) {
		println "File does not exist: <${_sourceZipFile.absolutePath}>"
		System.exit 1
	}

	_rules = (_ruleFile.exists() && _ruleFile.file) ?
				_ruleFile.readLines() : []

	println "Rules: ${_rules}"

	_rules.push ''
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

	def zipFile = new ZipFile(_sourceZipFile, ZIP_FILE_NAME_ENCODING)

	try {

		// 解凍パスワード
		if (!password.empty) {
			zipFile.setPassword password.getBytes(ZIP_FILE_PASSWORD_ENCODING)
		}

		// CRCチェック
		zipFile.setCheckCrc true

		zipFile.entries.each { zipEntry ->
			def entryPath = zipEntry.name
			println "entry: <$entryPath>"
		}

	} finally {
		zipFile.close()
	}
}
