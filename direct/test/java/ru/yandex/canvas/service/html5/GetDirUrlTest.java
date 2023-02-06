package ru.yandex.canvas.service.html5;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.MDSService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class GetDirUrlTest {

    @Mock
    MDSService.MDSDir mdsDir;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getParentDirTest() throws MalformedURLException {

        when(mdsDir.getNameSpace()).thenReturn("canvas-html5-test");
        when(mdsDir.getCoupleId()).thenReturn(979657);
        when(mdsDir.getDirName()).thenReturn("931fff9c-f3f6-467f-a21c-3653e22c2b7b");

        when(mdsDir.getURL()).thenReturn("https://storage.mds.yandex.net/get-canvas-html5-test/979657/931fff9c-f3f6" +
                "-467f-a21c-3653e22c2b7b/files/f72a4dd2-1357-4b58-9f99-d835372bb079.html");
        when(mdsDir.getDirUrl()).thenCallRealMethod();

        String res = null;

        try {
            res = mdsDir.getDirUrl();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        assertEquals("Dir is as expected", res, "https://storage.mds.yandex.net/get-canvas-html5-test/979657/931fff9c" +
                "-f3f6-467f-a21c-3653e22c2b7b/");
    }

}
