package ru.yandex.market.deliverycalculator.workflow.util.segmentation2;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Класс с константами для запуска теста {@link SegmentationServiceTest#compareOldAndNewSegmentationAlgorithms(int, long)}.
 */
class TestingTariffs {

    // Список идентификаторов тарифов
    static final List<Long> TARIFF_IDS = ImmutableList.of(
    );

    // Список xml-описаний ожидаемых тарифов, например:
    // "<tariff min-weight=...>", ...
    static final List<String> EXPECTED_TARIFFS = ImmutableList.of(
    );

    // Список xml-описаний акуальных тарифов, например:
    // "<tariff min-weight=...>", ...
    static final List<String> ACTUAL_TARIFFS = ImmutableList.of(
    );

    // Список ссылок на ожидаемые бакеты, например:
    // "https://market-mbi-test.s3.mdst.yandex.net/delivery-calculator/buckets/daas-courier/gen-1040438-1040467/1390-nsrqhqvlny.pb.sn"
    static final List<String> EXPECTED_BUCKETS_URLS = ImmutableList.of(
    );

    // Список ссылок на актуальные бакеты, например:
    // "https://market-mbi-dev.s3.mdst.yandex.net/deliverycalculator_kotov-anton/buckets/daas-courier/gen-26-53/1390-pdjignhsgy.pb.sn"
    static final List<String> ACTUAL_BUCKETS_URLS = ImmutableList.of(
    );
}