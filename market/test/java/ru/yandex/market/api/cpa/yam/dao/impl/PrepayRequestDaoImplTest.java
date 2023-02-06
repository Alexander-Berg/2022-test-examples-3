package ru.yandex.market.api.cpa.yam.dao.impl;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.dao.filter.PrepayRequestFilter;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.supplier.prepay.PartnerApplicationKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link PrepayRequestDaoImpl}.
 */
@DbUnitDataSet(before = "db/PrepayRequestDaoImplTest_CPA_YAM_REQ_HISTORY.before.csv")
class PrepayRequestDaoImplTest extends FunctionalTest {

    @Autowired
    private PrepayRequestDaoImpl prepayRequestDao;


    /**
     * Проверяем, что заявки со статусом для переподписания
     * {@link PartnerApplicationStatus#NEW_PROGRAMS_VERIFICATION_REQUIRED}
     * отдаются в активынх заявках.
     */
    @Test
    void test_findActiveByDatasource_when_existsRequestInNewVerificationFailed_then_returnAsActive() {
        var reqsByShopId = prepayRequestDao.findActiveByDatasource(772);
        assertThat(reqsByShopId).hasSize(1);
    }

    /**
     * Проверяем, что заявки со статусом для переподписания {@link PartnerApplicationStatus#CANCELLED}
     * отдаются в заявках по простому поиску.
     */
    @Test
    void test_findByDatasource_when_existsRequestCancelled_then_returnRequest() {
        var reqsByShopId = prepayRequestDao.findByDatasource(775);
        assertThat(reqsByShopId).hasSize(1);
    }

    /**
     * Проверяем, что заявки со статусом для переподписания {@link PartnerApplicationStatus#CLOSED}
     * отдаются в заявках по простому поиску.
     */
    @Test
    void test_findByDatasource_when_existsRequestClosed_then_returnRequest() {
        var reqsByShopId = prepayRequestDao.findByDatasource(776);
        assertThat(reqsByShopId).hasSize(1);
    }

    /**
     * Проверяем, что заявки со статусом для переподписания {@link PartnerApplicationStatus#CLOSED}
     * не отдается в заявках по поиску активных
     */
    @ParameterizedTest
    @CsvSource({
            "775",
            "776"
    })
    void test_findActiveByDatasource_when_requestIsNotActive_then_returnNothing(int datasourceId) {
        var reqsByShopId = prepayRequestDao.findActiveByDatasource(datasourceId);
        assertThat(reqsByShopId).isEmpty();
    }

    /**
     * Проверяем, что заявки со статусом для переподписания
     * {@link PartnerApplicationStatus#NEW_PROGRAMS_VERIFICATION_FAILED}
     * отдаются в активынх заявках.
     */
    @Test
    void test_findActiveByDatasource_when_existsRequestInNewVerificationRequired_then_returnAsActive() {
        var reqsByShopId = prepayRequestDao.findActiveByDatasource(773);
        assertThat(reqsByShopId).hasSize(1);
    }

    @Test
    @DisplayName("Поиск по составному ключу")
    void findByKey() {
        var filterBuilder = PrepayRequestFilter.newBuilder();

        //Заявки не найдены
        filterBuilder.addApplicationKeys(Collections.singleton(new PartnerApplicationKey(100500, 500100)));
        var prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests).isEmpty();

        //Найдена одна заявка
        filterBuilder.addApplicationKeys(Collections.singleton(new PartnerApplicationKey(100, 771)));
        prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests).hasSize(1);

        //Найдена одна заявка с дополнительным фильтром
        filterBuilder.addStatuses(Collections.singleton(PartnerApplicationStatus.COMPLETED));
        prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests).hasSize(1);

        //Найдено 2 заявки
        filterBuilder.addApplicationKeys(Collections.singleton(new PartnerApplicationKey(101, 772)));
        filterBuilder.addStatuses(Collections.singleton(PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_REQUIRED));
        prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests).hasSize(2);
    }

    @Test
    @DisplayName("Проверка замены непечатных символов")
    void findByKeyBankName() {
        var filterBuilder = PrepayRequestFilter.newBuilder();

        //Найдена одна заявка
        filterBuilder.addApplicationKeys(Collections.singleton(new PartnerApplicationKey(104, 774)));
        var prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests)
                .singleElement()
                .satisfies(r -> assertThat(r.getBankName()).isEqualTo("bankName"));
    }

    @Test
    @DisplayName("Проверка iaAutoFilled")
    void findByStatusIsAutoFilled() {
        var filterBuilder = PrepayRequestFilter.newBuilder();

        //Найдена одна заявка
        filterBuilder.addStatuses(Collections.singleton(PartnerApplicationStatus.INIT));
        var prepayRequests = prepayRequestDao.find(filterBuilder.build());
        assertThat(prepayRequests)
                .singleElement()
                .satisfies(r -> assertThat(r.getIsAutoFilled()).isTrue());
    }

    @Test
    void getAllRequestIds() {
        var result = prepayRequestDao.getAllRequestIds();
        assertThat(result).containsExactly(100L, 101L, 102L, 104L, 105L);
    }
}
