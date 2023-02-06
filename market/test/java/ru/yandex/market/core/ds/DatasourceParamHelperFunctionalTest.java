package ru.yandex.market.core.ds;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;

import static org.junit.Assert.assertEquals;

/**
 * Тесты на {@link DatasourceParamHelper}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "csv/DatasourceParamHelper.before.csv")
class DatasourceParamHelperFunctionalTest extends FunctionalTest {

    private static final long DROPSHIP_SUPPLIER_ID = 1111L;

    @Autowired
    private DatasourceParamHelper datasourceParamHelper;

    /**
     * Проверяем, что у дропшип поставщика локальный регион доставки - Москва
     */
    @Test
    @DbUnitDataSet(before = "csv/DatasourceParamHelper.dropship.before.csv")
    void testDropshipLocalDeliveryRegion() {
        assertEquals(datasourceParamHelper.getLocalDeliveryRegion(DROPSHIP_SUPPLIER_ID), (Long) 213L);
    }

    /**
     * Проверяем, что у SMB без модерации - новичок, а с - не новичок
     */
    @ParameterizedTest
    @MethodSource("args")
    @DbUnitDataSet(before = "csv/DatasourceParamHelper.smb.before.csv")
    void testSMBNewbie(long shopId, boolean isNewbie) {
        assertEquals(isNewbie, datasourceParamHelper.isNewbie(PartnerId.datasourceId(shopId)));
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                //smb без модерации
                Arguments.of(110L, true),
                //smb с модерацией
                Arguments.of(220L, false),
                // обычный магазин без модерации и с оплатой
                Arguments.of(330L, false),
                // обычный магазин с модерацией и с оплатой
                Arguments.of(440L, false),
                // обычный магазин с модерацией без оплаты
                Arguments.of(550L, true)
        );
    }
}
