package ru.yandex.chemodan.app.djfs.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

public class DjfsApiMkdirTest extends DjfsApiActionTest {

    private static final DjfsUid UID = DjfsUid.cons(123L);

    @Before
    public void setUp() {
        super.setUp();
        getDjfsUserTestHelper().initializePgUser(UID, 1, Function.identity());
    }

    @Test
    public void testMkdir() throws IOException {
        final String startPath = "/disk/testMkdir";
        final int startPathDepth = StringUtils.countMatches(startPath, "/");
        final String url = String.format("/api/legacy/json/mkdir?uid=%s&path=", UID.asLong());

        A3TestHelper helper = getA3TestHelper();
        String path = startPath;
        Assert.ge(filesystem.getFolderDepthMax(), startPathDepth);

        // OK while directory is shallow enough
        for (int depth = startPathDepth; depth <= filesystem.getFolderDepthMax(); ++depth, path += "/testMkdir") {
            HttpResponse response = helper.get(url + path);
            Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        }

        // Fail when path is too deep
        HttpResponse response = helper.get(url + path);
        Assert.equals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
        Map<String, Object> result =
                mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>(){});
        Assert.equals(295, result.get("code"));
        Assert.equals("folder depth limit exceeded", result.get("title"));
    }
}
