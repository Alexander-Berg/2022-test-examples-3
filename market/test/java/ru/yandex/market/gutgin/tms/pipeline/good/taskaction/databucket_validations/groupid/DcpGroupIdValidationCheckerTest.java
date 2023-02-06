package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.extractor.ExtractorConfig;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidationTest;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.MapUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidationTest.CATEGORY_WITH_VENDOR_SIZE_PARAM_ID;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidationTest.buildCategoryDataWithSizes;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.ENUM_OPTION_1;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.ENUM_OPTION_2;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.ENUM_PARAM_ID;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.OLD_ENUM_OPTION_1;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.OLD_ENUM_OPTION_2;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.OLD_ENUM_PARAM_ID;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.OLD_STRING_PARAM_ID;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.STRING_PARAM_ID;
import static ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataParamMigrationTest.VENDOR_OPTION;

public class DcpGroupIdValidationCheckerTest {
    public static final int GROUP_ID = 12;
    private static final long CATEGORY_WITH_PARAM_MIGRATION = 123L;
    private static final long CATEGORY_WITHOUT_SKU_DEFINING_PARAMS = 144L;
    private static final PosterCategory.Model FUNNY_POSTER_MODEL = new PosterCategory.Model("Hollywood star poster", "funny", "A4");

    private DcpGroupIdValidationChecker checker;
    private final Judge judge = new Judge();
    private final CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();

