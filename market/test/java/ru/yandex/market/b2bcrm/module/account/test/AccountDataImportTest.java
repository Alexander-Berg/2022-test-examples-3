package ru.yandex.market.b2bcrm.module.account.test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.market.b2bcrm.module.account.AbstractDataImportTest;
import ru.yandex.market.b2bcrm.module.account.B2bAccountContactRelation;
import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.account.Brand;
import ru.yandex.market.b2bcrm.module.account.Business;
import ru.yandex.market.b2bcrm.module.account.ImportMbiOffset;
import ru.yandex.market.b2bcrm.module.account.OnboardingState;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.b2bcrm.module.account.Vendor;
import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.dataimport.DataImportService;
import ru.yandex.market.jmf.dataimport.conf.Config;
import ru.yandex.market.jmf.dataimport.conf.datasource.StreamType;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.XmlUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.jmf.entity.test.assertions.EntityAttributeMatcher.havingAttributes;

public class AccountDataImportTest extends AbstractDataImportTest {

    @Inject
    DataImportService dataImportService;
    @Inject
    XmlUtils xmlUtils;
    @Inject
    ResourceLoader resourceLoader;

    private static OffsetDateTime withSystemOffset(OffsetDateTime offsetDateTime) {
        return offsetDateTime.withOffsetSameInstant(OffsetDateTime.now().getOffset());
    }

