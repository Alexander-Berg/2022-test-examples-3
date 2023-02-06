package dictionaries;

import org.junit.Test;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.IndustrialManagerClient;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.ShopCpaOpenCutoff;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author aostrikov
 */
public class DictionaryIdFieldTest {

    @Test
    public void shouldGenerateCondition() {
        IndustrialManagerClient client = new IndustrialManagerClient();
        client.setClientId("norm");

        assertThat(client.toCondition(), is("(client_id = 'norm')"));
    }

    @Test
    public void shouldGenerateConditionWithNull() {
        IndustrialManagerClient client = new IndustrialManagerClient();

        assertThat(client.toCondition(), is("()"));
    }

    @Test
    public void shouldGenerateConditionWithoutFieldAnnotation() {
        System.out.println(new ShopCpaOpenCutoff().toCondition());
        ShopCpaOpenCutoff val = new ShopCpaOpenCutoff();
        val.setId("copy_that");

        assertThat(val.toCondition(), is("(id = 'copy_that')"));
    }
}
