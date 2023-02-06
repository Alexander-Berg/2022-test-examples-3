package ru.yandex.market.ff.model.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.dto.RequestStatusHistoryDTO;
import ru.yandex.market.ff.model.entity.RequestStatusHistory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тест для {@link RequestStatusHistoryConverter}.
 *
 * @author avetokhin 14/12/17.
 */
class RequestStatusHistoryConverterTest extends BaseConverterTest {
    private RequestStatusHistoryConverter converter = new RequestStatusHistoryConverter();

    @Test
    void covert() {
        final RequestStatusHistory source = filledStatusHistory();
        final RequestStatusHistoryDTO result = converter.convert(source);
        assertRequestStatusHistoryDTO(source, result);
    }

    @Test
    void convertNull() {
        assertThat(converter.convert(null), nullValue());
    }

}
