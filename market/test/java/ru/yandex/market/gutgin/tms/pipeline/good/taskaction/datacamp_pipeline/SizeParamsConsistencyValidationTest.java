package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import Market.DataCamp.DataCampContentMarketParameterValue.MarketParameterValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent.OfferContent;
import Market.DataCamp.DataCampOfferContent.PartnerContent;
import Market.DataCamp.DataCampOfferMarketContent.MarketParameterValues;
import Market.DataCamp.DataCampOfferMarketContent.MarketSpecificContent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataForSizeParametersMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType.NUMERIC;
import static Market.DataCamp.DataCampContentMarketParameterValue.MarketValueType.STRING;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidationTest.ParameterBuilder.numeric;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidationTest.ParameterBuilder.string;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType.SIZE_PARAMS_CONSISTENCY;

public class SizeParamsConsistencyValidationTest {
    public static final long CATEGORY_WITH_VENDOR_SIZE_PARAM_ID = 1L;
    private static final long CATEGORY_WITH_ONE_SIZE_PARAM_ID = 0L;
    private static final long CATEGORY_WITH_MANY_SIZE_PARAMS_ID = 2L;
    private static final long CATEGORY_WITH_MANY_SIZE_PARAMS_ID_WITH_MANDATORY_SIZE = 3L;
    private static final long CATEGORY_WITH_MANDATORY_UNITED_SIZE = 4L;
    private static final long CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE = 5L;

    private static final long VENDOR_SIZE_PARAM_ID = 1L;
    private static final long VENDOR_SIZE_PARAM_MIN_ID = 1_0L;
    private static final long VENDOR_SIZE_PARAM_MAX_ID = 1_1L;
    private static final long SIZE_PARAM_1_ID = 2L;
    private static final long SIZE_PARAM_1_MIN_ID = 2_0L;
    private static final long SIZE_PARAM_1_MAX_ID = 2_1L;
    private static final long SIZE_PARAM_2_ID = 3L;
    private static final long SIZE_PARAM_2_MIN_ID = 3_0L;
    private static final long SIZE_PARAM_2_MAX_ID = 3_1L;
    private static final long SIZE_PARAM_3_ID = 4L;
    private static final long SIZE_PARAM_3_MIN_ID = 4_0L;
    private static final long SIZE_PARAM_3_MAX_ID = 4_1L;

    private static final String SHOP_SKU = "test";

    private SizeParamsConsistencyValidation sizeParamsConsistencyValidation;
    private MessageReporter messageReporter;

