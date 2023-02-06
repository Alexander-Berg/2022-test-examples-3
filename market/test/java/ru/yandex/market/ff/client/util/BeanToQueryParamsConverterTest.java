package ru.yandex.market.ff.client.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.StockType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit тесты для {@link BeanToQueryParamsConverter}.
 *
 * @author avetokhin 14.08.18.
 */
class BeanToQueryParamsConverterTest {

    @Test
    void convert() {
        final ShopRequestFilterDTO filter = new ShopRequestFilterDTO();
        filter.setArticle("shop_sku1");
        filter.setCreationDateFrom(LocalDate.of(2018, 1, 1));
        filter.setCreationDateTo(LocalDate.of(2018, 2, 1));
        filter.setRequestDateFrom(LocalDate.of(2019, 1, 1));
        filter.setRequestDateTo(LocalDate.of(2019, 5, 1));
        filter.setRequestIds(Arrays.asList("ID123", "ID456"));
        filter.setStockType(StockType.EXPIRED);
        filter.setStatuses(Arrays.asList(RequestStatus.PROCESSED, RequestStatus.CREATED));
        filter.setTypes(Collections.singletonList(1));
        filter.setHasDefects(true);
        filter.setHasAnomaly(true);
        filter.setSize(10);
        filter.setPage(1);

        final MultiValueMap<String, String> result = BeanToQueryParamsConverter.convert(filter);
        assertThat(result, notNullValue());
        assertThat(result.size(), equalTo(15));
        assertThat(result, hasEntry(equalTo("article"), contains("shop_sku1")));
        assertThat(result, hasEntry(equalTo("creationDateFrom"), contains("2018-01-01")));
        assertThat(result, hasEntry(equalTo("creationDateTo"), contains("2018-02-01")));
        assertThat(result, hasEntry(equalTo("requestDateFrom"), contains("2019-01-01")));
        assertThat(result, hasEntry(equalTo("requestDateTo"), contains("2019-05-01")));
        assertThat(result, hasEntry(equalTo("requestIds"), contains("ID123", "ID456")));
        assertThat(result, hasEntry(equalTo("stockType"), contains("1")));
        assertThat(result, hasEntry(equalTo("statuses"), contains("7", "0")));
        assertThat(result, hasEntry(equalTo("hasShortage"), contains("false")));
        assertThat(result, hasEntry(equalTo("hasDefects"), contains("true")));
        assertThat(result, hasEntry(equalTo("hasAnomaly"), contains("true")));
        assertThat(result, hasEntry(equalTo("statuses"), contains("7", "0")));
        assertThat(result, hasEntry(equalTo("types"), contains("1")));
        assertThat(result, hasEntry(equalTo("size"), contains("10")));
        assertThat(result, hasEntry(equalTo("page"), contains("1")));
    }

}
