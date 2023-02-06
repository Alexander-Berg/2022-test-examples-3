package ru.yandex.market.mboc.common.logisticsparams;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sergeitelnov
 * @date 15/04/19
 */
@SuppressWarnings("checkstyle:magicnumber")
public class LogisticsParamRepositoryImplTest extends BaseDbTestClass {

    private static final long SUPPLIER_ID = 123;
    private static final long MOSCOW_WAREHOUSE_ID = 145;
    private static final long ROSTOV_WAREHOUSE_ID = 147;
    private static final String ALL_DAYS_STR = "пн,вт,ср,чт,пт,сб,вс";
    private static final String WORK_DAYS_STR = "пн,вт,ср,чт,пт";
    private static final int DELIVERY_TIME = 10;
    private static final LogisticsParam DUMMY = new LogisticsParam(
        SUPPLIER_ID, MOSCOW_WAREHOUSE_ID, ALL_DAYS_STR, DELIVERY_TIME);
    @Autowired
    private LogisticsParamRepository logisticsParamRepository;

    @Test
    public void insertOrUpdateTest() {
        logisticsParamRepository.insertOrUpdate(DUMMY);

        List<LogisticsParam> list = logisticsParamRepository.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(DUMMY);
    }

    @Test
    public void insertOrUpdateEqualsItemTest() {
        logisticsParamRepository.insertOrUpdate(DUMMY);
        logisticsParamRepository.insertOrUpdate(DUMMY);

        List<LogisticsParam> list = logisticsParamRepository.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(DUMMY);
    }

    @Test
    public void updateTest() {
        logisticsParamRepository.insertOrUpdate(DUMMY);
        LogisticsParam newDummy = new LogisticsParam(
            SUPPLIER_ID,
            MOSCOW_WAREHOUSE_ID,
            WORK_DAYS_STR,
            DELIVERY_TIME
        );

        logisticsParamRepository.insertOrUpdate(newDummy);

        List<LogisticsParam> list = logisticsParamRepository.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(newDummy);
        assertThat(list.get(0)).isNotEqualTo(DUMMY);
    }

    @Test
    public void getTest() {
        logisticsParamRepository.insertOrUpdate(DUMMY);

        List<LogisticsParam> list = logisticsParamRepository.getLogisticsParams(SUPPLIER_ID);
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(DUMMY);
    }

    @Test
    public void deleteTest() {
        logisticsParamRepository.insertOrUpdate(DUMMY);
        logisticsParamRepository.delete(DUMMY);

        assertThat(logisticsParamRepository.findAll()).isEmpty();
    }

    @Test
    public void multipleWarehousesSaveTest() {
        LogisticsParam moscowSupplier = new LogisticsParam(
            SUPPLIER_ID, MOSCOW_WAREHOUSE_ID, ALL_DAYS_STR, DELIVERY_TIME);
        LogisticsParam rostovSupplier = new LogisticsParam(
            SUPPLIER_ID, ROSTOV_WAREHOUSE_ID, ALL_DAYS_STR, DELIVERY_TIME);

        logisticsParamRepository.insertOrUpdate(moscowSupplier);
        logisticsParamRepository.insertOrUpdate(rostovSupplier);

        List<LogisticsParam> params = logisticsParamRepository.findAll();
        assertThat(params).hasSize(2);
        assertThat(params.get(0)).isEqualTo(moscowSupplier);
        assertThat(params.get(1)).isEqualTo(rostovSupplier);

        LogisticsParam newMoscowSupplier = new LogisticsParam(
            SUPPLIER_ID, MOSCOW_WAREHOUSE_ID, WORK_DAYS_STR, DELIVERY_TIME + 1);

        logisticsParamRepository.insertOrUpdate(newMoscowSupplier);

        params = logisticsParamRepository.findAll();

        assertThat(params).hasSize(2);
        assertThat(params.get(0)).isEqualTo(rostovSupplier);
        assertThat(params.get(1)).isNotEqualTo(moscowSupplier);
        assertThat(params.get(1)).isEqualTo(newMoscowSupplier);
    }

    @Test
    public void multipleWarehouseDeleteTest() {
        LogisticsParam moscowSupplier = new LogisticsParam(
            SUPPLIER_ID, MOSCOW_WAREHOUSE_ID, ALL_DAYS_STR, DELIVERY_TIME);
        LogisticsParam rostovSupplier = new LogisticsParam(
            SUPPLIER_ID, ROSTOV_WAREHOUSE_ID, ALL_DAYS_STR, DELIVERY_TIME);

        logisticsParamRepository.insertOrUpdate(moscowSupplier);
        logisticsParamRepository.insertOrUpdate(rostovSupplier);

        logisticsParamRepository.delete(rostovSupplier);

        List<LogisticsParam> params = logisticsParamRepository.findAll();
        assertThat(params).hasSize(1);
        assertThat(params.get(0)).isEqualTo(moscowSupplier);
    }
}
