package ru.yandex.market.application.properties.etcd.resources;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.application.properties.etcd.EtcdClientMock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author s-ermakov
 */
public class RemoteEtcdResourceTest {

    private EtcdClientMock etcdClient;

    @Before
    public void setUp() throws Exception {
        etcdClient = Mockito.spy(new EtcdClientMock());
        etcdClient.addKeyValue("/datasources/test", "test.key1", "test-value1");
        etcdClient.addKeyValue("/datasources/test", "test.key2", "test-value2");
    }

    @Test
    public void testCorrectProperties() throws IOException {
        RemoteEtcdResource resource = new RemoteEtcdResource("/datasources/test", etcdClient);
        String resultFile = convertStreamToString(resource.getInputStream());

        Assert.assertEquals("test.key1=test-value1\ntest.key2=test-value2", resultFile);
    }

    @Test
    public void testPropertiesWontLoadInResourceConstructor() throws IOException {
        Mockito.clearInvocations(etcdClient);
        RemoteEtcdResource resource = new RemoteEtcdResource("/datasources/test", etcdClient);

        Mockito.verify(etcdClient, Mockito.never()).getProperties(Mockito.any());

        resource.getInputStream();

        Mockito.verify(etcdClient, Mockito.times(1)).getProperties(Mockito.eq("/datasources/test"));
    }

    @Test
    public void testSingleCallClientForMultipleGetInputStreamCalls() throws IOException {
        RemoteEtcdResource resource = new RemoteEtcdResource("/datasources/test", etcdClient);

        Mockito.clearInvocations(etcdClient);
        resource.getInputStream();
        resource.getInputStream();

        Mockito.verify(etcdClient, Mockito.times(1)).getProperties(Mockito.eq("/datasources/test"));
    }

    @Test
    public void testExists() {
        RemoteEtcdResource resource = new RemoteEtcdResource("/datasources/test", etcdClient);
        Assert.assertTrue(resource.exists());

        RemoteEtcdResource resource2 = new RemoteEtcdResource("/datasources/test2", etcdClient);
        Assert.assertFalse(resource2.exists());
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testGetContentLength() throws IOException {
        RemoteEtcdResource resource = new RemoteEtcdResource("/datasources/test", etcdClient);
        Assert.assertEquals(43, resource.contentLength());

        try {
            RemoteEtcdResource resource2 = new RemoteEtcdResource("/datasources/test2", etcdClient);
            resource2.getInputStream();
            Assert.fail("Expected FileNotFoundException to be thrown");
        } catch (FileNotFoundException ignored) {
        }
    }

    public String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
