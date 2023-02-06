package ru.yandex.canvas.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.model.Util.deepcopy;

/**
 * Tests for {@link Util}
 * <p>
 * Created by pupssman on 10.04.17.
 */
public class UtilTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deepcopySmoke() throws Exception {
        Bundle bundle = new Bundle("foo", 1);
        Bundle copy = deepcopy(mapper, bundle, Bundle.class);

        assertEquals(bundle.getName(), copy.getName());
        assertEquals(bundle.getVersion(), copy.getVersion());
    }

    @Test
    public void deepcopyDeepness() throws Exception {
        CreativeData data = new CreativeData();
        data.setBundle(new Bundle("foo", 1));

        CreativeData copy = deepcopy(mapper, data, CreativeData.class);

        assertEquals("foo", copy.getBundle().getName());

        copy.getBundle().setName("bar");

        assertEquals("foo", data.getBundle().getName());
        assertEquals("bar", copy.getBundle().getName());
    }
}
