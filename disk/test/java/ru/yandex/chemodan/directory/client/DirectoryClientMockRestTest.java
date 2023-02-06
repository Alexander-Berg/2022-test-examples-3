package ru.yandex.chemodan.directory.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ContextConfiguration(classes = {DirectoryClientIntegrationTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectoryClientMockRestTest {
    @Autowired
    private DirectoryClient directoryClient;

    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @Before
    public void init() {
        objectMapper = new ObjectMapper();
        mockServer = MockRestServiceServer.createServer(directoryClient.getRestTemplate());
    }

    @Test
    public void testGetUserInfo() throws URISyntaxException {
        PassportUid uid = PassportUid.cons(4093541324L);
        String orgId = "110021";
        ObjectNode rs = objectMapper.createObjectNode()
                .put("id", uid.toString())
                .put("is_admin", true);

        mockServer.expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(new URI("https://api-internal-test.directory.ws.yandex" +
                                ".net/v11/users/" + uid + "/?fields=is_admin")))
                .andExpect(header("X-ORG-ID", orgId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(rs.toString())
                );

        DirectoryUsersInfoResponse userInfo = directoryClient.getUserInfo(uid, orgId).get();
        Assert.equals(uid.getUid(), userInfo.getId());
        Assert.isTrue(userInfo.isAdmin());
    }
}
