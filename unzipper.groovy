def zipFile = new File(args[0])

println "Zip file: <${zipFile.absolutePath}>"

if (!zipFile.exists() || !zipFile.file) {
	println "File does not exist: <${args[0]}>"
	System.exit 1
}


