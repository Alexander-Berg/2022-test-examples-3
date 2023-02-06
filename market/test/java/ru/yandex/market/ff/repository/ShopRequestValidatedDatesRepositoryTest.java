package ru.yandex.market.ff.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequestValidatedDate;
import ru.yandex.market.ff.service.DateTimeService;

class ShopRequestValidatedDatesRepositoryTest extends IntegrationTest {

    @Autowired
    private ShopRequestValidatedDatesRepository requestValidatedDatesRepository;

    @Autowired
    private DateTimeService dateTimeService;

    @Test
    @DatabaseSetup("classpath:repository/shop-request-validated-dates/singular.xml")
    public void findAllByRequestIdEmptyList() {
        List<ShopRequestValidatedDate> result = requestValidatedDatesRepository.findAllByRequestId(1L);

        assertions.assertThat(result).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request-validated-dates/singular.xml")
    public void findAllByRequestIdSingletonList() {
        List<ShopRequestValidatedDate> result = requestValidatedDatesRepository.findAllByRequestId(0L);

        assertions.assertThat(result.size()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request-validated-dates/multiple.xml")
    public void findAllByRequestIdMultipleEntries() {
        List<ShopRequestValidatedDate> resultForFirstRequest = requestValidatedDatesRepository.findAllByRequestId(0L);
        List<ShopRequestValidatedDate> resultForSecondRequest = requestValidatedDatesRepository.findAllByRequestId(1L);

        assertions.assertThat(resultForFirstRequest.size()).isEqualTo(3);
        assertions.assertThat(resultForSecondRequest.size()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("classpath:repository/shop-request-validated-dates/before.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request-validated-dates/after.xml", assertionMode =
            DatabaseAssertionMode.NON_STRICT)
    public void saveSuccessful() {
        ShopRequestValidatedDate entity = new ShopRequestValidatedDate(0L, 0L, dateTimeService.localDateNow());

        requestValidatedDatesRepository.save(entity);
    }
}
