package ru.yandex.market.rating;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link ImportShopRatingExecutor}.
 */
class ImportShopRatingTest extends FunctionalTest {

    @Autowired
    ImportShopRatingExecutor importShopRatingExecutor;

    @Autowired
    HttpClient httpClient;

    void mockHttpClient(String json) throws IOException {

        final CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
        final HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        final StatusLine statusLine = Mockito.mock(StatusLine.class);

        Mockito.doReturn(this.getClass().getResourceAsStream(json))
                .when(httpEntity)
                .getContent();
        Mockito.doReturn(httpEntity)
                .when(httpResponse)
                .getEntity();
        Mockito.doReturn(HttpStatus.SC_OK)
                .when(statusLine)
                .getStatusCode();
        Mockito.doReturn(statusLine)
                .when(httpResponse)
                .getStatusLine();
        Mockito.doReturn(httpResponse)
                .when(httpClient)
                .execute(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Прямой сценарий, все хорошо. Импортируем только те магазины," +
            "которые есть в datasource. При дубликатах сохраняем первый, второй пропускаем.")
    @DbUnitDataSet(before = "csv/ImportShopRatingTest.importRatingTest.before.csv",
            after = "csv/ImportShopRatingTest.importRatingTest.after.csv")
    void importRatingTest() throws IOException {
        mockHttpClient("shop_rating.txt");
        importShopRatingExecutor.doJob(null);
    }

    @Test()
    @DisplayName("Битый json, рейтинг должен остаться прежним")
    @DbUnitDataSet(before = "csv/ImportShopRatingTest.importRatingTest.before.csv",
            after = "csv/ImportShopRatingTest.importRatingTest.before.csv")
    void importRatingTestCorruptedJson() throws IOException {
        mockHttpClient("shop_rating_corrupted.txt");
        assertThrows(JsonSyntaxException.class, () -> importShopRatingExecutor.doJob(null));
    }


    @Test()
    @DisplayName("При недоступности json бросаем UncheckedIOException. Старые данные сохраняются")
    @DbUnitDataSet(before = "csv/ImportShopRatingTest.importRatingTest.before.csv",
            after = "csv/ImportShopRatingTest.importRatingTest.before.csv")
    void importRatingTestNotFound() {
        assertThrows(UncheckedIOException.class, () -> importShopRatingExecutor.doJob(null));
    }
}
