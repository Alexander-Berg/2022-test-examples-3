package ru.yandex.market.core.feed.validation.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.assortment.model.AssortmentFeedValidationRequest;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidation;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationInfo;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationType;
import ru.yandex.market.core.misc.resource.RemoteResource;

import static ru.yandex.market.core.feed.validation.FeedValidationTestUtils.createAssortmentValidation;

/**
 * Date: 27.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class UnitedValidationInfoWrapperTest {

    public static ValidationInfoWrapper<AssortmentFeedValidationRequest, AssortmentValidationInfo> viw;
    public static AssortmentValidation assortmentValidation;
    public static AssortmentValidationInfo validationInfo;

    @BeforeAll
    static void init() {
        assortmentValidation = createAssortmentValidation(42L, 1001L, RemoteResource.of("http://ya", "login", "pass"), AssortmentValidationType.MAPPING_WITH_PRICES);
        validationInfo = AssortmentValidationInfo.of(assortmentValidation);
        viw = new UnitedValidationInfoWrapper(validationInfo, CampaignType.SUPPLIER);
    }

    @DisplayName("Получение идентификатора объекта на валидацию")
    @Test
    void getId_correctData_result() {
        Assertions.assertThat(viw.getId())
                .isEqualTo(assortmentValidation.id());
    }

    @DisplayName("Получение идентификатора магазина")
    @Test
    void getPartnerId_correctData_result() {
        Assertions.assertThat(viw.getPartnerId())
                .isEqualTo(assortmentValidation.request().partnerId());
    }

    @DisplayName("Получение ресурса, по которому скачивается фид")
    @Test
    void getRemoteResource_correctData_result() {
        Assertions.assertThat(viw.getRemoteResource())
                .isEqualTo(assortmentValidation.request().resource());
    }

    @DisplayName("Получение запроса на валидацию")
    @Test
    void getRequest_correctData_result() {
        Assertions.assertThat(viw.getRequest())
                .isEqualTo(assortmentValidation.request());
    }

    @DisplayName("Получение информации по валидации")
    @Test
    void getValidation_correctData_result() {
        Assertions.assertThat(viw.getValidation())
                .isEqualTo(validationInfo);
    }

    @DisplayName("Получение признака процессинга валидации фида")
    @Test
    void isProcessing_correctData_result() {
        Assertions.assertThat(viw.isProcessing())
                .isTrue();
    }

    @DisplayName("Получение времени начала процесса валидации")
    @Test
    void getRequestTime_correctData_result() {
        Assertions.assertThat(viw.getRequestTime())
                .isEqualTo(assortmentValidation.requestTime());
    }

    @DisplayName("Получение типа партнера")
    @ParameterizedTest(name = "campaignType = {0}")
    @CsvSource({
            "SUPPLIER",
            "SHOP"
    })
    void getCampaignType_correctData_result(CampaignType campaignType) {
        Assertions.assertThat(new UnitedValidationInfoWrapper(validationInfo, campaignType).getCampaignType())
                .isEqualTo(campaignType);
    }
}
