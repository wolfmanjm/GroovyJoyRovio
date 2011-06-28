package com.e4net.rovio
import groovyx.net.http.HTTPBuilder

/**
 * grab a jpeg buffer
 */

url= 'http://rovio'
user= 'morris'
password= 'qaz1xsw'
http= new HTTPBuilder(url)
http.auth.basic user, password

println http.get(path: "Rev.cgi", query: [Cmd: 'nav', action: '19', 'LIGHT': "1"])

/*
http.parser.'image/jpeg' = { resp ->
	len= resp.headers.'Content-Length'.toInteger()
	is= resp.getEntity().getContent()
	byte [] buf= new byte[len]
	int off= 0
	while (off < len) {
		cnt= is.read(buf, off, len-off)
		if(cnt < 0) throw new IOException("early end of stream")
		off += cnt
	}
	return buf
}

jpeg= http.get(path: '/files/IMG_0332.JPG')
println jpeg.class
println jpeg.length
println jpeg

*/