    @Before
    public void setUp() throws Exception {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledge, null);
        GcSkuValidationDao gcSkuValidationDao = Mockito.mock(GcSkuValidationDao.class);
        GcSkuTicketDao gcSkuTicketDao = Mockito.mock(GcSkuTicketDao.class);
        sizeParamsConsistencyValidation = new SizeParamsConsistencyValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataHelper);
        messageReporter = new MessageReporter(SHOP_SKU);

        categoryDataKnowledge.addCategoryData(
                CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                buildCategoryDataWithSizes()
        );

        categoryDataKnowledge.addCategoryData(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID,
                new CategoryDataForSizeParametersMock(
                        new HashMap<Long, Pair<Long, Long>>() {
                            {
                                put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                            }
                        },
                        generateParameter(SIZE_PARAM_1_ID),
                        generateParameter(SIZE_PARAM_1_MIN_ID),
                        generateParameter(SIZE_PARAM_1_MAX_ID)
                )
        );

        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_MANY_SIZE_PARAMS_ID, buildCategoryDataWithSizes());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_MANY_SIZE_PARAMS_ID_WITH_MANDATORY_SIZE,
                buildCategoryDataWithOneMandatorySize());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_MANDATORY_UNITED_SIZE,
                buildCategoryDataWithMandatoryUnitedSize());
        categoryDataKnowledge.addCategoryData(CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE,
                buildCategoryDataWithMandatoryUnitedSizeAndRange());
    }

    @Test
    public void shouldAcceptTicketWithConsistentOneSizeParameter() {
        GcSkuTicket gcSkuTicketWithOnlyMinParameter = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID, SIZE_PARAM_1_ID, SIZE_PARAM_1_MIN_ID
        );

        GcSkuTicket gcSkuTicketWithOnlyMaxParameter = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID, SIZE_PARAM_1_ID, SIZE_PARAM_1_MAX_ID
        );

        GcSkuTicket gcSkuTicketWithBothSizeParams = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID, SIZE_PARAM_1_ID, SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithOnlyMinParameter, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithOnlyMaxParameter, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithBothSizeParams, messageReporter, Set.of());

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void shouldRejectTicketWithInconsistentOneSizeParameter() {
        GcSkuTicket gcSkuTicketWithoutMinMaxParameters = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID,
                SIZE_PARAM_1_ID
        );

        GcSkuTicket gcSkuTicketWithDifferentSizeParameter = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID,
                SIZE_PARAM_2_ID,
                SIZE_PARAM_2_MAX_ID
        );

        GcSkuTicket gcSkuTicketWithoutSizeParams = createTicketWith(CATEGORY_WITH_ONE_SIZE_PARAM_ID, new Long[0]);

        GcSkuTicket gcSkuTicketWithInconsistentSizeAndSizeMinParameters = createTicketWith(
                CATEGORY_WITH_ONE_SIZE_PARAM_ID,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_2_MAX_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutMinMaxParameters, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithDifferentSizeParameter, messageReporter,
                Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutSizeParams, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(
                gcSkuTicketWithInconsistentSizeAndSizeMinParameters,
                messageReporter, Set.of()
        );

        assertThat(messageReporter.getMessages().size()).isEqualTo(4);

        FailData failData = messageReporter.getFailData();
        // ошибка по отсутствующим параметрам - нечего запоминать
        assertThat(failData).isNull();
    }

    @Test
    public void shouldAcceptTicketWithConsistentVendorSizeParams() {
        GcSkuTicket gcSkuTicketWithOnlyMaxParam = createTicketWith(
                CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                VENDOR_SIZE_PARAM_ID,
                VENDOR_SIZE_PARAM_MAX_ID
        );

        GcSkuTicket gcSkuTicketWithOnlyMinParam = createTicketWith(
                CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                VENDOR_SIZE_PARAM_ID,
                VENDOR_SIZE_PARAM_MIN_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithOnlyMaxParam, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithOnlyMinParam, messageReporter, Set.of());

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void shouldAcceptTicketWithConsistentMultipleSizedCategoryParameters() {
        GcSkuTicket gcSkuTicketWithParam1Min = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_1_MIN_ID
        );

        GcSkuTicket gcSkuTicketWithParam1Max = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_1_MAX_ID
        );

        GcSkuTicket gcSkuTicketWithParam1 = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_1_MIN_ID,
                SIZE_PARAM_1_MAX_ID
        );

        GcSkuTicket gcSkuTicketWithParam3 = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_3_ID,
                SIZE_PARAM_3_MIN_ID,
                SIZE_PARAM_3_MAX_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1Min, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1Max, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam3, messageReporter, Set.of());

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void shouldRejectTicketWithInconsistentMultipleSizedCategoryParameters() {
        GcSkuTicket gcSkuTicketWithParam1MinInconsistent = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_2_MIN_ID
        );

        GcSkuTicket gcSkuTicketWithParam1WithoutMinmax = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                SIZE_PARAM_2_MIN_ID
        );

        GcSkuTicket gcSkuTicketWithoutParams = createTicketWith(CATEGORY_WITH_MANY_SIZE_PARAMS_ID, new Long[0]);

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1MinInconsistent, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1WithoutMinmax, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutParams, messageReporter, Set.of());

        assertThat(messageReporter.getMessages().size()).isEqualTo(3);

        FailData failData = messageReporter.getFailData();
        // ошибка по отсутствующим параметрам - нечего запоминать
        assertThat(failData).isNull();
    }


    @Test
    public void shouldRejectTicketWithInconsistentMultipleMandatorySizedCategoryParameters() {
        GcSkuTicket gcSkuTicketWithParam1MinConsistent = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID_WITH_MANDATORY_SIZE,
                SIZE_PARAM_1_ID,
                SIZE_PARAM_1_MIN_ID
        );

        GcSkuTicket gcSkuTicketWithParam1WithoutMin = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID_WITH_MANDATORY_SIZE,
                SIZE_PARAM_1_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1MinConsistent, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1WithoutMin, messageReporter, Set.of());

        assertThat(messageReporter.getMessages().size()).isEqualTo(1);

        FailData failData = messageReporter.getFailData();
        // ошибка по отсутствующим параметрам - нечего запоминать
        assertThat(failData).isNull();
    }

    @Test
    public void shouldRejectTicketWithMinValueGreaterThenMaxValue() {
        GcSkuTicket gcSkuTicketWithMinGreaterThenMax = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                numeric(SIZE_PARAM_1_ID, 32),
                numeric(SIZE_PARAM_1_MIN_ID, 123),
                numeric(SIZE_PARAM_1_MAX_ID, 12)
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithMinGreaterThenMax, messageReporter, Set.of());

        assertThat(messageReporter.getMessages().size()).isEqualTo(1);
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).containsExactly(
                new ParamInfo(SIZE_PARAM_1_ID, null, false)
        );
    }

    @Test
    public void shouldAcceptTicketWithMinValueLessOrEqualThenMaxValue() {
        GcSkuTicket gcSkuTicketWithMinLessThenMax = createTicketWith(
                CATEGORY_WITH_MANY_SIZE_PARAMS_ID,
                numeric(SIZE_PARAM_1_ID, 0),
                numeric(SIZE_PARAM_1_MIN_ID, 11),
                numeric(SIZE_PARAM_1_MAX_ID, 12)
        );

        GcSkuTicket gcSkuTicketWithMinEqualsMax = createTicketWith(
                CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                numeric(VENDOR_SIZE_PARAM_ID, 0),
                numeric(VENDOR_SIZE_PARAM_MIN_ID, 12),
                numeric(VENDOR_SIZE_PARAM_MAX_ID, 12)
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithMinLessThenMax, messageReporter, Set.of());
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithMinEqualsMax, messageReporter, Set.of());

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void shouldAcceptTicketWithMinOrMaxUnitedSizeParameter() {
        GcSkuTicket gcSkuTicketWithParam1MinConsistent = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE,
                KnownParameters.UNITED_SIZE.getId(),
                SIZE_PARAM_1_MIN_ID
        );
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1MinConsistent, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(0);

        GcSkuTicket gcSkuTicketWithParam1MaxConsistent = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE,
                KnownParameters.UNITED_SIZE.getId(),
                SIZE_PARAM_1_MAX_ID
        );
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithParam1MaxConsistent, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(0);
    }

    @Test
    public void shouldRejectTicketWithInconsistentUnitedSizeParameter() {
        // у оффера не заполнена ни одна из мерок
        GcSkuTicket gcSkuTicketWithoutMinMax = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE,
                KnownParameters.UNITED_SIZE.getId()
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutMinMax, messageReporter, Set.of());

        assertThat(messageReporter.getMessages().size()).isEqualTo(1);

        FailData failData = messageReporter.getFailData();
        // ошибка по отсутствующим параметрам - нечего запоминать
        assertThat(failData).isNull();
    }

    @Test
    public void shouldAcceptTicketWithMandatoryUnitedSizeAndRange() {
        GcSkuTicket gcSkuTicketWithRangeParams = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE,
                KnownParameters.UNITED_SIZE.getId(),
                SIZE_PARAM_1_MIN_ID,
                SIZE_PARAM_1_MAX_ID
        );
        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithRangeParams, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(0);
    }

    @Test
    public void shouldRejectTicketWithInconsistentUnitedSizeAndRange() {
        // у оффера не заполнена ни одна из мерок
        GcSkuTicket gcSkuTicketWithoutMinMax = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE,
                KnownParameters.UNITED_SIZE.getId()
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutMinMax, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(1);

        GcSkuTicket gcSkuTicketWithoutMin = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE,
                KnownParameters.UNITED_SIZE.getId(),
                SIZE_PARAM_1_MAX_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutMin, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(2);

        GcSkuTicket gcSkuTicketWithoutMax = createTicketWith(
                CATEGORY_WITH_MANDATORY_UNITED_SIZE_AND_RANGE,
                KnownParameters.UNITED_SIZE.getId(),
                SIZE_PARAM_1_MIN_ID
        );

        sizeParamsConsistencyValidation.validateTicket(gcSkuTicketWithoutMax, messageReporter, Set.of());
        assertThat(messageReporter.getMessages().size()).isEqualTo(3);

        FailData failData = messageReporter.getFailData();
        // ошибка по отсутствующим параметрам - нечего запоминать
        assertThat(failData).isNull();
    }

    public static List<GcSkuTicket> prepareBadTicketsForTestingSizes() {
        return Arrays.asList(
                createTicketWith(
                        1_1L, 0, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10)
                ),
                createTicketWith(
                        1_2L, 0, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 11)
                ),
                createTicketWith(
                        1_3L, 0, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10)
                ),

                createTicketWith(
                        2_1L, 1, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 10)
                ),
                createTicketWith(
                        2_2L, 1, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 10)
                ),
                createTicketWith(
                        2_3L, 1, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 15)
                ),

                createTicketWith(
                        3_1L, 2, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 12),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 14)
                ),
                createTicketWith(
                        3_2L, 2, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 15)
                ),
                createTicketWith(
                        3_3L, 2, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 15)
                )
        );
    }

    public static List<GcSkuTicket> prepareGoodTicketsForTestingSizes() {
        return Arrays.asList(
                /*
                 * Group with only min param:
                 */

                createTicketWith(
                        1_1L, 10, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "11"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10)
                ),
                createTicketWith(
                        1_2L, 10, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "12"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10)
                ),
                createTicketWith(
                        1_4L, 10, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "14"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 11)
                ),

                /*
                 * Group with both of min and max params:
                 */

                createTicketWith(
                        3_1L, 11, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "31"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 15)
                ),
                createTicketWith(
                        3_2L, 11, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "32"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 10),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 15)
                ),
                createTicketWith(
                        3_4L, 11, CATEGORY_WITH_VENDOR_SIZE_PARAM_ID,
                        string(VENDOR_SIZE_PARAM_ID, "34"),
                        numeric(VENDOR_SIZE_PARAM_MIN_ID, 6),
                        numeric(VENDOR_SIZE_PARAM_MAX_ID, 8)
                )
        );
    }

    @Test
    public void getValidationTypeShouldReturnCorrectValue() {
        assertThat(sizeParamsConsistencyValidation.getValidationType())
                .isEqualTo(SIZE_PARAMS_CONSISTENCY);
    }

    private GcSkuTicket createTicketWith(Long categoryId, Long... paramIds) {
        return createTicketWith(
                categoryId,
                0,
                Arrays.stream(paramIds)
                        .map(id -> MarketParameterValue.newBuilder().setParamId(id).build())
                        .collect(toList())
        );
    }

    private GcSkuTicket createTicketWith(Long categoryId, MarketParameterValue... parameters) {
        return createTicketWith(categoryId, 0, Arrays.asList(parameters));
    }

    private static GcSkuTicket createTicketWith(
            Long id,
            Integer groupId,
            Long categoryId,
            MarketParameterValue... parameters
    ) {
        GcSkuTicket ticket = createTicketWith(categoryId, groupId, Arrays.asList(parameters));
        ticket.setId(id);
        ticket.setShopSku(String.valueOf(id));
        ticket.setDcpGroupId(groupId);
        return ticket;
    }

    private static GcSkuTicket createTicketWith(
            Long categoryId, Integer groupId, List<MarketParameterValue> marketParameterValues
    ) {
        GcSkuTicket gcSkuTicket = new GcSkuTicket();
        gcSkuTicket.setCategoryId(categoryId);
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder().setContent(
                OfferContent.newBuilder().setPartner(
                        PartnerContent.newBuilder().setMarketSpecificContent(
                                MarketSpecificContent.newBuilder().setParameterValues(
                                        MarketParameterValues.newBuilder().addAllParameterValues(marketParameterValues)
                                )
                        )
                )
        );
        offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                .getGroupNameBuilder().setValue(String.valueOf(groupId));
        gcSkuTicket.setDatacampOffer(offerBuilder.build());
        return gcSkuTicket;
    }

    public static CategoryData buildCategoryDataWithSizes() {
        return new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(VENDOR_SIZE_PARAM_ID, Pair.of(VENDOR_SIZE_PARAM_MIN_ID, VENDOR_SIZE_PARAM_MAX_ID));
                        put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                        put(SIZE_PARAM_2_ID, Pair.of(SIZE_PARAM_2_MIN_ID, SIZE_PARAM_2_MAX_ID));
                        put(SIZE_PARAM_3_ID, Pair.of(SIZE_PARAM_3_MIN_ID, SIZE_PARAM_3_MAX_ID));
                    }
                },
                generateParameter(VENDOR_SIZE_PARAM_ID, false,
                        MboParameters.SKUParameterMode.SKU_DEFINING, MboParameters.ValueType.STRING),
                generateParameter(VENDOR_SIZE_PARAM_MIN_ID),
                generateParameter(VENDOR_SIZE_PARAM_MAX_ID),
                generateParameter(SIZE_PARAM_1_ID),
                generateParameter(SIZE_PARAM_1_MIN_ID),
                generateParameter(SIZE_PARAM_1_MAX_ID),
                generateParameter(SIZE_PARAM_2_ID),
                generateParameter(SIZE_PARAM_2_MIN_ID),
                generateParameter(SIZE_PARAM_2_MAX_ID),
                generateParameter(SIZE_PARAM_3_ID),
                generateParameter(SIZE_PARAM_3_MIN_ID),
                generateParameter(SIZE_PARAM_3_MAX_ID)
        );
    }

    public static MboParameters.Parameter generateParameter(long parameterId) {
        return generateParameter(parameterId, false);
    }

    public static MboParameters.Parameter generateParameter(long parameterId, boolean mandatoryForPartner) {
        return generateParameter(parameterId, mandatoryForPartner, null, null);
    }

    public static MboParameters.Parameter generateParameter(long parameterId, boolean mandatoryForPartner,
                                                            MboParameters.SKUParameterMode skuParameterMode,
                                                            MboParameters.ValueType valueType
    ) {
        MboParameters.Parameter.Builder builder = MboParameters.Parameter
                .newBuilder()
                .setId(parameterId);

        if (mandatoryForPartner) {
            builder.setMandatoryForPartner(mandatoryForPartner);
        }

        if (Objects.nonNull(skuParameterMode)) {
            builder.setSkuMode(skuParameterMode);
        }

        if (Objects.nonNull(valueType)) {
            builder.setValueType(valueType);
        }

        return builder.build();
    }

    public static MboParameters.Parameter generateUnitedSizeParameter(boolean mandatoryForPartner) {
        return MboParameters.Parameter.newBuilder()
                .setId(KnownParameters.UNITED_SIZE.getId())
                .setXslName(KnownParameters.UNITED_SIZE.getXslName())
                .setMandatoryForPartner(mandatoryForPartner)
                .build();
    }

    public static CategoryData buildCategoryDataWithOneMandatorySize() {
        return new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(VENDOR_SIZE_PARAM_ID, Pair.of(VENDOR_SIZE_PARAM_MIN_ID, VENDOR_SIZE_PARAM_MAX_ID));
                        put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                        put(SIZE_PARAM_2_ID, Pair.of(SIZE_PARAM_2_MIN_ID, SIZE_PARAM_2_MAX_ID));
                        put(SIZE_PARAM_3_ID, Pair.of(SIZE_PARAM_3_MIN_ID, SIZE_PARAM_3_MAX_ID));
                    }
                },
                generateParameter(VENDOR_SIZE_PARAM_ID, false,
                        MboParameters.SKUParameterMode.SKU_DEFINING, MboParameters.ValueType.STRING),
                generateParameter(VENDOR_SIZE_PARAM_MIN_ID),
                generateParameter(VENDOR_SIZE_PARAM_MAX_ID),
                generateParameter(SIZE_PARAM_1_ID, true),
                generateParameter(SIZE_PARAM_1_MIN_ID),
                generateParameter(SIZE_PARAM_1_MAX_ID),
                generateParameter(SIZE_PARAM_2_ID),
                generateParameter(SIZE_PARAM_2_MIN_ID),
                generateParameter(SIZE_PARAM_2_MAX_ID),
                generateParameter(SIZE_PARAM_3_ID),
                generateParameter(SIZE_PARAM_3_MIN_ID),
                generateParameter(SIZE_PARAM_3_MAX_ID)
        );
    }

    public static CategoryData buildCategoryDataWithMandatoryUnitedSize() {
        return new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                        put(SIZE_PARAM_2_ID, Pair.of(SIZE_PARAM_2_MIN_ID, SIZE_PARAM_2_MAX_ID));
                    }
                },
                generateUnitedSizeParameter(true),
                generateParameter(SIZE_PARAM_1_ID),
                generateParameter(SIZE_PARAM_1_MIN_ID),
                generateParameter(SIZE_PARAM_1_MAX_ID),
                generateParameter(SIZE_PARAM_2_ID),
                generateParameter(SIZE_PARAM_2_MIN_ID),
                generateParameter(SIZE_PARAM_2_MAX_ID)
        );
    }

    public static CategoryData buildCategoryDataWithMandatoryUnitedSizeAndRange() {
        return new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                        put(SIZE_PARAM_2_ID, Pair.of(SIZE_PARAM_2_MIN_ID, SIZE_PARAM_2_MAX_ID));
                    }
                },
                generateUnitedSizeParameter(true),
                generateParameter(SIZE_PARAM_1_ID),
                generateParameter(SIZE_PARAM_1_MIN_ID, true),
                generateParameter(SIZE_PARAM_1_MAX_ID, true),
                generateParameter(SIZE_PARAM_2_ID),
                generateParameter(SIZE_PARAM_2_MIN_ID),
                generateParameter(SIZE_PARAM_2_MAX_ID)
        );
    }

    static class ParameterBuilder {

        private Long id;
        private String stringValue;
        private Number numericValue;
        private MarketValueType valueType = NUMERIC;

        private ParameterBuilder() {
        }

        public static MarketParameterValue numeric(Long id, Number numericParameter) {
            return (new ParameterBuilder()).id(id).numericValue(numericParameter).build();
        }

        public static MarketParameterValue string(Long id, String stringValue) {
            return (new ParameterBuilder()).id(id).stringValue(stringValue).build();
        }

        public static ParameterBuilder builder() {
            return new ParameterBuilder();
        }

        public ParameterBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ParameterBuilder stringValue(String stringValue) {
            this.stringValue = stringValue;
            this.valueType = STRING;
            return this;
        }

        public ParameterBuilder numericValue(Number longValue) {
            this.numericValue = longValue;
            this.valueType = NUMERIC;
            return this;
        }

        public MarketParameterValue build() {
            MarketValue.Builder marketValue = MarketValue.newBuilder();

            switch (valueType) {
                case STRING:
                    marketValue.setStrValue(stringValue);
                    break;
                case NUMERIC:
                    marketValue.setNumericValue(numericValue.toString());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value of valueType: " + valueType);
            }

            return MarketParameterValue
                    .newBuilder().setParamId(id)
                    .setValue(marketValue.setValueType(valueType).build())
                    .build();
        }

    }

}
