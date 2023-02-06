package ru.yandex.direct.i18n.tanker.test;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.i18n.tanker.Branch;
import ru.yandex.direct.i18n.tanker.Tanker;
import ru.yandex.direct.i18n.tanker.TankerException;
import ru.yandex.direct.test.utils.MockedHttpWebServerRule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AsyncHttpTankerTest {
    @Rule
    public final MockedHttpWebServerRule mockedHTTPServer = new MockedHttpWebServerRule(ContentType.APPLICATION_JSON);

    private Tanker tankerClient;

    @Before
    public void setup() {
        mockedHTTPServer.addPostResponsesFromConfig("ru/yandex/direct/i18n/tanker/test/http_mock.conf");

        tankerClient =
                new Tanker(mockedHTTPServer.getServerURL(), "test-token", "test-project", false);
    }

    @Test
    public void createBranchTest() {
        Branch branch = new Branch("name", "ref");
        tankerClient.createBranch(branch);
    }

    @Test
    public void createBranchBadTest() {
        Branch branch = new Branch("bad_branch", "ref");
        assertThatThrownBy(() -> tankerClient.createBranch(branch)).isInstanceOf(TankerException.class);
    }
}
