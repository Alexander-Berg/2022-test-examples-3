package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.matcher.AssertionMatcher;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.grouping.group.SegmentGroup;
import ru.yandex.market.crm.campaign.domain.promo.entities.PeriodicPromoEntity;
import ru.yandex.market.crm.campaign.domain.promo.entities.PromoEntity;
import ru.yandex.market.crm.campaign.domain.sending.AbstractPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.GncPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.dto.actions.PeriodicActionDto;
import ru.yandex.market.crm.campaign.dto.actions.PlainActionDto;
import ru.yandex.market.crm.campaign.dto.campaign.AbstractPromoEntityDto;
import ru.yandex.market.crm.campaign.dto.campaign.PeriodicPromoEntityDto;
import ru.yandex.market.crm.campaign.dto.segment.SegmentDto;
import ru.yandex.market.crm.campaign.dto.sending.AbstractPeriodicSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.SendingInfoDto;
import ru.yandex.market.crm.campaign.services.grouping.group.SegmentGroupDAO;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.segments.SegmentBuildsDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.users.UserInfo;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.CampaignTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.GncSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.PagedResult;
import ru.yandex.market.crm.core.domain.ReactTableRequest;
import ru.yandex.market.crm.core.domain.ReactTableRequest.Filter;
import ru.yandex.market.crm.core.domain.ReactTableRequest.Sort;
import ru.yandex.market.crm.core.domain.segment.BuildStatus;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.Counts;
import ru.yandex.market.crm.core.domain.segment.Counts.IdTypeEntry;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentAlgorithmPart;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild.Initiator;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;
import ru.yandex.market.crm.core.domain.segment.export.IdType;
import ru.yandex.market.crm.core.services.segments.SegmentsDAO;
import ru.yandex.market.crm.core.test.utils.BlackboxHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.matchers.LiluMatchers.pageInfo;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.ordersFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;

/**
 * @author apershukov
 */
public class SegmentsControllerTest extends AbstractControllerMediumTest {

    private static final String TARGET_ID_TYPE = "target_id_type";
    private static final String LINKING_MODE = "linking_mode";
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private SegmentsDAO segmentsDAO;
    @Inject
    private SegmentService segmentService;
    @Inject
    private SegmentBuildsDAO buildDAO;
    @Inject
    private SegmentGroupDAO segmentGroupDAO;
    @Inject
    private UsersRolesDao usersRolesDao;
    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;
    @Inject
    private PushSendingTestHelper pushSendingTestHelper;
    @Inject
    private GncSendingTestHelper gncSendingTestHelper;
    @Inject
    private ActionTestHelper actionTestHelper;
    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;
    @Inject
    private PushPeriodicSendingTestHelper pushPeriodicSendingTestHelper;
    @Inject
    private PeriodicActionsTestHelper periodicActionsTestHelper;
    @Inject
    private CampaignTestHelper campaignTestHelper;
    @Inject
    private BlackboxHelper blackboxHelper;

    private static SegmentBuild build() {
        return new SegmentBuild()
                .setMode(LinkingMode.ALL)
                .setInitiator(Initiator.USER);
    }

    private static SegmentBuild completedBuilding(long daysAgo, Map<IdType, Long> counts) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(daysAgo);

