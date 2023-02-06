package ru.yandex.market.partner.mvc.controller.business;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.mbi.web.paging.PageTokenHelper;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "BusinessCampaignsControllerTest.before.csv")
public class BusinessCampaignsControllerTest extends FunctionalTest {
    private static final PageTokenHelper pageTokenHelper = new PageTokenHelper(OBJECT_MAPPER);

    @Autowired
    private BalanceContactService balanceContactService;

    private static Stream<Arguments> testBusinessCampaigns() {
        return Stream.of(
                // нет такого контакта
                Arguments.of(999, 1, null, "business-campaigns-empty.json", null),
                // контакт есть, нет привязанных бизнесов
                Arguments.of(11, 1, null, "business-campaigns-empty.json", null),
                // Задаем условие id > 101
                // Ответ пустой, нет бизнесов с ид > 101
                Arguments.of(12, 1, pageTokenHelper.createNextToken(101), "business-campaigns-empty.json", null),
                // нет связи с бизнесом, но есть линк с кампанией, которая в бизнесе, и нет роли в кампании
                Arguments.of(14, 1, null, "business-campaigns-empty.json", null),
                // нет связи с бизнесом, но админ кампании, которая в бизнесе
                Arguments.of(16, 1, null, "business-campaigns-16.json", null),
                // агентство с доступом к кампании под бизнесом
                Arguments.of(101, 5, null, "business-campaigns-101.json", null),
                // агентство без доступа к кампаниям под бизнесами
                Arguments.of(102, 5, null, "business-campaigns-empty.json", null),
                // агентство с доступом в несколько бизнесов, в бизнесе 104 у поставщика выключен доступ
                // от агентства @code ParamType#AGENCY_SUPPLIER_ACCESS(132)
                Arguments.of(103, 5, null, "business-campaigns-103.json", null),
                // агентство с доступом в бизнес только к одной кампании из двух
                Arguments.of(200, 5, null, "business-campaigns-200.json", null),
                // поставлен флажок возвращающий партнёров с закрытыми компаниями
                Arguments.of(19, 5, null, "business-campaigns-19.json", "&closed_campaigns=true")
        );
    }

    @ParameterizedTest
    @MethodSource("testBusinessCampaigns")
    @DbUnitDataSet(before = "BusinessCampaignsControllerTest.testBusinessCampaigns.before.csv")
    void testBusinessCampaigns(long uid, long limit, String pageToken, String expectedPath, String params) {
        when(balanceContactService.getClientIdByUid(101)).thenReturn(1011L);
        when(balanceContactService.getClientIdByUid(102)).thenReturn(1012L);
        when(balanceContactService.getClientIdByUid(103)).thenReturn(1013L);
        when(balanceContactService.getClientIdByUid(200)).thenReturn(1014L);
        when(balanceContactService.getClientIdByUid(19)).thenReturn(2001L);
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "businesses/campaigns?_user_id=" + uid + "&limit=" + limit +
                        (pageToken == null ? "" : "&page_token=" + pageToken) +
                        (params == null ? "": params)
                );
        JsonTestUtil.assertEquals("expected/" + expectedPath, getClass(), response);
    }
}
