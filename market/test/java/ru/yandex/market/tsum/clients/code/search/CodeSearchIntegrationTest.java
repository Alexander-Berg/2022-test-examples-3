package ru.yandex.market.tsum.clients.code.search;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.code.search.model.CodeSearchRequest;
import ru.yandex.market.tsum.clients.code.search.model.CodeSearchResponse;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 19/11/2018
 */

@Ignore
public class CodeSearchIntegrationTest {
    private final CodeSearchClient client = new CodeSearchClient(CodeSearchClient.DEFAULT_URL);

    @Test
    public void search() {
        CodeSearchResponse response = client.search(new CodeSearchRequest()
            .withRegex("MJ_VERSION v1")
            .withFileRegex(".*/ya.make")
            .withIgnoreRegex("/test/ya.make|junk/*")
            .withNoJunk(true)
            .withNoContrib(true)
        );

        System.out.println(response);
    }
}
