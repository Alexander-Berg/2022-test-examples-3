
package ru.yandex.common.util.http.zora;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 05.12.2016
 */
public class ZoraInputStreamReaderTest {
    @Test
    public void testReader() throws Exception {
        ZoraResponse response = new ZoraResponse();
        ZoraClient.parseExtrasToResponse(
                "http://www.ya.ru\tencoding=unknown\tlang=ru\tmime=text/json\t" +
                        "langregion=RU\terrorclass=REDIRECT\thttpcode=302", response
        );
        assertEquals("http://www.ya.ru", response.getUrl());
        assertEquals(302, response.getHttpCode());
        assertEquals("unknown", response.getEncoding());
        assertEquals("ru", response.getLang());
        assertEquals("text/json", response.getMime());

        response = new ZoraResponse();
        ZoraClient.parseExtrasToResponse(
                "http://www.ya.ru\tencoding=unknown\tlang=(null)\tmime=\t" +
                        "langregion=RU\terrorclass=REDIRECT\thttpcode=5000", response
        );
        assertEquals("http://www.ya.ru", response.getUrl());
        assertEquals(null, response.getLang());
        assertEquals(5000, response.getHttpCode());
        assertEquals("", response.getMime());
    }
}