    @Before
    public void setUp() {
        categoryDataKnowledge.addCategoryData(PosterCategory.CATEGORY_ID, createCategoryDataMock());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITHOUT_SKU_DEFINING_PARAMS, createCategoryDataMockNoSku());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_PARAM_MIGRATION, buildCategoryWithParamMigration());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_VENDOR_SIZE_PARAM_ID, buildCategoryDataWithSizes());

        checker = new DcpGroupIdValidationChecker(categoryDataKnowledge, judge, null);
    }

    @Test
    public void shouldAcceptTicketsWithConsistentSizeParameters() {
        List<GcSkuTicket> tickets = SizeParamsConsistencyValidationTest.prepareGoodTicketsForTestingSizes();

        ProcessTaskResult<List<TicketValidationResult>> result = checker.validate(tickets, Map.of());
        assertThat(result.getResult()).allMatch(TicketValidationResult::isValid);
    }

    @Test
    public void shouldRejectTicketsWithEmptySkuDefiningParamsList() {
        GcSkuTicket ticket1 = generateTicketWithGroupName(1, "Group", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS);
        GcSkuTicket ticket2 = generateTicketWithGroupName(1, "Group", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS);

        ProcessTaskResult<List<TicketValidationResult>> result = checker.validate(Arrays.asList(ticket1, ticket2),
                Map.of());
        assertThat(result.getResult()).allMatch(TicketValidationResult -> !TicketValidationResult.isValid());
        assertThat(result.getResult()).allMatch(TicketValidationResult -> TicketValidationResult.getValidationMessages()
                .stream()
                .allMatch(messageInfo ->
                        messageInfo.getCode().equals("ir.partner_content.dcp.validation.groupId.noSkuDefiningParams")));
    }

    @Test
    public void shouldRejectTicketsEmptyOrInvalidGroupName() {
        List<GcSkuTicket> testList = Arrays.asList(
                generateTicketWithGroupName(1, "", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(1, "", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(2, "a", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(2, "a", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(3, "-", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(3, "-", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(4, "1-!", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(4, "1-!", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(5, " a ; - ", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(5, " a ; - ", CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(6, null, CATEGORY_WITHOUT_SKU_DEFINING_PARAMS),
                generateTicketWithGroupName(6, null, CATEGORY_WITHOUT_SKU_DEFINING_PARAMS));

                ProcessTaskResult<List<TicketValidationResult>> result = checker.validate(testList, Map.of());
        assertThat(result.getResult()).allMatch(TicketValidationResult -> !TicketValidationResult.isValid());
        assertThat(result.getResult()).allMatch(ticketValidationResult -> ticketValidationResult.getFailData() != null);
        assertThat(result.getResult()).allMatch(ticketValidationResult -> ticketValidationResult.getFailData().getParams().size() != 0);
    }

    @Test
    public void shouldAcceptTicketsValidGroupName() {
        List<GcSkuTicket> testList1 = generateTicketWithGroupNameAndParams(1, "Very good name");
        List<GcSkuTicket> testList2 = generateTicketWithGroupNameAndParams(1, "go2");

        ProcessTaskResult<List<TicketValidationResult>> result1 = checker.validate(testList1, Map.of());
        ProcessTaskResult<List<TicketValidationResult>> result2= checker.validate(testList2, Map.of());
        assertThat(result1.getResult()).allMatch(TicketValidationResult -> TicketValidationResult.isValid());
        assertThat(result2.getResult()).allMatch(TicketValidationResult -> TicketValidationResult.isValid());

    }

    private List<GcSkuTicket> generateTicketWithGroupNameAndParams(int groupId, String groupName) {
        List<GcSkuTicket> tickets = SizeParamsConsistencyValidationTest.prepareGoodTicketsForTestingSizes();

        tickets.forEach(t -> {
            t.setDcpGroupId(groupId);
            if (groupName != null) {
                DataCampOffer.Offer.Builder offerBuilder = t.getDatacampOffer().toBuilder();
                offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                        .getGroupNameBuilder().setValue(groupName);
                t.setDatacampOffer(offerBuilder.build());
            }
        });

        return tickets;
    }

    private GcSkuTicket generateTicketWithGroupName(int groupId, String groupName, Long categoryId) {
        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCategoryId(categoryId);
        ticket.setId(1L);
        ticket.setDcpGroupId(groupId);
        ticket.setShopSku("123");
        DataCampOffer.Offer.Builder offerBuilder2 = DataCampOffer.Offer.newBuilder();
        if (groupName != null) {
            offerBuilder2.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                    .getGroupNameBuilder().setValue(groupName);
        }
        ticket.setDatacampOffer(offerBuilder2.build());

        return ticket;
    }

    @Test
    public void shouldRejectTicketsWithInconsistentSizeParameters() {
        List<GcSkuTicket> tickets = SizeParamsConsistencyValidationTest.prepareBadTicketsForTestingSizes();

        ProcessTaskResult<List<TicketValidationResult>> result = checker.validate(tickets, Map.of());
        assertThat(result.getResult()).noneMatch(TicketValidationResult::isValid);
        assertThat(result.getResult()).allMatch(ticketValidationResult -> ticketValidationResult.getFailData() != null);
        assertThat(result.getResult()).allMatch(ticketValidationResult -> ticketValidationResult.getFailData().getParams().size() != 0);
    }

    @Test
    public void testTicketCheckedWithMigratedParams() {
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().getGroupNameBuilder().setValue("g1");
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(STRING_PARAM_ID)

                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                        .setStrValue("my_string")
                                        .build())
                                .build()

                )
                .addParameterValues(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(ENUM_PARAM_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                        .setOptionId(ENUM_OPTION_1)
                                        .build())
                                .build()
                );

        GcSkuTicket baseTicket = new GcSkuTicket();
        baseTicket.setDcpGroupId(GROUP_ID);
        baseTicket.setCategoryId(CATEGORY_WITH_PARAM_MIGRATION);

        baseTicket.setDatacampOffer(offerBuilder.build());
        baseTicket.setId(1L);
        baseTicket.setShopSku("shop_sku_1");

        GcSkuTicket otherTicket = new GcSkuTicket();
        otherTicket.setDcpGroupId(GROUP_ID);
        otherTicket.setCategoryId(CATEGORY_WITH_PARAM_MIGRATION);

        offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().getGroupNameBuilder().setValue("g1");
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .getParameterValuesBuilder()
                .addParameterValues(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(OLD_STRING_PARAM_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                        .setStrValue("my_string")
                                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                        .build())
                                .build()

                )
                .addParameterValues(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(OLD_ENUM_PARAM_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                        .setOptionId(OLD_ENUM_OPTION_1)
                                        .build())
                                .build()
                );
        otherTicket.setDatacampOffer(offerBuilder.build());
        otherTicket.setId(2L);
        otherTicket.setShopSku("shop_sku_2");

        ProcessTaskResult<List<TicketValidationResult>> validateResult =
                checker.validate(Arrays.asList(baseTicket, otherTicket), Map.of());

        assertThat(validateResult.hasResult()).isTrue();
        Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);

        assertThat(validateResultMap.keySet()).isEqualTo(ImmutableSet.of(1L, 2L));
    }

    @Test
    public void testTicketsOneVariationPerTime() {
        for (int i = 0; i < SKU_ALTERNATORS.size(); i++) {
            GcSkuTicket baseTicket = createTravoltaSku().createTicket(22L, null);

            PosterCategory.Sku otherSku = createTravoltaSku();
            SKU_ALTERNATORS.get(i).accept(otherSku);
            GcSkuTicket otherTicket = otherSku.createTicket(23L, null);

            ProcessTaskResult<List<TicketValidationResult>> validateResult =
                    checker.validate(Arrays.asList(baseTicket, otherTicket), Map.of());

            assertThat(validateResult.hasResult()).isTrue();
            Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);

            assertThat(validateResultMap.keySet()).isEqualTo(ImmutableSet.of(22L, 23L));
            assertThat(new HashSet<>(validateResultMap.values()))
                    .describedAs("Result for alteration #" + i)
                    .isEqualTo(Collections.singleton(Optional.empty()));
        }
    }

    @Test
    public void testTicketsAllVariationsAtOnce() {
        GcSkuTicket baseTicket = createTravoltaSku().createTicket(22L, null);

        PosterCategory.Sku otherSku = createTravoltaSku();

        for (int i = 0; i < SKU_ALTERNATORS.size(); i++) {
            SKU_ALTERNATORS.get(i).accept(otherSku);
        }

        GcSkuTicket otherTicket = otherSku.createTicket(23L, null);

        ProcessTaskResult<List<TicketValidationResult>> validateResult =
                checker.validate(Arrays.asList(baseTicket, otherTicket), Map.of());

        assertThat(validateResult.hasResult()).isTrue();
        Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);
        assertThat(validateResultMap.keySet()).isEqualTo(ImmutableSet.of(22L, 23L));
        assertThat(new HashSet<>(validateResultMap.values()))
                .isEqualTo(Collections.singleton(Optional.empty()));
    }

    @Test
    public void testIdenticalTickets() {
        PosterCategory.Sku travoltaSku = createTravoltaSku();
        GcSkuTicket ticket1 = travoltaSku.createTicket(22L, null);
        GcSkuTicket ticket2 = travoltaSku.createTicket(23L, null);

        PosterCategory.Sku otherSku = createTravoltaSku();
        otherSku.persons = Collections.singletonList(PosterCategory.Person.TILDA);
        GcSkuTicket ticket3 = otherSku.createTicket(24L, null);
        GcSkuTicket ticket4 = otherSku.createTicket(25L, null);
        GcSkuTicket ticket5 = otherSku.createTicket(26L, null);
        GcSkuTicket ticket6 = otherSku.createTicket(27L, null);

        ProcessTaskResult<List<TicketValidationResult>> validateResult =
                checker.validate(Arrays.asList(ticket1, ticket2, ticket3, ticket4, ticket5, ticket6), Map.of());

        assertThat(validateResult.hasResult()).isTrue();
        Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);
        assertThat(validateResultMap.keySet().size()).isEqualTo(6);

        assertThat(validateResultMap.values()).allMatch(Optional::isPresent);

        MessageInfo messageFor22 = validateResultMap.get(22L).get();
        assertThat(messageFor22.getParams().get("otherShopSkus")).isEqualTo(new String[] {"shop_sku_23"});
        assertThat(getAll(ImmutableList.of(22L, 23L), validateResultMap))
            .allMatch(o -> ((Object[])o.get().getParams().get("otherShopSkus")).length == 1)
            .allMatch(o -> ((Object[])o.get().getParams().get("parameterNames")).length == 5);

        MessageInfo messageFor26 = validateResultMap.get(26L).get();
        assertThat(messageFor26.getParams().get("otherShopSkus")).isEqualTo(new String[] {"shop_sku_24", "shop_sku_25", "shop_sku_27"});

        assertThat(getAll(ImmutableList.of(24L, 25L, 26L, 27L), validateResultMap))
            .allMatch(o -> ((Object[])o.get().getParams().get("otherShopSkus")).length == 3)
            .allMatch(o -> ((Object[])o.get().getParams().get("parameterNames")).length == 5);
    }

    @Test
    public void skuDefiningParamIsNullInOneTicket() {
        PosterCategory.Model model = createSovietCartoonModel();
        PosterCategory.Sku poohSku = createWinnieThePoohSku(model);
        PosterCategory.Sku gavSku = createGavSkuWithNull(model);
        GcSkuTicket ticket1 = poohSku.createTicket(1, 10L);
        GcSkuTicket ticket2 = gavSku.createTicket(2, 11L);

        ProcessTaskResult<List<TicketValidationResult>> validate =
                checker.validate(Arrays.asList(ticket1, ticket2), Map.of());

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(2);
        //valid ticket
        TicketValidationResult result1 = result.get(0);
        assertThat(result1.isValid()).isTrue();
        assertThat(result1.getValidationMessages()).isEmpty();
        //для офера у которого параметр есть - не пишем failData
        assertThat(result1.getFailData()).isNull();

        //ticket with empty 'person' parameter
        TicketValidationResult result2 = result.get(1);
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.getValidationMessages()).hasSize(1);
        assertThat(result2.getFailData()).isNotNull();
        assertThat(result2.getFailData().getParams()).hasSize(1);
        MessageInfo actualMessage = result2.getValidationMessages().get(0);
        assertThat(actualMessage).extracting(MessageInfo::getCode)
            .isEqualTo("ir.partner_content.dcp.validation.groupId.ticketHasNullInSkuDefiningParameter");
        assertThat(actualMessage.getParams().get("thisShopSku")).isEqualTo("shop_sku_2");
        assertThat(actualMessage.getParams().get("otherShopSkus")).isEqualTo(new String[]{"shop_sku_1"});
        assertThat(actualMessage.getParams().get("modelParameterName")).isEqualTo("person");
    }

    @Test
    public void skuDefiningParamIsNullInAllTickets() {
        PosterCategory.Model model = createSovietCartoonModel();
        PosterCategory.Sku poohSku = createWinnieThePoohSkuWithNull(model);
        PosterCategory.Sku gavSku = createGavSkuWithNull(model);
        GcSkuTicket ticket1 = poohSku.createTicket(1, 10L);
        GcSkuTicket ticket2 = gavSku.createTicket(2, 11L);

        ProcessTaskResult<List<TicketValidationResult>> validate = checker.validate(Arrays.asList(ticket1, ticket2), Map.of());

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TicketValidationResult::isValid).containsOnly(true);
        assertThat(result).allSatisfy(singleResult -> assertThat(singleResult.getValidationMessages()).isEmpty());
        assertThat(result).allSatisfy(singleResult -> assertThat(singleResult.getFailData()).isNull());
    }

    private static PosterCategory.Model createSovietCartoonModel() {
        return new PosterCategory.Model("Posters of Soviet cartoons", "Soviet cartoons", "A4");
    }

    private static PosterCategory.Sku createFuntikSku(PosterCategory.Model sovietCartoonPostModel) {
        return new PosterCategory.Sku(sovietCartoonPostModel,
                Collections.singletonList("Funtik"), Arrays.asList("He has run from Ms. Beladonna!", "Like honest honest?"),
                PosterCategory.Size.TEN, 1986, "Funtik", false);
    }

    private static PosterCategory.Sku createWinnieThePoohSku(PosterCategory.Model sovietCartoonPostModel) {
        return new PosterCategory.Sku(sovietCartoonPostModel,
                Collections.singletonList("WinnieThePooh"), Collections.emptyList(),
                PosterCategory.Size.TEN, 1969, "Pooh", false);
    }

    private static PosterCategory.Sku createGavSku(PosterCategory.Model sovietCartoonPostModel) {
        return new PosterCategory.Sku(sovietCartoonPostModel,
                Collections.singletonList("Gav the Kitten"), Collections.singletonList("Why keep troubles waiting?"),
                PosterCategory.Size.TEN, 1976, "Gav the kitten", false);
    }

    private static PosterCategory.Sku createGavSkuWithNull(PosterCategory.Model sovietCartoonPostModel) {
        return new PosterCategory.Sku(sovietCartoonPostModel,
                Collections.emptyList(), Collections.singletonList("Why keep troubles waiting?"),
                PosterCategory.Size.TEN, 1976, "Gav the kitten", false);
    }

    private static PosterCategory.Sku createWinnieThePoohSkuWithNull(PosterCategory.Model sovietCartoonPostModel) {
        return new PosterCategory.Sku(sovietCartoonPostModel,
            Collections.emptyList(), Collections.emptyList(),
            PosterCategory.Size.TEN, 1969, "Pooh", false);
    }

    @Test
    public void testDifferentModelParams() {
        PosterCategory.Model model1 = createSovietCartoonModel();
        PosterCategory.Model model2 = createSovietCartoonModel();
        PosterCategory.Model model3 = createSovietCartoonModel();
        model3.theme = "russian cartoons";
        model3.name = "different name";//NAME_ID ignored

        PosterCategory.Sku funtikPosterSku = createFuntikSku(model1);
        PosterCategory.Sku winnieThePoohSku = createWinnieThePoohSku(model2);
        PosterCategory.Sku gavPosterSku = createGavSku(model3);

        // Offers are in the same group.
        {
            GcSkuTicket ticket1 = funtikPosterSku.createTicket(10, 20L, GROUP_ID);
            GcSkuTicket ticket2 = gavPosterSku.createTicket(11, 21L, GROUP_ID);
            GcSkuTicket ticket3 = winnieThePoohSku.createTicket(12, 22L, GROUP_ID);

            ProcessTaskResult<List<TicketValidationResult>> validateResult =
                    checker.validate(Arrays.asList(ticket1, ticket2, ticket3), Map.of());

            Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);

            assertThat(getAll(ImmutableList.of(10L, 12L), validateResultMap))
                .allMatch(o -> o.get().getCode().contains("ticketHasOtherModelParams"))
                .allMatch(o -> ((Object[])o.get().getParams().get("modelParameterNames")).length == 1)
                .allMatch(o -> ((Object[])o.get().getParams().get("otherShopSkus")).length == 1);

            assertThat(getAll(ImmutableList.of(11L), validateResultMap))
                .allMatch(o -> o.get().getCode().contains("ticketHasOtherModelParams"))
                .allMatch(o -> ((Object[])o.get().getParams().get("modelParameterNames")).length == 1)
                .allMatch(o -> ((Object[])o.get().getParams().get("otherShopSkus")).length == 2);
        }

        // Offers are explicitly in different groups.
        {
            GcSkuTicket ticket1 = funtikPosterSku.createTicket(10, 20L, GROUP_ID);
            GcSkuTicket ticket2 = gavPosterSku.createTicket(11, 21L, GROUP_ID + 1);

            ProcessTaskResult<List<TicketValidationResult>> validateResult =
                    checker.validate(Arrays.asList(ticket1, ticket2), Map.of());

            Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);

            assertThat(getAll(ImmutableList.of(10L, 11L), validateResultMap)).noneMatch(Optional::isPresent);
        }

        // Offers are in different groups as implicitly coded by null group id.
        {
            GcSkuTicket ticket1 = funtikPosterSku.createTicket(10, 20L, null);
            GcSkuTicket ticket2 = gavPosterSku.createTicket(11, 21L, null);

            ProcessTaskResult<List<TicketValidationResult>> validateResult =
                    checker.validate(Arrays.asList(ticket1, ticket2), Map.of());

            Map<Long, Optional<MessageInfo>> validateResultMap = parseResult(validateResult);

            assertThat(getAll(ImmutableList.of(10L, 11L), validateResultMap)).noneMatch(Optional::isPresent);
        }
    }

    private static <V> Collection<V> getAll(Collection<?> keys, Map<?, V> map) {
        List<V> result = new ArrayList<>();
        for (Object k : keys) {
            result.add(map.get(k));
        }
        return result;
    }

    @Test
    public void testHypothesesWork() {
        CategoryData categoryData = CategoryData.EMPTY;
        PosterCategory.Sku travoltaSku = createTravoltaSku();

        PosterCategory.Sku travoltaSkuWithHypotheses = createTravoltaSku();
        travoltaSkuWithHypotheses.persons = Collections.singletonList(PosterCategory.Person.TRAVOLTA.text);
        travoltaSkuWithHypotheses.size = PosterCategory.Size.FIVE.num;

        ParamVectorFetcher allParamFetcher = new ParamVectorFetcher(PosterCategory.ALL_PARAMS, categoryData);

        ParamVector forSkuModel = allParamFetcher.getVectorForSkuModel(travoltaSku.createModelSku(25L));
        ParamVector forSkuModelHypo = allParamFetcher.getVectorForSkuModel(travoltaSkuWithHypotheses.createModelSku(26L));

        ParamVector forTicket = allParamFetcher.getVectorForTicket(travoltaSku.createTicket(1L, null), Set.of());
        ParamVector forTicketHypo = allParamFetcher.getVectorForTicket(travoltaSkuWithHypotheses.createTicket(2L, null), Set.of());

        assertThat(Stream.of(forSkuModel, forSkuModelHypo, forTicket, forTicketHypo).distinct().count()).isEqualTo(1);
    }

    @Test
    public void testHypothesesEvaluationGivesCorrectOptionId() {
        CategoryData categoryData = CategoryData.EMPTY;
        PosterCategory.Sku travoltaSku = createTravoltaSku();
        travoltaSku.persons = Collections.singletonList(PosterCategory.Person.TRAVOLTA);

        PosterCategory.Sku travoltaSkuWithHypotheses = createTravoltaSku();
        travoltaSkuWithHypotheses.persons = Collections.singletonList(PosterCategory.Person.
            TRAVOLTA.text.toLowerCase());

        ParamVectorFetcher allParamFetcher = new ParamVectorFetcher(PosterCategory.ALL_PARAMS, categoryData);

        ParamVector forSkuModel = allParamFetcher.getVectorForSkuModel(travoltaSku.createModelSku(25L));
        ParamVector forSkuModelHypo = allParamFetcher.getVectorForSkuModel(travoltaSkuWithHypotheses.createModelSku(26L));

        ParamVector forTicket = allParamFetcher.getVectorForTicket(travoltaSku.createTicket(1L, null), Set.of());
        ParamVector forTicketHypo = allParamFetcher.getVectorForTicket(travoltaSkuWithHypotheses.createTicket(2L, null), Set.of());

        //Проверяем, что гипотеза с "john travolta" заматчилась на значение enum "John Travolta"
        assertThat(Stream.of(forSkuModel, forSkuModelHypo, forTicket, forTicketHypo).distinct().count()).isEqualTo(1);
    }

    @Test
    public void testWhenEqualSkuDefiningParamsThenEqualVectors() {
        MboParameters.Parameter boxParam = MboParameters.Parameter.newBuilder()
                .setId(15779091L)
                .setXslName("box")
                .setValueType(MboParameters.ValueType.ENUM)
                .setParamType(MboParameters.ParameterLevel.OFFER_LEVEL)
                .setIsUseForGuru(true)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .addOption(
                        MboParameters.Option.newBuilder()
                                .setId(15779092L)
                                .addName(MboParameters.Word.newBuilder().setName("картонная коробка").build())
                                .build()
                )
                .build();
        MboParameters.Parameter vesGlParam = MboParameters.Parameter.newBuilder()
                .setId(23674510L)
                .setXslName("ves_gl")
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setParamType(MboParameters.ParameterLevel.OFFER_LEVEL)
                .setIsUseForGuru(true)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build();

        MboParameters.Parameter vendor = MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(ParameterValueComposer.VENDOR)
                .setValueType(MboParameters.ValueType.ENUM)
                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                .setIsUseForGuru(true)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .addOption(
                        MboParameters.Option.newBuilder()
                                .setId(VENDOR_OPTION)
                                .addName(MboParameters.Word.newBuilder().setName("vendor").build())
                                .build()
                )
                .build();

        CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
                .setHid(16011796L)
                .addParameter(boxParam)
                .addParameter(vesGlParam)
                .addParameter(vendor)
        );

        List<MboParameters.Parameter> skuDefParams = Arrays.asList(boxParam, vesGlParam);

        ParamVectorFetcher paramFetcher = new ParamVectorFetcher(skuDefParams, categoryData);
        GcSkuTicket ticket1 = new GcSkuTicket();
        GcSkuTicket ticket2 = new GcSkuTicket();
        ticket1.setCategoryId(16011796L);
        ticket2.setCategoryId(16011796L);

        DataCampContentMarketParameterValue.MarketParameterValue marketBox =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                .setParamId(15779091L)
                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                        .setOptionId(15779092L)
                        .build())
                .build();

        DataCampContentMarketParameterValue.MarketParameterValue marketWeight =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                .setParamId(23674510L)
                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                        .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                        .setNumericValue("50")
                        .build())
                .build();


        Market.DataCamp.DataCampOffer.Offer datacampOffer = DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                                                .addParameterValues(marketBox)
                                                .addParameterValues(marketWeight)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Market.DataCamp.DataCampOffer.Offer datacampOffer2 = DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                                                .addParameterValues(marketWeight)
                                                .addParameterValues(marketBox)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        ticket1.setDatacampOffer(datacampOffer);
        ticket2.setDatacampOffer(datacampOffer2);
        ParamVector vectorForTicket1 = paramFetcher.getVectorForTicket(ticket1, Set.of());
        ParamVector vectorForTicket2 = paramFetcher.getVectorForTicket(ticket2, Set.of());
        boolean unique = !vectorForTicket1.equals(vectorForTicket2);
       assertThat(unique).isFalse();
    }


    @Test
    public void testWhenMultiValueModelParamsThenEqualVectors() {
        MboParameters.Parameter boxParam = MboParameters.Parameter.newBuilder()
                .setId(15779091L)
                .setXslName("box")
                .setValueType(MboParameters.ValueType.ENUM)
                .setParamType(MboParameters.ParameterLevel.OFFER_LEVEL)
                .setIsUseForGuru(true)
                .setMultivalue(true)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .addOption(
                        MboParameters.Option.newBuilder()
                                .setId(15779092L)
                                .addName(MboParameters.Word.newBuilder().setName("картонная коробка")
                                        .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
                                        .build())
                                .build()
                )
                .addOption(
                        MboParameters.Option.newBuilder()
                                .setId(157790921L)
                                .addName(MboParameters.Word.newBuilder().setName("пластиковая коробка")
                                        .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
                                        .build())
                                .build()
                )
                .build();

        MboParameters.Parameter vendor = MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(ParameterValueComposer.VENDOR)
                .setValueType(MboParameters.ValueType.ENUM)
                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                .setIsUseForGuru(true)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .addOption(
                        MboParameters.Option.newBuilder()
                                .setId(VENDOR_OPTION)
                                .addName(MboParameters.Word.newBuilder().setName("vendor").build())
                                .build()
                )
                .build();


        CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
                .setHid(16011796L)
                .addParameter(boxParam)
                .addParameter(vendor)
        );

        List<MboParameters.Parameter> modelParams = Arrays.asList(boxParam);

        ParamVectorFetcher paramFetcher = new ParamVectorFetcher(modelParams, categoryData);
        GcSkuTicket ticket1 = new GcSkuTicket();
        GcSkuTicket ticket2 = new GcSkuTicket();
        ticket1.setCategoryId(16011796L);
        ticket2.setCategoryId(16011796L);

        DataCampContentMarketParameterValue.MarketParameterValue marketBox1 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(15779091L)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .setOptionId(15779092L)
                                .build())
                        .build();

        DataCampContentMarketParameterValue.MarketParameterValue marketBox2 =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(15779091L)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .setOptionId(157790921L)
                                .build())
                        .build();


        Market.DataCamp.DataCampOffer.Offer datacampOffer = DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                                                .addParameterValues(marketBox1)
                                                .addParameterValues(marketBox2)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Market.DataCamp.DataCampOffer.Offer datacampOffer2 = DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                                                .addParameterValues(marketBox2)
                                                .addParameterValues(marketBox1)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        ticket1.setDatacampOffer(datacampOffer);
        ticket2.setDatacampOffer(datacampOffer2);
        ParamVector vectorForTicket1 = paramFetcher.getVectorForTicket(ticket1, Set.of());
        ParamVector vectorForTicket2 = paramFetcher.getVectorForTicket(ticket2, Set.of());
        assertThat(vectorForTicket1.equals(vectorForTicket2)).isTrue();
    }

    @Test
    public void validateParamCountAsNonNullWhenItIsUnmodifiable() {
        ModelStorageHelper modelStorageHelper = mock(ModelStorageHelper.class);
        checker = new DcpGroupIdValidationChecker(categoryDataKnowledge, judge, modelStorageHelper);
        PosterCategory.Model model = createSovietCartoonModel();
        PosterCategory.Sku poohSku = createWinnieThePoohSku(model);
        PosterCategory.Sku gavSku = createGavSkuWithNull(model);
        GcSkuTicket ticket1 = poohSku.createTicket(1, 10L);
        GcSkuTicket ticket2 = gavSku.createTicket(2, 11L);

        ModelStorage.Model gavSkuInStorage = ModelStorage.Model.newBuilder()
                .setId(11L)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1002)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("person").build())
                        .setValueType(MboParameters.ValueType.STRING)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .build();
        when(modelStorageHelper.findModelHierarchy(any())).thenReturn(Map.of(11L, gavSkuInStorage));

        ProcessTaskResult<List<TicketValidationResult>> validate =
                checker.validate(Arrays.asList(ticket1, ticket2), Map.of());

        List<TicketValidationResult> result = validate.getResult();
        assertThat(result).hasSize(2);
        //valid ticket
        TicketValidationResult result1 = result.get(0);
        assertThat(result1.isValid()).isTrue();
        assertThat(result1.getValidationMessages()).isEmpty();
        //для офера у которого параметр есть - не пишем failData
        assertThat(result1.getFailData()).isNull();

        //ticket with empty 'person' parameter
        TicketValidationResult result2 = result.get(1);
        assertThat(result2.isValid()).isTrue();
        assertThat(result2.getValidationMessages()).isEmpty();
        assertThat(result2.getFailData()).isNull();
    }


    private static Map<Long, Optional<MessageInfo>> parseResult(ProcessTaskResult<List<TicketValidationResult>> validateResult) {
        return MapUtils.toMap(validateResult.getResult(), TicketValidationResult::getTicketId, i -> {
            if (i.getValidationMessages().isEmpty()) {
                return Optional.empty();
            } else if (i.getValidationMessages().size() == 1) {
                return Optional.of(i.getValidationMessages().get(0));
            } else {
                throw new RuntimeException("Not supported");
            }
        });

    }

    private static CategoryData buildCategoryWithParamMigration() {
        return CategoryData.build(MboParameters.Category.newBuilder()
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(ParameterValueComposer.VENDOR_ID)
                                .setXslName(ParameterValueComposer.VENDOR)
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(VENDOR_OPTION)
                                                .addName(MboParameters.Word.newBuilder().setName("vendor").build())
                                                .build()
                                )
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(ENUM_PARAM_ID)
                                .setXslName("ENUM_PARAM_ID")
                                .addName(MboParameters.Word.newBuilder()
                                        .setName("ENUM_PARAM_ID")
                                        .setLangId(225)
                                        .build())
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setIsUseForGuru(true)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(ENUM_OPTION_1)
                                                .addName(MboParameters.Word.newBuilder()
                                                        .setName("ENUM_OPTION_1")
                                                        .setLangId(225)
                                                        .build())
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option.newBuilder()
                                                .setId(ENUM_OPTION_2)
                                                .addName(MboParameters.Word.newBuilder()
                                                        .setName("ENUM_OPTION_2")
                                                        .setLangId(225)
                                                        .build())
                                                .build()
                                )
                                .setMultivalue(true)
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(STRING_PARAM_ID)
                                .setXslName("STRING_PARAM_ID")
                                .addName(MboParameters.Word.newBuilder()
                                        .setName("STRING_PARAM_ID")
                                        .setLangId(225)
                                        .build())
                                .setValueType(MboParameters.ValueType.STRING)
                                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                .setExtractInSkubd(false)
                                .build()
                )
                .addParametersMapping(
                        MboParameters.ParameterMigration.newBuilder()
                                .setSourceParamId(OLD_ENUM_PARAM_ID)
                                .setTargetParamId(ENUM_PARAM_ID)
                                .addOptionsMigration(
                                        MboParameters.OptionMigration.newBuilder()
                                                .setSourceOptionId(OLD_ENUM_OPTION_1)
                                                .setTargetOptionId(ENUM_OPTION_1)
                                                .build()
                                )
                                .addOptionsMigration(
                                        MboParameters.OptionMigration.newBuilder()
                                                .setSourceOptionId(OLD_ENUM_OPTION_2)
                                                .setTargetOptionId(ENUM_OPTION_2)
                                                .build()
                                )
                                .build()
                )
                .addParametersMapping(
                        MboParameters.ParameterMigration.newBuilder()
                                .setSourceParamId(OLD_STRING_PARAM_ID)
                                .setTargetParamId(STRING_PARAM_ID)
                                .build()
                )
                .build());
    }

    private static CategoryData createCategoryDataMock() {
        Map<Long, MboParameters.Parameter> id2Param = PosterCategory.ALL_PARAMS.stream()
                .collect(Collectors.toMap(MboParameters.Parameter::getId, Functions.identity()));
        LongSet skuDefininingIds = new LongArraySet(
                PosterCategory.ALL_PARAMS.stream()
                        .filter(p -> p.getSkuMode() == MboParameters.SKUParameterMode.SKU_DEFINING)
                        .map(MboParameters.Parameter::getId)
                        .collect(Collectors.toList())
        );

        CategoryData categoryMock = mock(CategoryData.class);
        when(categoryMock.getParameterList()).thenReturn(new ArrayList<>(PosterCategory.ALL_PARAMS));
        when(categoryMock.getSkuDefiningParamIds()).thenReturn(skuDefininingIds);
        when(categoryMock.getModelParamIds()).thenAnswer(invocation -> {
            throw new RuntimeException();
        });
        when(categoryMock.getParamById(anyLong())).thenAnswer(invocation -> {
            long paramId = invocation.getArgument(0);
            return id2Param.get(paramId);
        });
        when(categoryMock.getSizeParamIds()).thenAnswer(invocation -> LongSets.EMPTY_SET);
        return categoryMock;
    }

    private static CategoryData createCategoryDataMockNoSku() {
        Map<Long, MboParameters.Parameter> id2Param = PosterCategory.ALL_PARAMS.stream()
                .collect(Collectors.toMap(MboParameters.Parameter::getId, Functions.identity()));
        LongSet skuDefininingIds = new LongArraySet();

        CategoryData categoryMock = mock(CategoryData.class);
        when(categoryMock.getParameterList()).thenReturn(new ArrayList<>(PosterCategory.ALL_PARAMS));
        when(categoryMock.getSkuDefiningParamIds()).thenReturn(skuDefininingIds);
        when(categoryMock.getModelParamIds()).thenAnswer(invocation -> {
            throw new RuntimeException();
        });
        when(categoryMock.getParamById(anyLong())).thenAnswer(invocation -> {
            long paramId = invocation.getArgument(0);
            return id2Param.get(paramId);
        });
        when(categoryMock.getSizeParamIds()).thenAnswer(invocation -> LongSets.EMPTY_SET);
        return categoryMock;
    }

    private static PosterCategory.Sku createTravoltaSku() {
        return new PosterCategory.Sku(
                FUNNY_POSTER_MODEL, Collections.singletonList(PosterCategory.Person.TRAVOLTA),
                Arrays.asList("agaga", "rrtt"), PosterCategory.Size.FIVE.num, 2000, "Hobbit", false
        );
    }

    private static final List<Consumer<PosterCategory.Sku>> SKU_ALTERNATORS = Arrays.asList(
            s -> s.persons = Arrays.asList(PosterCategory.Person.TRAVOLTA, PosterCategory.Person.TILDA),
            s -> s.size = PosterCategory.Size.TEN,
            s -> s.year = 2001,
            s -> s.flickName = "Home Alone",
            s -> s.adult = true
    );

}
