/* Copyright 2011 Florian Schulz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */
package de.hanbei.httpserver.request;

import de.hanbei.httpserver.common.Cookie;
import de.hanbei.httpserver.common.Header;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeaderTest {

	private Header header;

	@Before
	public void setUp() throws Exception {
		this.header = new Header();
		this.header.addParameter("Accept", new Parameter("text/xml"));
		this.header.addParameter("Accept", new Parameter("text/html", 0.1));

		this.header.addParameter("Accept-Language", new Parameter("en", 0.1));
		this.header
				.addParameter("Accept-Language", new Parameter("en-us", 0.5));

		this.header.addParameter("Accept-Encoding", new Parameter("gzip"));
		this.header.addParameter("Accept-Encoding", new Parameter("deflate"));

		this.header.addParameter("Accept-Charset", "ISO-8859-1");
		this.header.addParameter("Accept-Charset", "utf-8", 0.7);
		this.header.addParameter("Accept-Charset", "*", 0.7);

	}

	@Test
	public void testToString() {
		assertEquals("Accept-Language: en;q=0.1,en-us;q=0.5\n"
				+ "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\n"
				+ "Accept-Encoding: gzip,deflate\n"
				+ "Accept: text/xml,text/html;q=0.1\n", header.toString());
	}

	@Test
	public void testToStringWithCookie() {
		this.header.addCookie(new Cookie("test", "test"));
		assertEquals("Accept-Language: en;q=0.1,en-us;q=0.5\n"
				+ "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\n"
				+ "Accept-Encoding: gzip,deflate\n"
				+ "Accept: text/xml,text/html;q=0.1\n" + "Cookie: test=test\n",
				header.toString());
	}
}
