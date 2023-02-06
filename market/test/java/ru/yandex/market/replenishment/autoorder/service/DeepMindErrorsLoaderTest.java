package ru.yandex.market.replenishment.autoorder.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DeepMindError;
import ru.yandex.market.replenishment.autoorder.service.client.TankerClient;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.DeepMindErrorsLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class DeepMindErrorsLoaderTest extends FunctionalTest {

    private DeepMindErrorsLoader loader;

    @Autowired
    private SqlSession batchSqlSession;

    @Before
    public void mockTankerClient() throws IOException {
        final TankerClient tankerClient = Mockito.mock(TankerClient.class);
        when(tankerClient.getDeepMindErrors()).thenReturn(
            List.of(
                new DeepMindError("101", "error1"),
                new DeepMindError("102", "error2")
            )
        );
        loader = new DeepMindErrorsLoader(batchSqlSession, tankerClient);
    }

    @Test
    public void testJSONParsing() {
        List<DeepMindError> deepMindErrors = TankerClient.parseDeepMindErrors(
            DeepMindErrorsLoaderTest.class.getResourceAsStream("DeepMindErrorsLoaderTest.data.json")
        );

        assertNotNull(deepMindErrors);
        assertEquals(14, deepMindErrors.size());
        deepMindErrors.sort(Comparator.comparing(DeepMindError::getCode));

        DeepMindError deepMindError = deepMindErrors.get(0);

        assertNotNull(deepMindError);
        assertEquals("mboc.msku.error.supply-forbidden.abo-legal-forbidden", deepMindError.getCode());
        assertEquals("Удалите товар из поставки — склад не примет товар, который скрыт с витрины по запросу " +
            "правообладателя", deepMindError.getText());

        deepMindError = deepMindErrors.get(13);

        assertNotNull(deepMindError);
        assertEquals("mboc.msku.error.supply-forbidden.warehouse", deepMindError.getCode());
        assertEquals("Удалите товар из списка — выбранный вами склад с ним не работает", deepMindError.getText());
    }

    @Test
    @DbUnitDataSet(
        before = "DeepMindErrorsTest.before.csv",
        after = "DeepMindErrorsTest.after.csv"
    )
    public void testDeepMindErrorsImport() {
        loader.load();
    }
}

