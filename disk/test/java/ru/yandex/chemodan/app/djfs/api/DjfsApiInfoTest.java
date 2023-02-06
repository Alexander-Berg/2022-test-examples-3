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
import ru.yandex.misc.test.Assert;

public class DjfsApiInfoTest extends DjfsApiActionTest {

    private static final DjfsUid UID = DjfsUid.cons(123L);

    @Before
    public void setUp() {
        super.setUp();
        getDjfsUserTestHelper().initializePgUser(UID, 1, Function.identity());
    }

    @Test
    public void testInfo() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse mkdirResponse = helper.get(String.format("/api/legacy/json/mkdir?uid=%s&path=/disk/test", UID.asLong()));
        Assert.equals(HttpStatus.SC_OK, mkdirResponse.getStatusLine().getStatusCode());
        HttpResponse infoResponse = helper.get(String.format("/api/legacy/json/info?uid=%s&path=/disk/test&meta=resource_id", UID.asLong()));
        Assert.equals(HttpStatus.SC_OK, infoResponse.getStatusLine().getStatusCode());
        Map<String, Object> result = mapper.readValue(helper.getResult(infoResponse), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(result.containsKey("type"));
        Assert.equals("dir", result.get("type"));
        Assert.isTrue(result.containsKey("path"));
        Assert.equals("/disk/test", result.get("path"));
        Assert.isTrue(result.containsKey("meta"));
    }
}