        return build()
                .setStatus(BuildStatus.COUNTED)
                .setStartTime(startTime)
                .setFinishTime(startTime.plusMinutes(30))
                .setMode(LinkingMode.ALL)
                .setIdTypes(counts.keySet().stream().map(IdType::getSourceType).collect(Collectors.toSet()))
                .setCounts(counts);
    }

    private static SegmentBuild inProgressBuilding() {
        return build()
                .setStatus(BuildStatus.IN_PROGRESS)
                .setStartTime(LocalDateTime.now().minusMinutes(5))
                .setTaskId(123L);
    }

    private static void assertBuild(BuildStatus expectedState,
                                    Map<IdType, Long> expectedCounts,
                                    SegmentBuild fact) {
        Assertions.assertEquals(expectedState, fact.getStatus());
        Assertions.assertEquals(expectedCounts, fact.getCounts());
    }

    private static void assertIdCounts(Map<IdType, Long> expected, Map<IdType, IdTypeEntry> idTypes) {
        Map<IdType, Long> idCounts = idTypes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getCount()
                ));

        Assertions.assertEquals(expected, idCounts);
    }

    private static AssertionMatcher<AbstractPromoEntityDto> correspondTo(PromoEntity promo) {
        return new AssertionMatcher<>() {
            @Override
            public void assertion(AbstractPromoEntityDto promoDto) throws AssertionError {
                Assertions.assertEquals(promo.getId(), promoDto.getId());
                Assertions.assertEquals(promo.getName(), promoDto.getName());
                if (promo instanceof PeriodicPromoEntity) {
                    Assertions.assertEquals(((PeriodicPromoEntity) promo).getVersion(),
                            ((PeriodicPromoEntityDto) promoDto).getVersion());
                }
            }
        };
    }

    private static AssertionMatcher<SendingInfoDto> correspondTo(AbstractPlainSending sending) {
        return new AssertionMatcher<>() {
            @Override
            public void assertion(SendingInfoDto sendingDto) throws AssertionError {
                Assertions.assertEquals(sending.getId(), sendingDto.getId());
                Assertions.assertEquals(sending.getName(), sendingDto.getName());
            }
        };
    }

    @BeforeEach
    public void setUp() {
        IntStream.range(0, 25)
                .mapToObj(i -> {
                    Segment segment = new Segment("segment_" + i);
                    segment.setName("Segment - " + i + " name");
                    segment.setConfig(new SegmentGroupPart());
                    return segment;
                })
                .forEach(segmentsDAO::createOrUpdateSegment);
    }

    @Test
    public void testListSegmentsWithoutFilters() throws Exception {
        ReactTableRequest request = new ReactTableRequest();
        PagedResult<SegmentDto> result = querySegments(request);

        Assertions.assertEquals(25L, (long) result.getPageInfo().getElementCount());
        MatcherAssert.assertThat(result.getElements(), hasSize(25));
    }

    @Test
    public void testGetSegmentsPage() throws Exception {
        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(1)
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(3, 1, 10));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(10));
        Assertions.assertEquals("segment_10", segments.get(0).getId());
        Assertions.assertEquals("segment_19", segments.get(9).getId());
    }

    @Test
    public void testPageAfterLast() throws Exception {
        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(3)
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(3, 3, 10));
        MatcherAssert.assertThat(result.getElements(), empty());
    }

    @Test
    public void testFilterById() throws Exception {
        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(5)
                        .setPageNumber(0)
                        .setFilters(Collections.singletonList(
                                new Filter("id", "4")
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(1, 0, 5));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(3));
        Assertions.assertEquals("segment_4", segments.get(0).getId());
        Assertions.assertEquals("segment_14", segments.get(1).getId());
        Assertions.assertEquals("segment_24", segments.get(2).getId());
    }

    @Test
    public void testCaseInsensitiveFiltration() throws Exception {
        Segment segment = new Segment("id");
        segment.setName("АБВ");

        segmentsDAO.createOrUpdateSegment(segment);

        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(5)
                        .setPageNumber(0)
                        .setFilters(Collections.singletonList(
                                new Filter("name", "абв")
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(1, 0, 5));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals(segment.getId(), segments.get(0).getId());
    }

    @Test
    public void testSkipFiltersWithEmptyValue() throws Exception {
        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(0)
                        .setFilters(Collections.singletonList(
                                new Filter("id", null)
                        ))
        );

        Assertions.assertNotNull(result.getPageInfo());
        Assertions.assertEquals(25, (long) result.getPageInfo().getElementCount());
    }

    @Test
    public void testValidateFilters() throws Exception {
        mockMvc.perform(post("/api/segments/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        jsonSerializer.writeObjectAsString(
                                new ReactTableRequest()
                                        .setFilters(Collections.singletonList(
                                                new Filter("invalid-filter", "value")
                                        ))
                        )
                )
        ).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testFilterByMultipleFilters() throws Exception {
        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(5)
                        .setPageNumber(0)
                        .setFilters(Arrays.asList(
                                new Filter("id", "4"),
                                new Filter("name", "Segment - 14 name")
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(1, 0, 5));
        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals("segment_14", segments.get(0).getId());
    }

    @Test
    public void testFilterByDescription() throws Exception {
        Segment segment = new Segment("id");
        segment.setName("name");
        segment.setDescription("iddqd");

        segmentsDAO.createOrUpdateSegment(segment);

        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(5)
                        .setPageNumber(0)
                        .setFilters(Collections.singletonList(
                                new Filter("description", "iddqd")
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(1, 0, 5));
        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals("id", segments.get(0).getId());
        Assertions.assertEquals("iddqd", segments.get(0).getDescription());
    }

    @Test
    public void testSortById() throws Exception {
        Segment segment = new Segment("aaa");
        segment.setName("name");

        segmentsDAO.createOrUpdateSegment(segment);

        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(0)
                        .setSorts(Collections.singletonList(
                                new Sort("id", false)
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(3, 0, 10));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(10));
        Assertions.assertEquals("aaa", segments.get(0).getId());
    }

    @Test
    public void testSortByAllLinkElementsCount() throws Exception {
        Segment segment = new Segment("first_segment");
        segment.setName("first_segment_name");

        Segment segment2 = new Segment("second_segment");
        segment2.setName("second_segment_name");

        Segment segment3 = new Segment("third_segment");
        segment3.setName("third_segment_name");

        segmentsDAO.createOrUpdateSegment(segment);
        segmentsDAO.createOrUpdateSegment(segment2);
        segmentsDAO.createOrUpdateSegment(segment3);

        prepareBuilds(segment, completedBuilding(1, Map.of(IdType.PUID, 10_000L)));
        prepareBuilds(segment2, completedBuilding(1, Map.of(IdType.EMAIL, 1000L)));
        prepareBuilds(segment3, completedBuilding(1, Map.of(IdType.YUID, 100L)));

        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(0)
                        .setSorts(Collections.singletonList(
                                new Sort("counts.ALL", true)
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(3, 0, 10));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(10));
        Assertions.assertEquals(segment.getId(), segments.get(0).getId());
        Assertions.assertEquals(segment2.getId(), segments.get(1).getId());
        Assertions.assertEquals(segment3.getId(), segments.get(2).getId());
    }

    @Test
    public void testSortByMultipleFields() throws Exception {
        Segment segment1 = new Segment("aaa");
        segment1.setName("AAA");
        segment1.setDescription("aaa");

        segmentsDAO.createOrUpdateSegment(segment1);

        Segment segment2 = new Segment("bbb");
        segment2.setName("AAA");
        segment2.setDescription("bbb");

        segmentsDAO.createOrUpdateSegment(segment2);

        PagedResult<SegmentDto> result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(0)
                        .setSorts(Arrays.asList(
                                new Sort("name", false),
                                new Sort("description", true)
                        ))
        );

        MatcherAssert.assertThat(result.getPageInfo(), pageInfo(3, 0, 10));

        List<SegmentDto> segments = result.getElements();
        MatcherAssert.assertThat(segments, hasSize(10));

        Assertions.assertEquals("AAA", segments.get(0).getName());
        Assertions.assertEquals("bbb", segments.get(0).getDescription());

        Assertions.assertEquals("AAA", segments.get(1).getName());
        Assertions.assertEquals("aaa", segments.get(1).getDescription());
    }

    @Test
    public void testValidateSortFields() throws Exception {
        mockMvc.perform(post("/api/segments/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        jsonSerializer.writeObjectAsString(
                                new ReactTableRequest()
                                        .setSorts(Collections.singletonList(
                                                new Sort("invalid-field", false)
                                        ))
                        )
                )
        ).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testGetAll() throws Exception {
        Segment segment1 = segment("Совершали заказы",
                ordersFilter()
        );
        segmentsDAO.createOrUpdateSegment(segment1);

        Segment segment2 = segment("Подписаны на рекламу",
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );
        segmentsDAO.createOrUpdateSegment(segment2);

        List<SegmentDto> segments = requestSegmentsWithSubs("сов");

        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals(segment1.getId(), segments.get(0).getId());
    }

    @Test
    public void testGetSegmentAuthor() throws Exception {
        var testLogin = "test_login";
        var profile = SecurityUtils.profile(testLogin);
        SecurityUtils.setAuthentication(profile);
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        blackboxHelper.setUpResolveYandexTeamInfoByUid(profile.getUid(), testLogin);

        Segment segment1 = segment("segment_name_test");
        segment1.setAuthorUid(profile.getUid());
        segmentsDAO.createOrUpdateSegment(segment1);


        var result = querySegments(
                new ReactTableRequest()
                        .setPageSize(10)
                        .setPageNumber(0)
                        .setFilters(Collections.singletonList(
                                new Filter("name", "segment_name_test")
                        ))
        );

        var segments = result.getElements();

        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals(profile.getUid(), segments.get(0).getAuthorInfo().getUid());
        Assertions.assertEquals(testLogin, segments.get(0).getAuthorInfo().getLogin());
    }

    @Test
    public void testGetWithSubscribed() throws Exception {
        Segment segment = segment("AAA",
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );

        segmentsDAO.createOrUpdateSegment(segment);
        List<SegmentDto> segments = requestSegmentsWithSubs("");

        Set<String> ids = IntStream.range(0, 25)
                .mapToObj(i -> "segment_" + i)
                .collect(Collectors.toSet());
        ids.add(segment.getId());

        MatcherAssert.assertThat(segments, hasSize(26));
        for (SegmentDto dto : segments) {
            Assertions.assertTrue(ids.remove(dto.getId()));
        }
    }

    @Test
    public void testGetWithSubsUsingNamePath() throws Exception {
        Segment segment1 = segment("Одни люди",
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );
        segmentsDAO.createOrUpdateSegment(segment1);

        Segment segment2 = segment("Другие люди",
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );
        segmentsDAO.createOrUpdateSegment(segment2);

        List<SegmentDto> segments = requestSegmentsWithSubs("дру");

        MatcherAssert.assertThat(segments, hasSize(1));
        Assertions.assertEquals(segment2.getId(), segments.get(0).getId());
    }

    /**
     * Сегмент, содержащий фильтр, работающий по puid'ам, подходит для вычисления
     * email-адресов в режиме без склейки
     */
    @Test
    public void testSegmentWithPuidFilterIsSuitableForEmailListBuilding() throws Exception {
        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING),
                plusFilter()
        );
        segment = segmentService.addSegment(segment);

        List<SegmentDto> segments = requestSegments(builder -> builder
                .param(TARGET_ID_TYPE, UidType.EMAIL.name())
                .param(LINKING_MODE, LinkingMode.NONE.name())
        );

        Assertions.assertEquals(1, segments.size());
        Assertions.assertEquals(segment.getId(), segments.get(0).getId());
    }

    /**
     * Сегмент, содержащий фильтр, работающий по puid'ам, подходит для вычисления
     * списков мобильных устройств
     */
    @Test
    public void testSegmentWithPuidFilderIsSuitableForUuidListBuilding() throws Exception {
        Segment segment = segment(
                mobilesFilter(),
                plusFilter()
        );
        segment = segmentService.addSegment(segment);

        List<SegmentDto> segments = requestSegments(builder -> builder
                .param(TARGET_ID_TYPE, UidType.UUID.name())
                .param(LINKING_MODE, LinkingMode.NONE.name())
        );

        Assertions.assertEquals(1, segments.size());
        Assertions.assertEquals(segment.getId(), segments.get(0).getId());
    }

    /**
     * При наличии нескольких билдов сегмента в глобальные счетчики устанавливаются значения
     * из последних билдов
     */
    @Test
    public void testSegmentCountersIfCountedFromBuilds() throws Exception {
        Segment segment = segmentService.addSegment(
                segment(plusFilter())
        );

        prepareBuilds(segment,
                completedBuilding(1, Map.of(IdType.PUID, 10_000L)),
                completedBuilding(2, Map.of(IdType.PUID, 20_000L)),
                completedBuilding(3, Map.of(
                        IdType.PUID, 30_000L,
                        IdType.YUID, 100_000L
                ))
        );

        SegmentDto returnedSegment = requestSegment(segment.getId());
        Assertions.assertEquals(segment.getId(), returnedSegment.getId());

        List<SegmentBuild> builds = returnedSegment.getBuilds();
        MatcherAssert.assertThat(builds, hasSize(3));

        assertBuild(
                BuildStatus.COUNTED,
                Map.of(
                        IdType.PUID, 30_000L,
                        IdType.YUID, 100_000L
                ),
                builds.get(0)
        );

        assertBuild(BuildStatus.COUNTED, Map.of(IdType.PUID, 20_000L), builds.get(1));
        assertBuild(BuildStatus.COUNTED, Map.of(IdType.PUID, 10_000L), builds.get(2));

        Map<LinkingMode, Counts> counts = returnedSegment.getCounts();
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(1, counts.size());

        Counts allLinksCounts = counts.get(LinkingMode.ALL);
        Assertions.assertNotNull(allLinksCounts);

        Assertions.assertEquals(BuildStatus.COUNTED, allLinksCounts.getStatus());

        assertIdCounts(
                Map.of(
                        IdType.PUID, 10_000L,
                        IdType.YUID, 100_000L
                ),
                allLinksCounts.getTypes()
        );
    }

    @Test
    public void testGetListSegmentsAuthors() throws Exception {
        var profile1 = SecurityUtils.profile("test_1");
        SecurityUtils.setAuthentication(profile1);
        usersRolesDao.addRole(profile1.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.ADMIN));
        blackboxHelper.setUpResolveYandexTeamInfoByUid(profile1.getUid(), profile1.getLogin());
        segmentsDAO.createOrUpdateSegment(segment("test_author_1"));


        var profile2 = SecurityUtils.profile("test_2");
        SecurityUtils.setAuthentication(profile2);
        usersRolesDao.addRole(profile2.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));
        blackboxHelper.setUpResolveYandexTeamInfoByUid(profile2.getUid(), profile2.getLogin());
        segmentsDAO.createOrUpdateSegment(segment("test_author_2"));

        var profile3 = SecurityUtils.profile("test_3");
        SecurityUtils.setAuthentication(profile3);
        usersRolesDao.addRole(profile3.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));
        segmentsDAO.createOrUpdateSegment(segment("test_author_3"));

        var result = jsonDeserializer.readObject(
                new TypeReference<List<UserInfo>>() {
                },
                mockMvc.perform(get("/api/segments/authors"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        )
                .stream()
                .collect(LiluCollectors.index(UserInfo::getUid));

        Assertions.assertEquals(profile1.getLogin(), result.get(profile1.getUid()).getLogin());
        Assertions.assertEquals(profile2.getLogin(), result.get(profile2.getUid()).getLogin());

        Assertions.assertNull(result.get(profile3.getUid()).getLogin());
    }

    /**
     * В случае если в одном из режимов склейки идет построение сегмента в глобальных
     * счетчиках этот режим помечается как IN PROGRESS
     */
    @Test
    public void testCountsWithBuildingInProgress() throws Exception {
        Segment segment = segmentService.addSegment(
                segment(plusFilter())
        );

        prepareBuilds(segment,
                inProgressBuilding()
                        .setIdTypes(Set.of(UidType.PUID))
                        .setMode(LinkingMode.ALL),
                completedBuilding(1, Map.of(IdType.PUID, 20_000L))
                        .setMode(LinkingMode.ALL),
                completedBuilding(2, Map.of(IdType.PUID, 10_000L))
                        .setMode(LinkingMode.NONE)
        );

        SegmentDto returnedSegment = requestSegment(segment.getId());

        List<SegmentBuild> builds = returnedSegment.getBuilds();
        MatcherAssert.assertThat(builds, hasSize(3));

        assertBuild(BuildStatus.COUNTED, Map.of(IdType.PUID, 10_000L), builds.get(0));
        assertBuild(BuildStatus.COUNTED, Map.of(IdType.PUID, 20_000L), builds.get(1));
        assertBuild(BuildStatus.IN_PROGRESS, null, builds.get(2));

        Map<LinkingMode, Counts> counts = returnedSegment.getCounts();
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(2, counts.size());

        Counts noLinksCount = counts.get(LinkingMode.NONE);
        Assertions.assertNotNull(noLinksCount);
        assertIdCounts(Map.of(IdType.PUID, 10_000L), noLinksCount.getTypes());

        Counts allLinkCounts = counts.get(LinkingMode.ALL);
        Assertions.assertNotNull(allLinkCounts);
        Assertions.assertEquals(BuildStatus.IN_PROGRESS, allLinkCounts.getStatus());
        Assertions.assertNotNull(allLinkCounts.getBuildId());
    }

    /**
     * Если сегмент пересчитывается по инициативе системы (например, при сборке рассылки)
     * он не отображается на карточке семента и никак не влияет на глобальные счетчики пока
     * пересчет не будет завершен
     */
    @Test
    public void testIgnoreSystemInProgressCounts() throws Exception {
        Segment segment = segmentService.addSegment(
                segment(plusFilter())
        );

        prepareBuilds(segment,
                inProgressBuilding()
                        .setInitiator(Initiator.SYSTEM),
                completedBuilding(1, Map.of(IdType.PUID, 10_000L))
        );

        SegmentDto returnedSegment = requestSegment(segment.getId());

        List<SegmentBuild> buildingFacts = returnedSegment.getBuilds();
        MatcherAssert.assertThat(buildingFacts, hasSize(1));

        assertBuild(BuildStatus.COUNTED, Map.of(IdType.PUID, 10_000L), buildingFacts.get(0));

        Map<LinkingMode, Counts> counts = returnedSegment.getCounts();
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(1, counts.size());

        Counts allLinkCounts = counts.get(LinkingMode.ALL);
        Assertions.assertNotNull(allLinkCounts);
        assertIdCounts(Map.of(IdType.PUID, 10_000L), allLinkCounts.getTypes());
    }

    /**
     * Если пересчет сегмента, инициирован системой (например при сборке рассылки),
     * билд отображается в списке с билдами и результаты пересчета влияют на глобальные счетчики
     */
    @Test
    public void testFinishedSystemBuildingIsNotIgnored() throws Exception {
        Segment segment = segmentService.addSegment(
                segment(plusFilter())
        );

        prepareBuilds(segment,
                completedBuilding(1, Map.of(IdType.PUID, 10_000L))
                        .setInitiator(Initiator.SYSTEM)
        );

        SegmentDto returnedSegment = requestSegment(segment.getId());

        List<SegmentBuild> buildingFacts = returnedSegment.getBuilds();
        MatcherAssert.assertThat(buildingFacts, hasSize(1));

        Map<LinkingMode, Counts> counts = returnedSegment.getCounts();
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(1, counts.size());

        Counts allLinkCounts = counts.get(LinkingMode.ALL);
        Assertions.assertNotNull(allLinkCounts);
        assertIdCounts(Map.of(IdType.PUID, 10_000L), allLinkCounts.getTypes());
    }

    /**
     * Если пересчет сегмента завершился с ошибкой эти ошибки попадают в глобальный счетчик
     */
    @Test
    public void testBuildingFinishedWithErrorAffectsGlobalCounts() throws Exception {
        Segment segment = segmentService.addSegment(
                segment(plusFilter())
        );

        prepareBuilds(segment,
                completedBuilding(2, Map.of(IdType.PUID, 10_000L)),
                build()
                        .setStartTime(LocalDateTime.now().minusDays(1))
                        .setFinishTime(LocalDateTime.now().minusDays(1))
                        .setStatus(BuildStatus.ERROR)
                        .setIdTypes(Set.of(UidType.EMAIL, UidType.UUID))
                        .setMessage("Error")
        );

        SegmentDto returnedSegment = requestSegment(segment.getId());

        List<SegmentBuild> buildingFacts = returnedSegment.getBuilds();
        MatcherAssert.assertThat(buildingFacts, hasSize(2));

        Map<LinkingMode, Counts> counts = returnedSegment.getCounts();
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(1, counts.size());

        Counts allLinkCounts = counts.get(LinkingMode.ALL);
        Assertions.assertNotNull(allLinkCounts);

        IdTypeEntry puidsEntry = allLinkCounts.getTypes().get(IdType.PUID);
        Assertions.assertEquals(10_000, (long) puidsEntry.getCount());
        Assertions.assertEquals(BuildStatus.COUNTED, puidsEntry.getStatus());
        Assertions.assertNotNull(puidsEntry.getBuildId());

        IdTypeEntry emailEntry = allLinkCounts.getTypes().get(IdType.EMAIL);
        Assertions.assertNotNull(emailEntry);
        Assertions.assertEquals(BuildStatus.ERROR, emailEntry.getStatus());
        Assertions.assertEquals("Error", emailEntry.getMessage());

        IdTypeEntry deviceEntry = allLinkCounts.getTypes().get(IdType.DEVICE_ID);
        Assertions.assertNotNull(deviceEntry);
        Assertions.assertEquals(BuildStatus.ERROR, deviceEntry.getStatus());
        Assertions.assertEquals("Error", deviceEntry.getMessage());
    }

    @Test
    public void testReturnLinkedSendings() throws Exception {
        Segment linkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment unlinkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment aloneSegment = segmentService.addSegment(segment(plusFilter()));

        EmailPlainSending linkedEmail1 = emailSendingTestHelper.prepareSending(
                emailSendingTestHelper.prepareConfig(linkedSegment, LinkingMode.NONE));
        EmailPlainSending linkedEmail2 = emailSendingTestHelper.prepareSending(
                emailSendingTestHelper.prepareConfig(linkedSegment, LinkingMode.NONE));
        PushPlainSending linkedPush1 =
                pushSendingTestHelper.prepareSending(PushSendingTestHelper.config(linkedSegment));
        PushPlainSending linkedPush2 =
                pushSendingTestHelper.prepareSending(PushSendingTestHelper.config(linkedSegment));
        GncPlainSending linkedGnc1 = gncSendingTestHelper.prepareSending(linkedSegment);
        GncPlainSending linkedGnc2 = gncSendingTestHelper.prepareSending(linkedSegment);

        EmailPlainSending unlinkedEmail = emailSendingTestHelper.prepareSending(
                emailSendingTestHelper.prepareConfig(unlinkedSegment, LinkingMode.NONE));
        PushPlainSending unlinkedPush =
                pushSendingTestHelper.prepareSending(PushSendingTestHelper.config(unlinkedSegment));
        GncPlainSending unlinkedGnc = gncSendingTestHelper.prepareSending(unlinkedSegment);

        SegmentDto returnedLinkedSegment = requestSegment(linkedSegment.getId());
        SegmentDto returnedUnlinkedSegment = requestSegment(unlinkedSegment.getId());
        SegmentDto returnedAloneSegment = requestSegment(aloneSegment.getId());

        List<SendingInfoDto> linkedDependentSendings = returnedLinkedSegment.getDependentSendings();
        MatcherAssert.assertThat(linkedDependentSendings, hasSize(6));
        MatcherAssert.assertThat(linkedDependentSendings, containsInAnyOrder(correspondTo(linkedEmail1),
                correspondTo(linkedEmail2),
                correspondTo(linkedPush1),
                correspondTo(linkedPush2),
                correspondTo(linkedGnc1),
                correspondTo(linkedGnc2))
        );

        List<SendingInfoDto> unlinkedDependentSendings = returnedUnlinkedSegment.getDependentSendings();
        MatcherAssert.assertThat(unlinkedDependentSendings, hasSize(3));
        MatcherAssert.assertThat(unlinkedDependentSendings, containsInAnyOrder(correspondTo(unlinkedEmail),
                correspondTo(unlinkedPush),
                correspondTo(unlinkedGnc))
        );

        MatcherAssert.assertThat(returnedAloneSegment.getDependentSendings(), hasSize(0));
    }

    @Test
    public void testReturnLinkedActions() throws Exception {
        Segment linkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment unlinkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment aloneSegment = segmentService.addSegment(segment(plusFilter()));

        PlainAction linkedAction1 = actionTestHelper.prepareAction(linkedSegment.getId(), LinkingMode.NONE);
        PlainAction linkedAction2 = actionTestHelper.prepareAction(linkedSegment.getId(), LinkingMode.NONE);
        PlainAction unlinkedAction = actionTestHelper.prepareAction(unlinkedSegment.getId(), LinkingMode.NONE);

        SegmentDto returnedLinkedSegment = requestSegment(linkedSegment.getId());
        SegmentDto returnedUnlinkedSegment = requestSegment(unlinkedSegment.getId());
        SegmentDto returnedAloneSegment = requestSegment(aloneSegment.getId());

        List<PlainActionDto> linkedDependentActions = returnedLinkedSegment.getDependentActions();
        MatcherAssert.assertThat(linkedDependentActions, hasSize(2));
        MatcherAssert.assertThat(linkedDependentActions, containsInAnyOrder(correspondTo(linkedAction1),
                correspondTo(linkedAction2))
        );

        List<PlainActionDto> unlinkedDependentActions = returnedUnlinkedSegment.getDependentActions();
        MatcherAssert.assertThat(unlinkedDependentActions, hasSize(1));
        MatcherAssert.assertThat(unlinkedDependentActions.get(0), correspondTo(unlinkedAction));

        MatcherAssert.assertThat(returnedAloneSegment.getDependentActions(), hasSize(0));
    }

    @Test
    public void testReturnLinkedPeriodicSendings() throws Exception {
        Campaign campaign = campaignTestHelper.prepareCampaign();

        Segment linkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment unlinkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment aloneSegment = segmentService.addSegment(segment(plusFilter()));

        EmailPeriodicSending linkedEmail1 =
                emailPeriodicSendingTestHelper.prepareSending(campaign, linkedSegment, sending -> {
                });
        EmailPeriodicSending linkedEmail2 =
                emailPeriodicSendingTestHelper.prepareSending(campaign, linkedSegment, sending -> {
                });
        PushPeriodicSending linkedPush1 = pushPeriodicSendingTestHelper.prepareSending(linkedSegment);
        PushPeriodicSending linkedPush2 = pushPeriodicSendingTestHelper.prepareSending(linkedSegment);

        EmailPeriodicSending unlinkedEmail =
                emailPeriodicSendingTestHelper.prepareSending(campaign, unlinkedSegment, sending -> {
                });
        PushPeriodicSending unlinkedPush = pushPeriodicSendingTestHelper.prepareSending(unlinkedSegment);

        SegmentDto returnedLinkedSegment = requestSegment(linkedSegment.getId());
        SegmentDto returnedUnlinkedSegment = requestSegment(unlinkedSegment.getId());
        SegmentDto returnedAloneSegment = requestSegment(aloneSegment.getId());

        List<AbstractPeriodicSendingDto> linkedDependentSendings = returnedLinkedSegment.getDependentPeriodicSendings();
        MatcherAssert.assertThat(linkedDependentSendings, hasSize(4));
        MatcherAssert.assertThat(linkedDependentSendings, containsInAnyOrder(correspondTo(linkedEmail1),
                correspondTo(linkedEmail2),
                correspondTo(linkedPush1),
                correspondTo(linkedPush2))
        );

        List<AbstractPeriodicSendingDto> unlinkedDependentActions =
                returnedUnlinkedSegment.getDependentPeriodicSendings();
        MatcherAssert.assertThat(unlinkedDependentActions, hasSize(2));
        MatcherAssert.assertThat(unlinkedDependentActions, containsInAnyOrder(correspondTo(unlinkedEmail),
                correspondTo(unlinkedPush))
        );

        MatcherAssert.assertThat(returnedAloneSegment.getDependentPeriodicSendings(), hasSize(0));
    }

    @Test
    public void testReturnLinkedPeriodicActions() throws Exception {
        Campaign campaign = campaignTestHelper.prepareCampaign();

        Segment linkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment unlinkedSegment = segmentService.addSegment(segment(plusFilter()));
        Segment aloneSegment = segmentService.addSegment(segment(plusFilter()));

        PeriodicAction linkedAction1 = periodicActionsTestHelper.prepareAction(campaign, linkedSegment);
        PeriodicAction linkedAction2 = periodicActionsTestHelper.prepareAction(campaign, linkedSegment);
        PeriodicAction unlinkedAction = periodicActionsTestHelper.prepareAction(campaign, unlinkedSegment);

        SegmentDto returnedLinkedSegment = requestSegment(linkedSegment.getId());
        SegmentDto returnedUnlinkedSegment = requestSegment(unlinkedSegment.getId());
        SegmentDto returnedAloneSegment = requestSegment(aloneSegment.getId());

        List<PeriodicActionDto> linkedDependentActions = returnedLinkedSegment.getDependentPeriodicActions();
        MatcherAssert.assertThat(linkedDependentActions, hasSize(2));
        MatcherAssert.assertThat(linkedDependentActions, containsInAnyOrder(correspondTo(linkedAction1),
                correspondTo(linkedAction2))
        );

        List<PeriodicActionDto> unlinkedDependentActions = returnedUnlinkedSegment.getDependentPeriodicActions();
        MatcherAssert.assertThat(unlinkedDependentActions, hasSize(1));
        MatcherAssert.assertThat(unlinkedDependentActions.get(0), correspondTo(unlinkedAction));

        MatcherAssert.assertThat(returnedAloneSegment.getDependentPeriodicActions(), hasSize(0));
    }

    @Test
    public void testReturnLimitedCountSegments() throws Exception {
        IntStream.range(25, 201)
                .mapToObj(i -> {
                    Segment segment = new Segment("segment_" + i);
                    segment.setName("Segment - " + i + " name");
                    segment.setConfig(new SegmentGroupPart());
                    return segment;
                })
                .forEach(segmentsDAO::createOrUpdateSegment);
        var segments = requestSegments(builder -> builder
                .param(LINKING_MODE, LinkingMode.ALL.name()));
        Assertions.assertEquals(100, segments.size());
    }

    @Test
    public void testAddSegmentInGroup() throws Exception {
        var segmentGroup = insertSegmentGroup();

        var segment = addSegmentWithGroupInfo(segmentGroup);

        Assertions.assertNotNull(segment.getGroup());
        Assertions.assertEquals(segmentGroup.getId(), segment.getGroup().getId());
    }

    @Test
    public void testListSegmentsContainGroupInfo() throws Exception {
        addSegmentWithGroupInfo(insertSegmentGroup());

        ReactTableRequest request = new ReactTableRequest();
        PagedResult<SegmentDto> result = querySegments(request);

        Assertions.assertTrue(result.getElements().stream().anyMatch(s -> s.getGroup() != null));

    }

    @Test
    public void testMoveSegmentWithoutGroupToGroup() throws Exception {
        var segmentDto = addSegmentWithGroupInfo(null);

        var segmentGroup = insertSegmentGroup();

        requestMoveSegmentToGroup(segmentGroup, segmentDto);

        var segment = getSegment(segmentDto.getId());

        Assertions.assertEquals(segmentGroup.getId(), segment.getGroupId());
    }

    @Test
    public void testMoveSegmentWithGroupToGroup() throws Exception {
        var segmentGroup = insertSegmentGroup();

        var segmentDto = addSegmentWithGroupInfo(segmentGroup);

        segmentGroup = insertSegmentGroup();

        requestMoveSegmentToGroup(segmentGroup, segmentDto);

        var segment = getSegment(segmentDto.getId());

        Assertions.assertEquals(segmentGroup.getId(), segment.getGroupId());
    }

    @Test
    public void testMoveSegmentToNonExistentGroup() throws Exception {
        var segmentDto = addSegmentWithGroupInfo(null);

        mockMvc.perform(put("/api/segment_groups/" + 123 + "/segments/" + segmentDto.getId()))
                .andExpect(status().isNotFound());
    }

    @NotNull
    private MvcResult requestMoveSegmentToGroup(SegmentGroup segmentGroup, SegmentDto segment) throws Exception {
        return mockMvc.perform(put("/api/segment_groups/" + segmentGroup.getId() + "/segments/" + segment.getId()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
    }

    private SegmentDto addSegmentWithGroupInfo(SegmentGroup segmentGroup) throws Exception {
        SegmentDto segment = new SegmentDto();
        segment.setGroup(segmentGroup);
        segment.setName("test_segment");

        SegmentGroupPart config = new SegmentGroupPart();
        config.setCondition(Condition.ALL);
        config.setParts(List.of(new SegmentAlgorithmPart()));
        segment.setConfig(config);

        return requestAddSegment(segment);
    }

    private SegmentGroup insertSegmentGroup() {
        SegmentGroup segmentGroup = new SegmentGroup();
        segmentGroup.setName("test_group");
        return segmentGroupDAO.insert(segmentGroup);
    }

    private List<SegmentDto> requestSegmentsWithSubs(String namePart) throws Exception {
        return requestSegments(builder -> builder
                .param("name_part", namePart)
                .param(TARGET_ID_TYPE, UidType.EMAIL.name())
                .param(LINKING_MODE, LinkingMode.ALL.name())
        );
    }

    private List<SegmentDto> requestSegments(
            Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> customiser) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/segments");
        builder = customiser.apply(builder);

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );
    }

    private SegmentDto requestAddSegment(SegmentDto segment) throws Exception {

        MvcResult result = mockMvc.perform(put("/api/segments/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(segment)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );
    }

    private PagedResult<SegmentDto> querySegments(ReactTableRequest request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/segments/list")
                .param("target_id_type", UidType.EMAIL.name())
                .param(LINKING_MODE, LinkingMode.ALL.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        jsonSerializer.writeObjectAsString(request)
                )
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                mvcResult.getResponse().getContentAsString()
        );
    }

    private SegmentDto requestSegment(String id) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/segments/get/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                mvcResult.getResponse().getContentAsString()
        );
    }

    private Segment getSegment(String segmentId) {
        Optional<Segment> segmentOpt = segmentsDAO.getSegmentById(segmentId);

        Assertions.assertTrue(segmentOpt.isPresent());
        return segmentOpt.get();
    }

    private void prepareBuilds(Segment segment, SegmentBuild... facts) {
        for (SegmentBuild fact : facts) {
            fact.setSegmentId(segment.getId())
                    .setConfig(segment.getConfig());

            buildDAO.insert(fact);
        }
    }
}
