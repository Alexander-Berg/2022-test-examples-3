package ru.yandex.travel.orders.services.train.tariffinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.travel.dicts.rasp.proto.TTrainTariffInfo;
import ru.yandex.travel.orders.configurations.TrainTariffInfoDataProviderProperties;
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder;

public class TrainTariffInfoServiceTest {
    private final TrainTariffInfoService trainTariffInfoService;

    public TrainTariffInfoServiceTest() {
        var config = new TrainTariffInfoDataProviderProperties();
        config.setTablePath("tablePath");
        config.setIndexPath("./train-tariff-info-index");
        config.setProxy(new ArrayList<>());

        List<TTrainTariffInfo> mockData = Arrays.asList(
                TTrainTariffInfo.newBuilder()
                        .setCode("child")
                        .setImRequestCode("Full")
                        .setTitleRu("Детский")
                        .setImResponseCodes("Child")
                        .build(),

                TTrainTariffInfo.newBuilder()
                        .setCode("happy")
                        .setImRequestCode("Happy")
                        .setTitleRu("Праздничный")
                        .setImResponseCodes("Happy,Family")
                        .build()
        );

        var luceneIndexBuilder = new TestLuceneIndexBuilder<TTrainTariffInfo>()
                .setLuceneData(mockData);

        trainTariffInfoService = new TrainTariffInfoService(config, luceneIndexBuilder);
    }

    @Test
    public void testGetImRequestCode() {
        var actual = trainTariffInfoService.getImRequestCode("happy");

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual).isEqualTo("Happy");
    }

    @Test
    public void testGetImRequestCodeNotFound() {
        Assertions.assertThatThrownBy(() -> {
            trainTariffInfoService.getImRequestCode("not-found");
        });
    }

    @Test
    public void testGetOptionalTariffCode() {
        var actual = trainTariffInfoService.getOptionalTariffCode("Child");

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual).isEqualTo("child");
    }

    @Test
    public void testGetOptionalTariffCodeByOneOfSeveralResponseCodes() {
        var actual1 = trainTariffInfoService.getOptionalTariffCode("Family");
        var actual2 = trainTariffInfoService.getOptionalTariffCode("Happy");

        Assertions.assertThat(actual1).isNotNull();
        Assertions.assertThat(actual2).isNotNull();
        Assertions.assertThat(actual1).isEqualTo(actual2);
    }

    @Test
    public void testGetOptionalTariffCodeNotFound() {
        var actual = trainTariffInfoService.getOptionalTariffCode("NotFound");

        Assertions.assertThat(actual).isNull();
    }

    @Test
    public void testGetOptionalTariffTitle() {
        var actual = trainTariffInfoService.getOptionalTariffTitle("child");

        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual).isEqualTo("Детский");
    }

    @Test
    public void testGetOptionalTariffTitleNotFound() {
        var actual = trainTariffInfoService.getOptionalTariffTitle("not-found");

        Assertions.assertThat(actual).isNull();
    }
}
