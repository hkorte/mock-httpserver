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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import de.hanbei.httpserver.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hanbei.httpserver.exceptions.RequestParseException;

/**
 * Parses a request from an inputstream and returns a {@link Request} object.
 */
public class RequestParser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(RequestParser.class);

    private static final String CONTENT = "Content";

    public Request parse(InputStream in) {
        Request request = new Request();
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            int bytesRead;
            while (in.available() > 0 && (bytesRead = in.read(buffer)) != -1) { // NOSONAR
                bytesOut.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RequestParseException("", e);
        }
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut
                .toByteArray());

        try {
            parseRequest(bytesIn, request);
            parseHeader(bytesIn, request);
            parseContent(bytesIn, request);
        } catch (IOException e) {
            throw new RequestParseException(e);
        } catch (URISyntaxException e) {
            throw new RequestParseException(e);
        }
        return request;
    }

    private void parseContent(ByteArrayInputStream bytesIn, Request request)
            throws IOException {
        Content content = request.getContent();
        if (content.getLength() <= 0) {
            return;
        }
        byte[] contentBytes = new byte[content.getLength()];
        // if (bytesIn.available() > 0) {
        int readBytes = bytesIn.read(contentBytes);
        if (readBytes != contentBytes.length) {
            throw new RequestParseException("");
        }
        content.setContent(contentBytes);
        // }
    }

    private void parseRequest(InputStream in, Request request)
            throws IOException, URISyntaxException {
        StringBuffer line = new StringBuffer();
        readLine(in, line);
        if (line.length() == 0) {
            return;
        }
        LOGGER.trace(line.toString());
        StringTokenizer tokenizer = new StringTokenizer(line.toString());
        request.setMethod(Method.valueOf(tokenizer.nextToken()));
        request.setRequestUri(new URI(tokenizer.nextToken()));

        String httpVersionString = tokenizer.nextToken();
        request.setVersion(HTTPVersion.parseString(httpVersionString
                .substring(httpVersionString.indexOf('/') + 1)));
    }

//    private void parseHost(ByteArrayInputStream in, Request request)
//            throws IOException, URISyntaxException {
//        StringBuffer line = new StringBuffer();
//        readLine(in, line);
//        if (line.length() == 0) {
//            return;
//        }
//        LOGGER.trace(line.toString());
//        StringTokenizer tokenizer = new StringTokenizer(line.toString());
//        tokenizer.nextToken();
//        String host = tokenizer.nextToken();
//        request.setHost(new URI(host));
//    }

    private void parseHeader(ByteArrayInputStream in, Request request)
            throws IOException {
        Header header = new Header();
        StringBuffer buffer = new StringBuffer();
        readLine(in, buffer);
        String line = buffer.toString();
        while (!line.isEmpty()) {
            if (line.startsWith(Header.Fields.COOKIE)) {
                parseCookie(line, header);
            } else if (line.startsWith(CONTENT)) {
                parseContentHeader(line, request);
            } else {
                parseHeaderField(line, header);
            }
            readLine(in, buffer);
            line = buffer.toString();
        }
        // old_parse(in, request, header, buffer, line);
        request.setHeader(header);
    }

    private void parseContentHeader(String line, Request request) {
        Content content = request.getContent();
        String[] parameterSplit = line.split(":");
        if (parameterSplit.length == 2) {
            String fieldName = parameterSplit[0].trim();

            if (Header.Fields.CONTENT_ENCODING.equals(fieldName)) {
                content.setEncoding(parameterSplit[1].trim());
            } else if (Header.Fields.CONTENT_LANGUAGE.equals(fieldName)) {
                content.setLanguage(parameterSplit[1].trim());
            } else if (Header.Fields.CONTENT_LENGTH.equals(fieldName)) {
                content.setLength(Integer.parseInt(parameterSplit[1].trim()));
            } else if (Header.Fields.CONTENT_LOCATION.equals(fieldName)) {
                content.setLocation(URI.create(parameterSplit[1].trim()));
            } else if (Header.Fields.CONTENT_MD5.equals(fieldName)) {
                content.setMd5(parameterSplit[1].trim());
            } else if (Header.Fields.CONTENT_RANGE.equals(fieldName)) {
                content.setRange(parameterSplit[1].trim());
            } else if (Header.Fields.CONTENT_RANGE.equals(fieldName)) {
                content.setRange(parameterSplit[1].trim());
            } else if (Header.Fields.CONTENT_TYPE.equals(fieldName)) {
                content.setMimetype(parameterSplit[1].trim());
            }
        }
    }

    private void parseCookie(String line, Header header) {
        StringTokenizer tokenizer = new StringTokenizer(line, " :;");
        tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String nextCookieString = tokenizer.nextToken();
            Cookie cookie = new Cookie();
            int index = nextCookieString.indexOf('=');
            cookie.setName(nextCookieString.substring(0, index).trim());
            cookie.setValue(nextCookieString.substring(index + 1).trim());
            header.addCookie(cookie);
        }
    }

    private void parseHeaderField(String line, Header header) {
        int indexOfColon = line.indexOf(':');
        if (indexOfColon == -1) {
            LOGGER.info("No header field {}", line);
            return;
        }

        String fieldName = line.substring(0, indexOfColon);
        String fieldValue = line.substring(indexOfColon + 1);
        if (fieldValue.isEmpty()) {
            return;
        }
        String[] parameterValueSplit = fieldValue.split(",");
        for (String parameterString : parameterValueSplit) {
            if (parameterString.indexOf(';') != -1) {
                String[] valueQualitySplit = parameterString.split(";");
                Header.Parameter parameter = new Header.Parameter(valueQualitySplit[0]
                        .trim(), Double.parseDouble(valueQualitySplit[1]
                        .substring(2)));
                header.addParameter(fieldName.trim(), parameter);
            } else {
                header.addParameter(fieldName.trim(),
                        parameterString.trim());
            }
        }
    }

    private static final int CR = 13;
    private static final int LF = 10;
    private int last = -1; // The last char we've read

    /**
     * Read a line of data from the underlying inputstream and save it in the
     * StringBuffer <code>sb</code>.
     *
     * @param in The input stream to read from.
     * @param sb The StringBuffer to save the read line from the inputstream.
     * @throws java.io.IOException Throws an IOException if the inputstream fails.
     */
    private void readLine(InputStream in, StringBuffer sb) throws IOException {
        sb.delete(0, sb.length());
        int ch = -1; // currently read char

        if (last != -1) {
            sb.append((char) last);
        }
        ch = in.read();
        if (ch == -1) {
            return;
        }
        while (ch != CR && ch != LF) {
            sb.append((char) ch);
            ch = in.read();
        }
        // Read the next byte and check if it's a LF
        last = in.read();
        if (last == LF) {
            last = -1;
        }
    }
}
