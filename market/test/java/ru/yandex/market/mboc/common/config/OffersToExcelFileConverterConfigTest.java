package ru.yandex.market.mboc.common.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.offers.MatchingOffer;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.converter.OffersToExcelFileConverter;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.utils.ModelTestUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleOffer;

/**
 * @author yuramalinov
 * @created 31.10.18
 */
public class OffersToExcelFileConverterConfigTest {
    private final static int SUPPLIER_ID = 1;

    private OffersExcelFileConverter<MatchingOffer> matchingConverter;
    private OffersToExcelFileConverter displayConverter;
    private OfferRepositoryMock offerRepository;

    @Before
    public void setUp() {
        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock();
        OffersToExcelFileConverterConfig config = new OffersToExcelFileConverterConfig(categoryCachingService);
        offerRepository = new OfferRepositoryMock();

        ModelStorageCachingServiceMock modelStorageService = new ModelStorageCachingServiceMock();
        modelStorageService.addModel(ModelTestUtils.publishedSku());
        matchingConverter = config.matchingConverter(modelStorageService, offerRepository);
        displayConverter = config.displayConverter();
    }

    @Test
    public void testNoCommentsOutput() {
        ExcelFile file = matchingConverter.convert(singletonList(simpleOffer()));
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle())).isNullOrEmpty();
    }

    @Test
    public void testSingleCommentOutput() {
        ExcelFile file = matchingConverter.convert(
            singletonList(simpleOffer().setContentComments(new ContentComment(ContentCommentType.DEPARTMENT_FROZEN))));
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle()))
            .isEqualTo(ContentCommentType.DEPARTMENT_FROZEN.getDescription());
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle())).isNullOrEmpty();
    }

    @Test
    public void testSingleCommentOutputWithItems() {
        ExcelFile file = matchingConverter.convert(
            singletonList(simpleOffer().setContentComments(
                new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "a", "b"))));
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle()))
            .isEqualTo(ContentCommentType.CONFLICTING_INFORMATION.getDescription());
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle())).isEqualTo("a, b");
    }

    @Test
    public void testThreeCommentOutputWithItems() {
        ExcelFile file = matchingConverter.convert(
            singletonList(simpleOffer().setContentComments(
                new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "a", "b"),
                new ContentComment(ContentCommentType.NO_PARAMETERS_IN_SHOP_TITLE, "c", "d"),
                new ContentComment(ContentCommentType.DEPARTMENT_FROZEN))));

        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle()))
            .isEqualTo(ContentCommentType.CONFLICTING_INFORMATION.getDescription());
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle())).isEqualTo("a, b");

        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE2.getTitle()))
            .isEqualTo(ContentCommentType.NO_PARAMETERS_IN_SHOP_TITLE.getDescription());
        assertThat(file.getValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS2.getTitle())).isEqualTo("c, d");
    }

    @Test
    public void testParseNoComments() {
        Offer offer = simpleOffer();
        offerRepository.setOffers(offer);
        ExcelFile file = matchingConverter.convert(singletonList(offer));
        OffersParseResult<MatchingOffer> result = matchingConverter.convert(file.toBuilder()
            .setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), ModelTestUtils.SKU_ID)
            .build());

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getOffers()).hasSize(1);
        assertThat(result.getOffer(0).getContentCommentType1()).isNull();
        assertThat(result.getOffer(0).getContentCommentType2()).isNull();
    }

    @Test
    public void testParseComments() {
        ExcelFile file = matchingConverter.convert(singletonList(simpleOffer()));
        OffersParseResult<MatchingOffer> result = matchingConverter.convert(file.toBuilder()
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle(),
                ContentCommentType.INCORRECT_INFORMATION.getDescription())
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle(), "a, b")
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE2.getTitle(),
                ContentCommentType.CONFLICTING_INFORMATION.getDescription())
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS2.getTitle(), "c, d, e")
            .build());

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getOffers()).hasSize(1);
        assertThat(result.getOffer(0).getContentCommentType1())
            .isEqualTo(ContentCommentType.INCORRECT_INFORMATION);
        assertThat(result.getOffer(0).getContentCommentItems1()).isEqualTo(Arrays.asList("a", "b"));
        assertThat(result.getOffer(0).getContentCommentType2())
            .isEqualTo(ContentCommentType.CONFLICTING_INFORMATION);
        assertThat(result.getOffer(0).getContentCommentItems2()).isEqualTo(Arrays.asList("c", "d", "e"));
    }

    @Test
    public void testParseWrongItems() {
        ExcelFile file = matchingConverter.convert(singletonList(simpleOffer()));
        OffersParseResult<MatchingOffer> result = matchingConverter.convert(file.toBuilder()
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_TYPE1.getTitle(), "Wrong Cell")
            .setValue(1, ExcelHeaders.CONTENT_COMMENT_ITEMS1.getTitle(), "a, b")
            .build());

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getErrors()).anyMatch(s -> s.contains("не совпадает ни с одним типовым комментарием"));
    }

    @Test
    public void testDisplayConverter() {
        Offer offer = simpleOffer()
            .setContentComment("Legacy comment")
            .setBusinessId(SUPPLIER_ID)
            .setServiceOffers(SUPPLIER_ID)
            .setContentComments(
                new ContentComment(ContentCommentType.DEPARTMENT_FROZEN),
                new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "information"));
        OfferForService offerForService = OfferForService.from(offer, SUPPLIER_ID);
        ExcelFile excelFile = displayConverter.convertOffersForService(
            Collections.singletonList(offerForService), Collections.emptySet());

        assertThat(excelFile.getValue(1, ExcelHeaders.FULL_CONTENT_COMMENT.getTitle()))
            .isEqualTo("Legacy comment\n" +
                "Работы в департаменте заморожены\n" +
                "Неверная информация: information");
    }

    @Test
    public void testNoWrongExtraFields() {
        ExcelFile file = matchingConverter.convert(singletonList(
            simpleOffer().storeOfferContent(OfferContent.builder().extraShopFields(ImmutableMap.of(
                "OK header", "Something",
                ExcelHeaders.CONTENT_COMMENT.getTitle(), "Some old comment!",
                "торговая марка", "vendor yay!"
            )).build())));

        Assertions.assertThat(file.getHeaders())
            .contains("OK header")
            .doesNotContain(ExcelHeaders.CONTENT_COMMENT.getTitle(), "торговая марка");
    }

    @Test
    public void testNoSupplierIdFound() {
        ExcelFile file = matchingConverter.convert(singletonList(simpleOffer()));
        OffersParseResult<MatchingOffer> result = matchingConverter.convert(file.toBuilder()
            .setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), ModelTestUtils.SKU_ID)
            .build());

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getErrors()).allMatch(s ->
            s.contains("Не удалось найти идентификатор поставщика для указанного идентификатора оффера"));
    }

    @Test
    public void veryLongStringInOffer() {
        var sb = new StringBuilder();
        for (int i = 0; i < 50_000; i++) {
            sb.append("x");
        }
        var offers = List.of(simpleOffer().setVendor(sb.toString()));
        ExcelFile file = matchingConverter.convert(offers);
        String savedValue = file.getValue(1, ExcelHeaders.VENDOR.getTitle());
        Assertions.assertThat(savedValue).hasSize(ExcelFile.EXCEL_MAXIMUM_CELL_LENGTH);
    }
}
