package ru.yandex.market.deliverycalculator.workflow.service;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.model.SiteType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.MardoCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.RegularCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.RegularPickupGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.workflow.daas.DaasCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardocourier.MardoCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.RegularCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.regularpickup.RegularPickupTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

class ActualDeliveryInfoCacheServiceTest extends FunctionalTest {

    @Autowired
    @Qualifier("regularCourierTariffSearchEngineWorkflow")
    private RegularCourierTariffWorkflow regularCourierTariffWorkflow;

    @Autowired
    @Qualifier("regularPickupTariffSearchEngineWorkflow")
    private RegularPickupTariffWorkflow regularPickupTariffWorkflow;

    @Autowired
    @Qualifier("mardoCourierTariffSearchEngineWorkflow")
    private MardoCourierTariffWorkflow mardoCourierTariffWorkflow;

    @Autowired
    @Qualifier("daasCourierTariffSearchEngineWorkflow")
    private DaasCourierTariffWorkflow daasCourierTariffWorkflow;

    @Autowired
    private SenderSettingsCacheService senderSettingsCacheService;

    @Autowired
    private ShopSettingsCacheService shopSettingsCacheService;

    @Autowired
    private ActualDeliveryInfoCacheService actualDeliveryInfoCacheService;

    @Autowired
    private GenerationRepository generationRepository;

