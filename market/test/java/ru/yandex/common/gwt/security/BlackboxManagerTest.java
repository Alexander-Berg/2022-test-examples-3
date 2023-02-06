package ru.yandex.common.gwt.security;

import java.util.Arrays;
import java.util.Collections;

import org.asynchttpclient.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.gwt.shared.UserWithBlackboxFields;
import ru.yandex.common.gwt.shared.blackbox.BlackBoxField;

/**
 * @author apluhin
 * @created 4/12/21
 */

public class BlackboxManagerTest {

    @Test
    public void testParseReadLoginResponse() throws Exception {
        BlackboxManager blackboxManager = new BlackboxManager();
        Response mock = Mockito.mock(Response.class);

        Mockito.when(mock.getResponseBodyAsStream())
                .thenReturn(this.getClass().getResourceAsStream("/success_userinfo_request.json"));
        UserWithBlackboxFields userWithBlackboxFields = blackboxManager.parseUserInfoResponse(mock,
                Arrays.asList(BlackBoxField.EMAIL, BlackBoxField.FIO));
        Assert.assertEquals("test", userWithBlackboxFields.getUser().getLogin());
        Assert.assertEquals(1L, userWithBlackboxFields.getUser().getUid());
        Assert.assertEquals("some name", userWithBlackboxFields.getDbfields().get(BlackBoxField.FIO));
        Assert.assertEquals("", userWithBlackboxFields.getDbfields().get(BlackBoxField.EMAIL));
    }

    @Test(expected = Exception.class)
    public void testParseFailedResponse() throws Exception {
        BlackboxManager blackboxManager = new BlackboxManager();
        Response mock = Mockito.mock(Response.class);

        Mockito.when(mock.getResponseBodyAsStream())
                .thenReturn(this.getClass().getResourceAsStream("/failed_userinfo_request.json"));
        blackboxManager.parseUserInfoResponse(mock, Collections.emptyList());
    }
}
