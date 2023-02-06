package ru.yandex.market.global.partner.domain.legal_entity;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.LegalEntity;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.api.LegalEntityApiService;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.mj.generated.server.model.LegalEntityPatchDto;
import ru.yandex.mj.generated.server.model.LegalEntityRequestDto;
import ru.yandex.mj.generated.server.model.LegalEntityResponseDto;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class LegalEntityApiServiceTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            LegalEntityApiServiceTest.class
    ).build();
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final TestPartnerFactory testPartnerFactory;
    private final LegalEntityApiService legalEntityApiService;
    private final IndexingService indexingService;

    @Test
    public void testCreate() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntityRequestDto request = RANDOM.nextObject(LegalEntityRequestDto.class)
                .businessId(business.getId())
                .id(null);

        LegalEntityResponseDto response =
                legalEntityApiService.apiV1LegalEntityCreatePost(business.getId(), request).getBody();

        Assertions.assertThat(response)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(request);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getTrustPartnerId()).isEqualTo(TRUST_MOCKED_PARTNER_ID);
        Assertions.assertThat(response.getTrustProductId()).isEqualTo(TRUST_MOCKED_PARTNER_ID);
    }

    @Test
    public void testCreateWithoutEzcount() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntityRequestDto request = RANDOM.nextObject(LegalEntityRequestDto.class)
                .businessId(business.getId()).id(null).ezcountApiKey(null).ezcountRefreshToken(null);

        LegalEntityResponseDto response =
                legalEntityApiService.apiV1LegalEntityCreatePost(business.getId(), request).getBody();

        Assertions.assertThat(response)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(request);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getEzcountApiKey()).isEqualTo(EZCOUNT_MOCKED_API_KEY);
        Assertions.assertThat(response.getEzcountRefreshToken()).isEqualTo(EZCOUNT_MOCKED_REF_TOKEN);
    }

    @Test
    public void testNotSuccessfulEzcount() {
        Mockito.when(ezcountApiClient.apiUserCreatePost(Mockito.any())).thenThrow(RuntimeException.class);
        Business business = testPartnerFactory.createBusiness();
        LegalEntityRequestDto request = RANDOM.nextObject(LegalEntityRequestDto.class)
                .businessId(business.getId()).id(null).ezcountApiKey(null).ezcountRefreshToken(null);

        LegalEntityResponseDto response =
                legalEntityApiService.apiV1LegalEntityCreatePost(business.getId(), request).getBody();

        Assertions.assertThat(response)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(request);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getEzcountApiKey()).isNull();
        Assertions.assertThat(response.getEzcountRefreshToken()).isNull();
    }

    @Test
    public void testPatch() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());

        LegalEntityPatchDto request = RANDOM.nextObject(LegalEntityPatchDto.class)
                .name(null)
                .businessId(null)
                .id(null);

        LegalEntityResponseDto response =
                legalEntityApiService.apiV1LegalEntityPatchPost(legalEntity.getId(), request).getBody();

        Assertions.assertThat(response)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(request);

        Assertions.assertThat(response)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .comparingOnlyFields("name", "businessId", "id")
                .isEqualTo(legalEntity);
    }

    @Test
    public void testIndexShopOnPatch() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());
        ShopModel shop = testPartnerFactory.createShop(business.getId(), legalEntity.getId());
        ShopModel otherShop = testPartnerFactory.createShopAndAllRequired();

        LegalEntityPatchDto request = RANDOM.nextObject(LegalEntityPatchDto.class)
                .businessId(null)
                .id(null);

        legalEntityApiService.apiV1LegalEntityPatchPost(legalEntity.getId(), request);

        verify(indexingService, times(1)).index(
                any(), eq(shop.getShop().getId())
        );
        //noinspection ConstantConditions
        verify(indexingService, never()).index(
                any(), not(eq(shop.getShop().getId()))
        );
    }

}