    @Test
    @DbUnitDataSet(before = "database/ActualDeliveryInfoCacheServiceTest.cacheCorrectnessTest.before.csv")
    void cacheCorrectnessTest() {
        long shopId1 = 111;
        long shopId2 = 112;
        long blueCustomerTariffId1 = 211;
        long blueShopTariffId1 = 311;
        long daasCourierTariffId1 = 511;
        String fakeTariffInfo = "<tariff><rule><bucket id=\"1\"/></rule></tariff>";

        // ???????? ???????????????????? ?????????? ???????????????? shopId1, ???????????? ?? ?????????????????? 1-1
        String regularCourierGeneration11Url = "shop1-regular-courier-url";
        Generation generation11 = new Generation(1, 1);
        RegularCourierGeneration regularCourierGeneration11 = new RegularCourierGeneration();
        regularCourierGeneration11.setShopId(shopId1);
        regularCourierGeneration11.setDeleted(false);
        regularCourierGeneration11.setBucketsUrl(regularCourierGeneration11Url);
        generation11.setRegularCourierGenerations(Collections.singleton(regularCourierGeneration11));
        regularCourierTariffWorkflow.loadFromGeneration(generation11);

        // ???????? ???????????????????? ?????????? ???????????????? shopId2, ???????????? ?? ?????????????????? 2-2
        String regularCourierGeneration22Url = "shop2-regular-courier-url";
        Generation generation22 = new Generation(2, 2);
        RegularCourierGeneration regularCourierGeneration22 = new RegularCourierGeneration();
        regularCourierGeneration22.setShopId(shopId2);
        regularCourierGeneration22.setDeleted(false);
        regularCourierGeneration22.setBucketsUrl(regularCourierGeneration22Url);
        generation22.setRegularCourierGenerations(Collections.singleton(regularCourierGeneration22));
        regularCourierTariffWorkflow.loadFromGeneration(generation22);

        // ???????? ?????????? ?????????? ???????????????? shopId2, ???????????? ?? ?????????????????? 3-3
        String regularPickupGeneration33Url = "shop2-regular-pickup-url";
        Generation generation33 = new Generation(3, 3);
        RegularPickupGeneration regularPickupGeneration33 = new RegularPickupGeneration();
        regularPickupGeneration33.setGeneration(generation33);
        regularPickupGeneration33.setShopId(shopId2);
        regularPickupGeneration33.setDeleted(false);
        regularPickupGeneration33.setBucketsURL(regularPickupGeneration33Url);
        regularPickupGeneration33.setTariffInfo(fakeTariffInfo);
        generation33.setRegularPickupGenerations(Collections.singleton(regularPickupGeneration33));
        regularPickupTariffWorkflow.loadFromGeneration(generation33);

        // ?????????? ???????????????????? ?????????? blueCustomerTariffId1-blueShopTariffId1, ???????????? ?? ?????????????????? 4-4
        String mardoCourierGeneration44Url = "blue-courier-tariff-url";
        Generation generation44 = new Generation(4, 4);
        MardoCourierGeneration mardoCourierGeneration44 = new MardoCourierGeneration();
        mardoCourierGeneration44.setGeneration(generation44);
        mardoCourierGeneration44.setCustomerTariffId(blueCustomerTariffId1);
        mardoCourierGeneration44.setShopTariffId(blueShopTariffId1);
        mardoCourierGeneration44.setDeleted(false);
        mardoCourierGeneration44.setBucketsUrl(mardoCourierGeneration44Url);
        mardoCourierGeneration44.setTariffInfo(fakeTariffInfo);
        generation44.setMardoCourierGenerations(Collections.singleton(mardoCourierGeneration44));
        mardoCourierTariffWorkflow.loadFromGeneration(generation44);

        // DAAS ???????????????????? ?????????? daasCourierTariff1, ???????????? ?? ?????????????????? 6-6
        String daasCourierGeneration66Url = "daas-courier-tariff-url-1";
        Generation generation66 = new Generation(6, 6);
        DaasCourierGeneration daasCourierGeneration66 = new DaasCourierGeneration();
        daasCourierGeneration66.setGeneration(generation66);
        daasCourierGeneration66.setTariffId(daasCourierTariffId1);
        daasCourierGeneration66.setDeleted(false);
        daasCourierGeneration66.setBucketsUrl(daasCourierGeneration66Url);
        daasCourierGeneration66.setTariffInfo(fakeTariffInfo);
        generation66.setDaasCourierGenerations(Collections.singleton(daasCourierGeneration66));
        daasCourierTariffWorkflow.loadFromGeneration(generation66);

        // DAAS ???????????????????? ?????????? daasCourierTariff1, ???????????????? ?? ?????????????????? 7-6 (?????? ?????????????????? ??????????)
        String daasCourierGeneration76Url = "daas-courier-tariff-url-2";
        Generation generation76 = new Generation(7, 6);
        DaasCourierGeneration daasCourierGeneration76 = new DaasCourierGeneration();
        daasCourierGeneration76.setGeneration(generation76);
        daasCourierGeneration76.setTariffId(daasCourierTariffId1);
        daasCourierGeneration76.setDeleted(false);
        daasCourierGeneration76.setBucketsUrl(daasCourierGeneration76Url);
        daasCourierGeneration76.setTariffInfo(fakeTariffInfo);
        generation76.setDaasCourierGenerations(Collections.singleton(daasCourierGeneration76));
        daasCourierTariffWorkflow.loadFromGeneration(generation76);
        // ?????????????????? ?????? ?????? ???????????????? ???????????????????? ???????????? ???? ?????????? ???? ????????????????
        daasCourierTariffWorkflow.outdateGeneration(6);

        // ?????????????????? ?????????????? senderId1, ?????????????? ?? ?????????????????? 8-7
        String sender1Url = "sender-1-url";
        Generation generation87 = generationRepository.findById(8L).orElseThrow();
        senderSettingsCacheService.loadToCache(generation87);

        //?????????????????? ???????????????????? ??????????????????????????. ?????????????? ?? ???????????????????? 10 ?? 9
        Generation generation9 = generationRepository.findById(9L).orElseThrow();
        Generation generation10 = generationRepository.findById(10L).orElseThrow();
        shopSettingsCacheService.loadToCache(generation9);
        shopSettingsCacheService.loadToCache(generation10);

        assertThat(
                actualDeliveryInfoCacheService.getAllBucketsUrlsForSiteType(SiteType.WHITE),
                containsInAnyOrder(
                        regularCourierGeneration11Url,
                        regularCourierGeneration22Url,
                        regularPickupGeneration33Url,
                        daasCourierGeneration76Url
                )
        );
        assertThat(actualDeliveryInfoCacheService.getAllBucketsUrlsForSiteType(SiteType.BLUE), containsInAnyOrder(mardoCourierGeneration44Url));
        assertThat(actualDeliveryInfoCacheService.getAllModifiersUrlsForSiteType(SiteType.WHITE), containsInAnyOrder(sender1Url,
                "shop-modifiers-url-1", "shop-modifiers-url-2"));
        assertThat(actualDeliveryInfoCacheService.getAllModifiersUrlsForSiteType(SiteType.BLUE), empty());

        // ???????????????????? ???????????? ?????????????????????? ???????????? blueCustomerTariffId1-blueShopTariffId1, ?????????????? ?? ?????????????????? 9-8 (?????? ????????????????)
        String mardoCourierGeneration98Url = "blue-courier-tariff-url-2";
        Generation generation98 = new Generation(9, 8);
        MardoCourierGeneration mardoCourierGeneration98 = new MardoCourierGeneration();
        mardoCourierGeneration98.setGeneration(generation98);
        mardoCourierGeneration98.setCustomerTariffId(blueCustomerTariffId1);
        mardoCourierGeneration98.setShopTariffId(blueShopTariffId1);
        mardoCourierGeneration98.setDeleted(false);
        mardoCourierGeneration98.setBucketsUrl(mardoCourierGeneration98Url);
        mardoCourierGeneration98.setTariffInfo(fakeTariffInfo);
        generation98.setMardoCourierGenerations(Collections.singleton(mardoCourierGeneration98));
        mardoCourierTariffWorkflow.loadFromGeneration(generation98);

        // ???????????????????? ???????????? ?????????????????????? ???????????? ???????????????? shopId2, ?????????????? ?? ?????????????????? 10-9 (?? ??????????????????)
        String regularCourierGeneration109Url = "shop2-regular-courier-url-2";
        Generation generation109 = new Generation(10, 9);
        RegularCourierGeneration regularCourierGeneration109 = new RegularCourierGeneration();
        regularCourierGeneration109.setShopId(shopId2);
        regularCourierGeneration109.setDeleted(false);
        regularCourierGeneration109.setBucketsUrl(regularCourierGeneration109Url);
        generation109.setRegularCourierGenerations(Collections.singleton(regularCourierGeneration109));
        regularCourierTariffWorkflow.loadFromGeneration(generation109);
        regularCourierTariffWorkflow.outdateGeneration(10);

        // ???????????????????? ?????????????????????? daas ???????????? daasCourierTariff1, ?????????????? ?? ?????????????????? 11-10 (?? ??????????????????)
        String daasCourierGeneration1110Url = "daas-courier-tariff-url-3";
        Generation generation1110 = new Generation(11, 10);
        DaasCourierGeneration daasCourierGeneration1110 = new DaasCourierGeneration();
        daasCourierGeneration1110.setGeneration(generation1110);
        daasCourierGeneration1110.setTariffId(daasCourierTariffId1);
        daasCourierGeneration1110.setDeleted(false);
        daasCourierGeneration1110.setBucketsUrl(daasCourierGeneration1110Url);
        daasCourierGeneration1110.setTariffInfo(fakeTariffInfo);
        generation1110.setDaasCourierGenerations(Collections.singleton(daasCourierGeneration1110));
        daasCourierTariffWorkflow.loadFromGeneration(generation1110);
        daasCourierTariffWorkflow.outdateGeneration(10);

        assertThat(
                actualDeliveryInfoCacheService.getAllBucketsUrlsForSiteType(SiteType.WHITE),
                containsInAnyOrder(
                        regularCourierGeneration11Url,
                        regularCourierGeneration109Url,
                        regularPickupGeneration33Url,
                        daasCourierGeneration1110Url
                )
        );
        assertThat(
                actualDeliveryInfoCacheService.getAllBucketsUrlsForSiteType(SiteType.BLUE),
                containsInAnyOrder(
                        mardoCourierGeneration44Url,
                        mardoCourierGeneration98Url
                )
        );
    }

}