    @Test
    public void xmlShopTest() throws Exception {
        txService.runInNewTx(() -> bcpService.create(Business.FQN, Map.of(Business.BUSINESS_ID, 1234567L,
                Business.TITLE, "business_name_old")));
        Map<String, Object> params = Map.of(
                "url", "classpath:/b2b_shop.xml",
                "filename", "shop_crm_export.xml.gz"
        );

        Config config = getConfig("classpath:/b2bcrm/module/account/importConfig/ou_xml_shop.import.xml");
        dataImportService.execute(config, params);

        EntityCollectionAssert.assertThat(dbService.list(Query.of(Shop.FQN)))
                .hasSize(3)
                .anyHasAttributes(
                        Shop.SHOP_ID, 148L,
                        Shop.TITLE, "Bestwatch",
                        Shop.CPC_ENABLED, true,
                        Shop.CPC_BUDGET, BigDecimal.valueOf(702972),
                        Shop.DOMAIN, new Hyperlink("http://www.bestwatch.ru", "www.bestwatch.ru")
                )
                .anyHasAttributes(
                        Shop.SHOP_ID, 410L,
                        Shop.TITLE, "Цифровой Ангел",
                        Shop.CPC_ENABLED, true,
                        Shop.CPC_BUDGET, BigDecimal.valueOf(5808),
                        Shop.DOMAIN, null,
                        Shop.BUSINESS_ID, 1234567L
                )
                .anyHasAttributes(
                        Shop.SHOP_ID, 5557L,
                        Shop.TITLE, "SLK-Service.ru",
                        Shop.CPC_ENABLED, true,
                        Shop.CPC_BUDGET, BigDecimal.valueOf(66138),
                        Shop.DOMAIN, new Hyperlink("http://slk-service.ru", "slk-service.ru")
                );

        EntityCollectionAssert.assertThat(dbService.list(Query.of(ImportMbiOffset.FQN)))
                .anyHasAttributes(
                        ImportMbiOffset.QUEUE, "shop_crm_export.xml.gz",
                        ImportMbiOffset.OFFSET, withSystemOffset(OffsetDateTime.parse("2020-10-07T01:00:16.833Z"))
                );
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Fqn.of("account$business"))))
                .hasSize(1)
                .anyHasAttributes(
                        "businessId", 1234567L,
                        "title", "business_name"
                );
    }

    @Test
    public void xmlSupplierTest() throws Exception {
        Supplier account = getSupplier();
        B2bContact contact = txService.doInNewTx(() -> bcpService.create(
                B2bContact.FQN,
                Map.of(
                        B2bContact.TITLE, "old",
                        B2bContact.EMAILS, List.of("old@mail.ru"),
                        B2bContact.SOURCE_SYSTEM, "MBI"
                ))
        );
        txService.runInNewTx(() -> bcpService.create(
                        B2bAccountContactRelation.FQN,
                        Map.of(
                                B2bAccountContactRelation.ACCOUNT, account,
                                B2bAccountContactRelation.CONTACT, contact,
                                B2bAccountContactRelation.SOURCE_SYSTEM, "MBI",
                                B2bAccountContactRelation.CONTACT_ROLE, "RETURN_CONTACT")
                )
        );

        Config config = getConfig("classpath:/b2bcrm/module/account/importConfig/ou_xml_supplier.import.xml");
        dataImportService.execute(config, Map.of("url", "classpath:/b2b_supplier.xml"));

        EntityCollectionAssert.assertThat(dbService.list(Query.of(Supplier.FQN)))
                .hasSize(3)
                .anyHasAttributes(
                        Supplier.SUPPLIER_ID, 646767L,
                        Supplier.TITLE, "Альфамедэкс",
                        Supplier.SUPER_ADMIN_UID, 1040500L,
                        Supplier.ORDERS_COUNT, 0L,
                        Supplier.USES_PAPI, false,
                        Supplier.DOMAIN, new Hyperlink("https://alfamedex.ru/", "https://alfamedex.ru/"),
                        Supplier.BUSINESS_ID, 1234567L,
                        Supplier.EXPRESS, true,
                        Supplier.ONBOARDING_STATES, hasSize(2),
                        Supplier.ONBOARDING_STATES, contains(
                                havingAttributes(
                                        OnboardingState.STEP_TYPE, "stock_update",
                                        OnboardingState.STEP_STATUS, "testing_failed",
                                        OnboardingState.ENTER_STATUS_TIME, Dates.parseDateTime("2021-11-15 11:12:13")
                                ),
                                havingAttributes(
                                        OnboardingState.STEP_TYPE, "marketplace",
                                        OnboardingState.STEP_STATUS, "suspended",
                                        OnboardingState.ENTER_STATUS_TIME, Dates.parseDateTime("2021-11-15 11:12:13")
                                )
                        )
                )
                .anyHasAttributes(
                        Supplier.SUPPLIER_ID, 646878L,
                        Supplier.TITLE, "Волшебный мир воздушных сувениров",
                        Supplier.SUPER_ADMIN_UID, 450050L,
                        Supplier.ORDERS_COUNT, 129L,
                        Supplier.USES_PAPI, false,
                        Supplier.DOMAIN, null
                )
                .anyHasAttributes(
                        Supplier.SUPPLIER_ID, 646835L,
                        Supplier.TITLE, "Магазин металлической мебели",
                        Supplier.SUPER_ADMIN_UID, 1005600L,
                        Supplier.ORDERS_COUNT, 0L,
                        Supplier.USES_PAPI, false,
                        Supplier.DOMAIN, new Hyperlink("http://metbiz.ru", "metbiz.ru"),
                        Supplier.EXPRESS, false
                );
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Fqn.of("account$business"))))
                .anyHasAttributes(
                        "businessId", 1234567L,
                        "title", "business_name"
                );
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Fqn.of("b2bContact"))))
                .hasSize(5)
                .anyHasAttributes(
                        "title", "Неизвестный",
                        "emails", List.of("g.guslibstyy@alfamedex.ru"),
                        "phones", List.of("+78126272141818")
                )
                .anyHasAttributes(
                        "title", "Гуслистый Григорий",
                        "emails", List.of("old@mail.ru", "g.guslibstyy@alfamedex.ru"),
                        "phones", List.of("+78126272141818")
                )
                .anyHasAttributes(
                        "title", "Евгений Кобяк",
                        "emails", List.of("ek@metbiz.ru"),
                        "phones", List.of("+79219112558")
                )
                .anyHasAttributes(
                        "title", "Перепелюков Антон",
                        "emails", List.of("perepelukovanton@gmail.com"),
                        "phones", List.of("+79188937511")
                )
                .anyHasAttributes(
                        "title", "Перепелюкова Ольга",
                        "emails", List.of("perepelukova@mail.ru"),
                        "phones", List.of("+79081770167")
                );
        EntityCollectionAssert.assertThat(dbService.list(Query.of(Fqn.of("b2bAccountContactRelation"))))
                .hasSize(5);
    }

    @Test
    public void xmlSupplierOnboardingStateRemovalTest() throws Exception {
        Supplier supplier = getSupplier();
        OnboardingState stateToRemain = supplier.getOnboardingStates()
                .stream()
                .filter(state -> "stock_update".equals(state.getStepType().getCode()))
                .findFirst()
                .get();
        Config config = getConfig("classpath:/b2bcrm/module/account/importConfig/ou_xml_supplier.import.xml");
        dataImportService.execute(config, Map.of("url", "classpath:/b2b_supplier.xml"));
        EntityCollectionAssert.assertThat(dbService.list(Query.of(OnboardingState.FQN)))
                .noneHasAttributes(
                        OnboardingState.STEP_TYPE, "legal" //удалили тот, которого нет в выгрузке
                )
                .anyHasAttributes(
                        OnboardingState.STEP_TYPE, "stock_update",
                        HasGid.GID, stateToRemain.getGid() //Остался старый (изменили старый, а не удалили и создали
                        // новый)
                )
                .anyHasAttributes(
                        OnboardingState.STEP_TYPE, "marketplace" //добавили новый
                );
    }

    @Test
    public void xmlBrandTest() {
        dataImportService.execute(
                "classpath:/b2bcrm/module/account/importConfig/ou_xml_brand.import.xml",
                Map.of("url", "classpath:/b2b_brand_vendor.xml")
        );

        EntityCollectionAssert.assertThat(dbService.list(Query.of(Brand.FQN)))
                .hasSize(3)
                .anyHasAttributes(
                        Brand.BRAND_ID, 6988946L,
                        Brand.TITLE, "Highscreen",
                        Brand.BRAND_URL, new Hyperlink("http://highscreen.ru", "http://highscreen.ru")
                )
                .anyHasAttributes(
                        Brand.BRAND_ID, 987130L,
                        Brand.TITLE, "Mio",
                        Brand.BRAND_URL, new Hyperlink("http://www.mio.com", "http://www.mio.com")
                )
                .anyHasAttributes(
                        Brand.BRAND_ID,
                        15258233L,
                        Brand.TITLE,
                        "МАЙ",
                        Brand.BRAND_URL,
                        new Hyperlink("http://yarkraski.ru/brands/137", "http://yarkraski.ru/brands/137")
                );
    }

    @Test
    public void xmlVendorTest() {
        dataImportService.execute(
                "classpath:/b2bcrm/module/account/importConfig/ou_xml_vendor.import.xml",
                Map.of("url", "classpath:/b2b_brand_vendor.xml")
        );

        EntityCollectionAssert.assertThat(dbService.list(Query.of(Vendor.FQN)))
                .hasSize(3)
                .anyHasAttributes(
                        Vendor.VENDOR_ID, 7104L,
                        Vendor.TITLE, "Highscreen",
                        Vendor.PARENT_ID, 6988946L
                )
                .anyHasAttributes(
                        Vendor.VENDOR_ID, 2762L,
                        Vendor.TITLE, "МАЙ",
                        Vendor.PARENT_ID, 15258233L
                )
                .anyHasAttributes(
                        Vendor.VENDOR_ID, 8001L,
                        Vendor.TITLE, "Mio",
                        Vendor.PARENT_ID, 987130L
                );
    }

    @Nonnull
    private Config getConfig(String resource) throws IOException {
        InputStream is = resourceLoader.getResource(resource).getInputStream();
        Config config = xmlUtils.parse(is, Config.class);
        config.getProcess().forEach(p -> p.getDataSource().setInputStreamType(StreamType.DEFAULT));

        return config;
    }

    private Supplier getSupplier() {
        return txService.doInNewTx(() -> {
            OnboardingState onboardingStateToEdit = bcpService.create(
                    OnboardingState.FQN,
                    Map.of(
                            OnboardingState.STEP_TYPE, "stock_update",
                            OnboardingState.STEP_STATUS, "testing",
                            OnboardingState.ENTER_STATUS_TIME, "2021-11-14 11:12:13"
                    )
            );
            OnboardingState onboardingStateToRemove = bcpService.create(
                    OnboardingState.FQN,
                    Map.of(
                            OnboardingState.STEP_TYPE, "legal",
                            OnboardingState.STEP_STATUS, "testing",
                            OnboardingState.ENTER_STATUS_TIME, "2021-11-14 11:12:13"
                    )
            );
            return bcpService.create(
                    Supplier.FQN,
                    Map.of(
                            Supplier.SUPPLIER_ID, 646767L,
                            Supplier.TITLE, "old",
                            Supplier.ONBOARDING_STATES, List.of(onboardingStateToEdit, onboardingStateToRemove)
                    )
            );
        });
    }
}
