package ru.yandex.market.mbi.affiliate.promo.service;

import java.util.List;
import java.util.Map;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.distribution.DistributionPlaceClient;
import ru.yandex.market.mbi.affiliate.promo.distribution.DistributionReportClient;
import ru.yandex.market.mbi.affiliate.promo.model.Partner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
@DbUnitDataSet(dataSource = "promoDataSource", before = "db/partners_before.csv")
public class PartnerServiceTest {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private DistributionReportClient reportClientMock;
    @Autowired
    private DistributionPlaceClient placeClientMock;

    @Test
    public void testGetClidsByUserLogin() {
        var partners = partnerService.getClidsByUserLogins(List.of("user2"));
        assertThat(partners, iterableWithSize(1));
        assertThat(partners.get(0), is(
                new Partner(21, "user2", "pack.domain2.ru", "Instagram")));

    }

    @Test
    public void testGetClidsByUserLoginNoPlaceType() {
        var partners = partnerService.getClidsByUserLogins(List.of("user3"));
        assertThat(partners, iterableWithSize(2));
        assertThat(partners, containsInAnyOrder(
                new Partner(32, "user3", "another.pack.domain3.ru", null),
                new Partner(33, "user3", "yet-another.pack.domain3.ru", null)));

    }

    @Test
    public void testGetClidsByUserLoginEmpty() {
        var partners = partnerService.getClidsByUserLogins(List.of("user10"));
        assertThat(partners, emptyIterable());
    }

    @Test
    public void testGetClidsByUserLoginEmptyList() {
        var partners = partnerService.getClidsByUserLogins(List.of());
        assertThat(partners, emptyIterable());
    }

    @Test
    public void testGetUserLoginByClid() {
        var userLogin = partnerService.getUserLoginByClid(21L);
        assertThat(userLogin, is("user2"));
    }

    @Test
    public void testGetUserLoginNotFound() {
        var userLogin = partnerService.getUserLoginByClid(58L);
        assertThat(userLogin, nullValue());
    }


    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/partners_after_update.csv")
    public void testUpdatePartners() {
        when(reportClientMock.getAllPartners()).thenReturn(
                List.of(new Partner(11, "user1", "pack.domain11.ru"),
                        new Partner(21, "user2", "pack.domain2.ru"),
                        new Partner(22, "user2", "another.pack.domain2.ru"),
                        new Partner(33, "user3", "pack.domain3.ru"),
                        new Partner(51, "user5", "pack.domain5.ru"),
                        new Partner(52, "user5", "another.pack.domain5.ru")
                        ));
        partnerService.updatePartners();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/partners_after_update_place_types.csv")
    public void testUpdatePlaceTypes() {
        when(placeClientMock.getAllClidsWithPlaceType())
                .thenReturn(Map.of(
                        11L, "Instagram",
                        21L, "Купонный агрегатор",
                        22L, "Другое",
                        31L, "Мобильное приложение",
                        32L, "Youtube"));
        partnerService.updatePlaceTypes();
    }
}