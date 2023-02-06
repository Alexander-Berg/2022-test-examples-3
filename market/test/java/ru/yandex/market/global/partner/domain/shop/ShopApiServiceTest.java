package ru.yandex.market.global.partner.domain.shop;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.LegalEntity;
import ru.yandex.market.global.db.jooq.tables.pojos.Shop;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.api.LegalEntityApiService;
import ru.yandex.market.global.partner.api.ShopApiService;
import ru.yandex.market.global.partner.authorization.PartnerUserDetails;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.market.global.partner.util.VisualCategoryUtil;
import ru.yandex.mj.generated.server.model.AddressPatchDto;
import ru.yandex.mj.generated.server.model.LegalEntityPatchDto;
import ru.yandex.mj.generated.server.model.ListShopsResponseDto;
import ru.yandex.mj.generated.server.model.ShopDto;
import ru.yandex.mj.generated.server.model.ShopPatchDto;

import static ru.yandex.market.global.partner.mapper.EntityMapper.MAPPER;

public class ShopApiServiceTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ShopApiServiceTest.class).build();

    //эти поля должны быть проброшены из Legal entity
    public static final RecursiveComparisonConfiguration LEGAL_ENTITY_IGNORED_CONFIGURATION =
            RecursiveComparisonConfiguration.builder().withIgnoredFields("taxId", "trustRegionId",
                    "trustPartnerId", "trustProductId", "ezcountApiKey", "ezcountRefreshToken").build();

    @Autowired
    private ShopApiService shopApiService;

    @Autowired
    private LegalEntityApiService legalEntityApiService;

    @Autowired
    private TestPartnerFactory testPartnerFactory;

    private static final List<String> VISUAL_CATEGORIES = List.of("home-and-kitchen");

    @Test
    public void testCreate() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());

        ShopDto shopDto = RANDOM.nextObject(ShopDto.class)
                .id(null)
                .businessId(null)
                .legalEntityId(legalEntity.getId())
                .ezcountApiKey(null)
                .ezcountRefreshToken(null)
                .trustProductId(null)
                .visualCategories(VISUAL_CATEGORIES)
                .hidden(true);
        ShopDto updated = shopApiService.apiV1ShopCreatePost(business.getId(), shopDto).getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison(LEGAL_ENTITY_IGNORED_CONFIGURATION)
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(shopDto);
    }

    @Test
    public void testCreateWithoutVisualCategories() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());

        ShopDto shopDto = RANDOM.nextObject(ShopDto.class)
                .id(null)
                .businessId(null)
                .legalEntityId(legalEntity.getId())
                .ezcountApiKey(null)
                .ezcountRefreshToken(null)
                .trustProductId(null)
                .hidden(true)
                .visualCategories(null);
        ShopDto updated = shopApiService.apiV1ShopCreatePost(business.getId(), shopDto).getBody();

        Assertions.assertThat(updated).isNotNull();
        Assertions.assertThat(updated.getVisualCategories()).isEmpty();
    }

    @Test
    public void testCreateWithInvalidVisualCategories() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());

        ShopDto shopDto = RANDOM.nextObject(ShopDto.class)
                .id(null)
                .businessId(null)
                .legalEntityId(legalEntity.getId())
                .ezcountApiKey(null)
                .ezcountRefreshToken(null)
                .trustProductId(null)
                .hidden(true)
                .visualCategories(List.of("Home&Kitchen"));

        Assertions.assertThatThrownBy(() -> shopApiService.apiV1ShopCreatePost(business.getId(), shopDto));
    }

    @Test
    public void testCreateWithoutLE() {
        Business business = testPartnerFactory.createBusiness();
        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(business.getId());

        ShopDto shopDto = RANDOM.nextObject(ShopDto.class)
                .id(null)
                .businessId(null)
                .legalEntityId(null)
                .ezcountApiKey(null)
                .ezcountRefreshToken(null)
                .trustProductId(null)
                .visualCategories(VISUAL_CATEGORIES)
                .hidden(true);
        ShopDto updated = shopApiService.apiV1ShopCreatePost(business.getId(), shopDto).getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison(LEGAL_ENTITY_IGNORED_CONFIGURATION)
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(shopDto);
        Assertions.assertThat(updated).isNotNull();
        Assertions.assertThat(updated.getLegalEntityId()).isEqualTo(legalEntity.getId());
    }

    @Test
    public void testUpdate() {
        ShopDto shopDto = RANDOM.nextObject(ShopDto.class)
                .id(null)
                .legalEntityId(testData.getSomeShop().getLegalEntityId())
                .permissions(null)
                .businessId(null)
                .visualCategories(VisualCategoryUtil.getValidCategories(3));
        ShopDto updated = shopApiService.apiV1ShopUpdatePost(testData.getSomeShop().getId(), shopDto).getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison(LEGAL_ENTITY_IGNORED_CONFIGURATION)
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(shopDto);
    }

    @Test
    public void testPatch() {
        ShopDto existing = MAPPER.toShopDto(testData.getSomeShopModel());
        ShopPatchDto shopPatchDto = RANDOM.nextObject(ShopPatchDto.class)
                .address(RANDOM.nextObject(AddressPatchDto.class).coordinates(null))
                .name(null)
                .enabled(!existing.getEnabled())
                .hidden(!existing.getHidden())
                .visualCategories(null);

        ShopDto updated = shopApiService.apiV1ShopPatchPost(testData.getSomeShop().getId(), shopPatchDto).getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison(LEGAL_ENTITY_IGNORED_CONFIGURATION)
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(shopPatchDto)

                .comparingOnlyFields("address.coordinates", "name")
                .ignoringCollectionOrder()
                .isEqualTo(existing);
    }

    @Test
    public void testPatchVisualCategories() {
        List<String> catergoies = List.of("gifts-and-flowers");
        ShopDto existing = MAPPER.toShopDto(testData.getSomeShopModel());
        ShopPatchDto shopPatchDto = new ShopPatchDto()
                .visualCategories(catergoies);

        ShopDto updated = shopApiService.apiV1ShopPatchPost(testData.getSomeShop().getId(), shopPatchDto).getBody();

        Assertions.assertThat(updated).isNotNull();
        Assertions.assertThat(updated.getVisualCategories()).isEqualTo(catergoies);
    }

    @Test
    public void testPatchEnabled() {
        ShopPatchDto patch = new ShopPatchDto().enabled(false);

        ShopDto existing = MAPPER.toShopDto(testData.getSomeShopModel());
        ShopDto updated = shopApiService.apiV1ShopPatchPost(testData.getSomeShop().getId(), patch).getBody();

        Assertions.assertThat(updated)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringExpectedNullFields()
                .isEqualTo(patch)

                .ignoringFields("enabled")
                .ignoringCollectionOrder()
                .isEqualTo(existing);
    }

    @Test
    public void legalEntityUpdate() {
        authenticateUser(testData.getSomeBusinessAdminUid());
        Shop createdShop = testData.getSomeBusinessShops().iterator().next();

        LegalEntityPatchDto legalEntityPatchDto =
                new LegalEntityPatchDto().ezcountRefreshToken("token1234567").taxId("new1234567890");
        legalEntityApiService.apiV1LegalEntityPatchPost(createdShop.getLegalEntityId(), legalEntityPatchDto);

        ResponseEntity<ListShopsResponseDto> resp = shopApiService.apiV1ShopListGet();
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(resp.getBody()).isNotNull();
        ShopDto updated = resp.getBody().getItems().stream()
                .filter(it -> createdShop.getId().equals(it.getId())).findFirst().orElse(null);

        Assertions.assertThat(updated)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("ezcountRefreshToken", "taxId").build())
                .ignoringCollectionOrder()
                .ignoringExpectedNullFields()
                .isEqualTo(legalEntityPatchDto);
    }

    @Test
    void testListShops() {
        authenticateUser(testData.getSomeBusinessAdminUid());

        ListShopsResponseDto response = shopApiService.apiV1ShopListGet().getBody();
        //noinspection ConstantConditions
        Assertions.assertThat(response.getItems().toArray())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        // Assertj неправильно обрабатывает withComparedFields
                        // Поэтому, сравнить только по некоторым полям, можно только, если игнорировать
                        // все кроме них. Заклинание ниже как раз это и делает.
                        .withIgnoredFieldsMatchingRegexes("(?!(?:id|businessId)$).*")
                        .build()
                )
                .containsExactlyInAnyOrder(testData.getSomeBusinessShops().toArray());
    }

    private void authenticateUser(long uid) {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                "User uid=" + uid, "N/A"
        );
        authentication.setDetails(new PartnerUserDetails(new MockHttpServletRequest(), uid));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
