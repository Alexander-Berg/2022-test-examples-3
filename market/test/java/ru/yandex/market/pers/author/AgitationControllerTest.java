package ru.yandex.market.pers.author;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.agitation.AgitationController;
import ru.yandex.market.pers.author.agitation.AgitationService;
import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.agitation.model.AgitationLimitType;
import ru.yandex.market.pers.author.agitation.model.AgitationPreviewDto;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoListWithPriority;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.AgitationCancelReason;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.author.mock.PersAuthorSaasMocks;
import ru.yandex.market.pers.author.mock.mvc.AgitationMvcMocks;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.intThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.agitation.model.Agitation.FLAG_FALSE;
import static ru.yandex.market.pers.author.client.api.model.AgitationCancelReason.ANNOYING;
import static ru.yandex.market.pers.author.client.api.model.AgitationCancelReason.CANCEL;
import static ru.yandex.market.pers.author.client.api.model.AgitationCancelReason.REVOKE;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.MODEL_GRADE;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.MODEL_GRADE_PHOTO;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.MODEL_GRADE_TEXT;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.MODEL_QUESTION_ANSWER;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_CONFIRM_ITEMS_REMOVED_BY_USER;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_DELIVERY_CONVERTED_TO_ON_DEMAND_BY_USER;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_FEEDBACK;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.SHOP_GRADE;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.SHOP_GRADE_TEXT;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.ENTITY_ID_SAAS_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.IMPORTANT_SAAS_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.ORDER_ID_SAAS_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.PAY_AVAILABLE_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.PAY_AVAILABLE_SAAS_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.POPUP_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.POPUP_SAAS_KEY;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.SKU_SAAS_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.06.2020
 */
public class AgitationControllerTest extends AbstractAgitationControllerTest {
    private static final String MODEL_ID = "4524551";
    private static final String SHOP_ID = "112314";
    private static final String QUESTION_ID = "6262451";
    private static final String ORDER_ID = "2128306";
    private static final String SKU = "1029384756";
    public static final String ORDER_ID_KEY = "orderId";
    public static final String SKU_KEY = "sku";

    @Autowired
    private PersAuthorSaasMocks authorSaasMocks;

    @Autowired
    private AgitationMvcMocks agitationMvc;

    @Autowired
    private AgitationService agitationService;

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasPreviewAndPage(AgitationUser user) {
        int pageSize = 3;

        // generate agitations for 2-3 types in saas.
        // required only in this test. Other tests could be a bit simpler, containing fewer types and no paging
        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3), // check duplicates
            SHOP_GRADE,
            List.of(SHOP_ID, SHOP_ID + 1, SHOP_ID + 2),
            MODEL_QUESTION_ANSWER,
            List.of(QUESTION_ID,
                QUESTION_ID + 1,
                QUESTION_ID + 2,
                QUESTION_ID + 3,
                QUESTION_ID + 4,
                QUESTION_ID + 5,
                QUESTION_ID + 6),
            ORDER_FEEDBACK,
            List.of(QUESTION_ID,
                QUESTION_ID + 1,
                QUESTION_ID + 2,
                QUESTION_ID + 3,
                QUESTION_ID + 4,
                QUESTION_ID + 5,
                QUESTION_ID + 6),
            ORDER_CONFIRM_ITEMS_REMOVED_BY_USER,
            List.of(QUESTION_ID + 7),
            ORDER_DELIVERY_CONVERTED_TO_ON_DEMAND_BY_USER,
            List.of(QUESTION_ID + 8)
        ));

        // check preview
        List<AgitationPreviewDto> preview = agitationMvc.getPreview(
            user,
            pageSize,
            MODEL_GRADE,
            MODEL_GRADE_TEXT,
            SHOP_GRADE,
            ORDER_FEEDBACK,
            ORDER_CONFIRM_ITEMS_REMOVED_BY_USER,
            ORDER_DELIVERY_CONVERTED_TO_ON_DEMAND_BY_USER);

        Map<AgitationType, DtoPager<Agitation>> previewMap = ListUtils.toMap(preview,
            AgitationPreviewDto::getTypeEnum,
            x -> x);

        assertEquals(6, preview.size());
        assertAgitations(previewMap, MODEL_GRADE, 4, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        assertAgitations(previewMap, SHOP_GRADE, 3, SHOP_ID, SHOP_ID + 1, SHOP_ID + 2);
        assertAgitations(previewMap, MODEL_GRADE_TEXT, 0);
        assertAgitations(previewMap, ORDER_FEEDBACK, 7, QUESTION_ID,
            QUESTION_ID + 1,
            QUESTION_ID + 2);


        // check paging
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, pageSize),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 2, pageSize),
            MODEL_GRADE, MODEL_ID + 3);

        assertAgitations(agitationMvc.getPage(user, SHOP_GRADE, 1, pageSize),
            SHOP_GRADE, SHOP_ID, SHOP_ID + 1, SHOP_ID + 2);

        assertAgitations(agitationMvc.getPage(user, MODEL_QUESTION_ANSWER, 1, pageSize),
            MODEL_QUESTION_ANSWER, QUESTION_ID, QUESTION_ID + 1, QUESTION_ID + 2);
        assertAgitations(agitationMvc.getPage(user, MODEL_QUESTION_ANSWER, 2, pageSize),
            MODEL_QUESTION_ANSWER, QUESTION_ID + 3, QUESTION_ID + 4, QUESTION_ID + 5);
        assertAgitations(agitationMvc.getPage(user, MODEL_QUESTION_ANSWER, 3, pageSize),
            MODEL_QUESTION_ANSWER, QUESTION_ID + 6);

        // check pager
        DtoPager.Pager pager = agitationMvc.getPagePager(user, MODEL_QUESTION_ANSWER, 2, pageSize);
        assertEquals(3, pager.getTotalPageCount());
        assertEquals(2, pager.getPageNum());
        assertEquals(7, pager.getCount());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasPopup(AgitationUser user) {
        // generate agitations for 2-3 types in saas.
        // required only in this test. Other tests could be little bit simple, containing less types and no paging
        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3), // check duplicates
            SHOP_GRADE,
            List.of(SHOP_ID, SHOP_ID + 1),
            MODEL_QUESTION_ANSWER,
            List.of(QUESTION_ID,
                QUESTION_ID + 4,
                QUESTION_ID + 3,
                QUESTION_ID + 2,
                QUESTION_ID + 1)
        ));

        // check popup works
        List<Agitation> popup = agitationMvc.getPopup(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertAgitationIds(popup,
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID),
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID + 4),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );
        assertAgitation(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);
        assertAgitation(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);


        // check cache - reset mocks - same result
        resetMocks();
        popup = agitationMvc.getPopup(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertAgitationIds(popup,
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID),
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID + 4),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );
        assertNull(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID));
        assertAgitation(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);

        // resets after cache invalidation
        invalidateCache();
        popup = agitationMvc.getPopup(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertEquals(0, popup.size());
        assertNull(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID));
    }


    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasPopupWithHideExp(AgitationUser user) {
        expFlagService.setFlag(AgitationController.EXP_HIDE_UGC_POPUP, true);

        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID)
        ));

        // check popup works
        List<Agitation> popup = agitationMvc.getPopup(user, MODEL_GRADE);
        assertAgitationIds(popup);

        // disable - check works well
        expFlagService.setFlag(AgitationController.EXP_HIDE_UGC_POPUP, false);
        popup = agitationMvc.getPopup(user, MODEL_GRADE);
        assertAgitationIds(popup, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasTasks(AgitationUser user) {
        // generate agitations for 2-3 types in saas.
        // required only in this test. Other tests could be little bit simple, containing less types and no paging
        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3), // check duplicates
            SHOP_GRADE,
            List.of(SHOP_ID, SHOP_ID + 1),
            MODEL_QUESTION_ANSWER,
            List.of(QUESTION_ID,
                QUESTION_ID + 4,
                QUESTION_ID + 3,
                QUESTION_ID + 2,
                QUESTION_ID + 1)
        ));

        // check tasks works
        List<Agitation> tasks = agitationMvc.getTasks(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertAgitationIds(tasks,
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID),
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID + 4),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );
        assertAgitation(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);
        assertAgitation(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);


        // check cache - reset mocks - same result
        resetMocks();
        tasks = agitationMvc.getTasks(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertAgitationIds(tasks,
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID),
            Agitation.buildAgitationId(MODEL_QUESTION_ANSWER, QUESTION_ID + 4),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );
        assertNull(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID));
        assertAgitation(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);

        // resets after cache invalidation
        invalidateCache();
        tasks = agitationMvc.getTasks(user, MODEL_QUESTION_ANSWER, MODEL_GRADE, MODEL_GRADE_TEXT);
        assertEquals(0, tasks.size());
        assertNull(agitationMvc.getStatusFast(user, MODEL_GRADE, MODEL_ID));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationPopupPriority(AgitationUser user) {
        // generate agitations for 2-3 types in saas.
        // required only in this test. Other tests could be little bit simple, containing less types and no paging
        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3),
            MODEL_GRADE_TEXT,
            List.of(MODEL_ID + 4),
            SHOP_GRADE,
            List.of(SHOP_ID, SHOP_ID + 1)
        ));

        Mockito.when(random.nextInt(intThat(v -> v > 10) + 1)).thenReturn(0);
        Mockito.when(random.nextInt(2) + 1).thenReturn(1);

        // check popup works
        DtoListWithPriority<Agitation> popupDto = agitationMvc
            .getPopupDto(user, MODEL_GRADE, MODEL_GRADE_TEXT, SHOP_GRADE);

        assertAgitationIds(popupDto.getData(),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1),
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 4),
            Agitation.buildAgitationId(SHOP_GRADE, SHOP_ID),
            Agitation.buildAgitationId(SHOP_GRADE, SHOP_ID + 1)
        );
        assertNotNull(popupDto.getPriority());
        assertAgitation(popupDto.getPriority(), MODEL_GRADE, MODEL_ID + 1);

        // try other random (max random weight)
        Mockito.when(random.nextInt(intThat(v -> v > 10) + 1)).then(a -> (int) a.getArgument(0) - 1);
        Mockito.when(random.nextInt(2) + 1).thenReturn(0);

        popupDto = agitationMvc.getPopupDto(user, MODEL_GRADE, MODEL_GRADE_TEXT, SHOP_GRADE);
        assertNotNull(popupDto.getPriority());
        assertAgitation(popupDto.getPriority(), SHOP_GRADE, SHOP_ID);
    }


    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationPopupLimits(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, PAY_AVAILABLE_SAAS_KEY, "1")
            )
        )));

        // check popup works
        DtoListWithPriority<Agitation> popupDto = agitationMvc.getPopupDto(user, MODEL_GRADE);

        assertAgitationIds(popupDto.getData(),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );

        agitationMvc.limitPopup(user, "P2D");

        // check no agitations in popup, but all in pager
        assertEquals(0, agitationMvc.getPopupDto(user, MODEL_GRADE).getData().size());
        assertEquals(2, agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE).size());
        assertEquals(AgitationLimitType.FORBID, agitationService.canShowAgitation(user));

        // shift to past (don't forget to reset cache)
        jdbcTemplate.update("update pers.agitation_limit_users set cr_time = now() - make_interval(hours := ?::int)",
            AgitationLimitType.LIMIT_PRIORITY_HOURS.toHours() + 1);
        invalidateCache();

        //show only priority
        assertEquals(1, agitationMvc.getPopupDto(user, MODEL_GRADE).getData().size());
        assertAgitations(agitationMvc.getPopupDto(user, MODEL_GRADE).getData(), MODEL_GRADE, MODEL_ID + 1);
        assertEquals(AgitationLimitType.PRIORITY, agitationService.canShowAgitation(user));

        agitationMvc.enablePopup(user);

        // check agitations enabled
        assertEquals(2, agitationMvc.getPopupDto(user, MODEL_GRADE).getData().size());
        assertEquals(AgitationLimitType.ALLOW, agitationService.canShowAgitation(user));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationTasksDontUseLimits(AgitationUser user) {
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // check tasks works
        List<Agitation> tasks = agitationMvc.getTasks(user, MODEL_GRADE);

        assertAgitationIds(tasks,
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1)
        );

        agitationMvc.limitPopup(user, "P2D");

        // check all agitations in tasks
        assertEquals(2, agitationMvc.getTasks(user, MODEL_GRADE).size());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationPopupEmpty(AgitationUser user) {
        authorSaasMocks.mockAgitation(user, Map.of(
            MODEL_GRADE,
            List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3)
        ));

        // check popup works
        DtoListWithPriority<Agitation> popupDto = agitationMvc.getPopupDto(user, SHOP_GRADE);
        assertEquals(0, popupDto.getData().size());
        assertNull(popupDto.getPriority());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndCancel(AgitationUser user) {
        // three agitations: one simple, one canceled, one canceled and expired
        // only two should work

        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2)));

        // cancel agitations
        agitationMvc.cancel(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), CANCEL);
        agitationMvc.cancel(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2), CANCEL);

        disableAgitationCancel(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2));

        // load agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 2);
    }


    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationUgcTasks(AgitationUser user) {
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2)));

        // delay agitations
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), null);
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2), null);

        // load agitations - regular should contain all, ugc should contain same
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        assertAgitations(agitationMvc.getPageUgcTasks(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);

        // reset and check all shown
        agitationMvc.reset(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));
        agitationMvc.reset(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2));

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        assertAgitations(agitationMvc.getPageUgcTasks(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasWithJsonData(AgitationUser user) {
        // three agitations: one simple, one with order_id and one with sku+unknown

        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            SHOP_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID + 1, ORDER_ID_SAAS_KEY, ORDER_ID)
            ),
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, SKU_SAAS_KEY, SKU, "unknownKey", "unknownValue")
            )
        )));

        List<Agitation> expected = List.of(
            new Agitation(SHOP_GRADE, SHOP_ID),
            new Agitation(SHOP_GRADE, SHOP_ID + 1, Map.of(ORDER_ID_KEY, ORDER_ID)),
            new Agitation(MODEL_GRADE, MODEL_ID, Map.of(SKU_KEY, SKU))
        );

        // load agitations
        List<Agitation> actual = agitationMvc.getPage(user, SHOP_GRADE, 1, PAGE_SIZE);
        actual.addAll(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE));
        assertEquals(expected, actual);

        assertAgitation(agitationMvc.getStatus(user, SHOP_GRADE, SHOP_ID), SHOP_GRADE, SHOP_ID);
        assertAgitation(agitationMvc.getStatus(user, SHOP_GRADE, SHOP_ID + 1), SHOP_GRADE, SHOP_ID + 1);
        assertEquals(ORDER_ID, agitationMvc.getStatus(user, SHOP_GRADE, SHOP_ID + 1).getData().get(ORDER_ID_KEY));
        assertAgitation(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);
        assertEquals(SKU, agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID).getData().get(SKU_KEY));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasWithPayAvailable(AgitationUser user) {
        final String payAvailable = "1";
        final String payNotAvailable = "0";

        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            SHOP_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID, PAY_AVAILABLE_SAAS_KEY, payAvailable),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID + 1, PAY_AVAILABLE_SAAS_KEY, payNotAvailable)
            ),
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, PAY_AVAILABLE_SAAS_KEY, payAvailable),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, PAY_AVAILABLE_SAAS_KEY, payNotAvailable)
            )
        )));

        List<Agitation> expected = List.of(
            new Agitation(SHOP_GRADE, SHOP_ID, Map.of(PAY_AVAILABLE_KEY, "1")),
            new Agitation(SHOP_GRADE, SHOP_ID + 1, Map.of(PAY_AVAILABLE_KEY, "0")),
            new Agitation(MODEL_GRADE, MODEL_ID, Map.of(PAY_AVAILABLE_KEY, "1")),
            new Agitation(MODEL_GRADE, MODEL_ID + 1, Map.of(PAY_AVAILABLE_KEY, "0"))
        );

        // load agitations
        List<Agitation> actual = agitationMvc.getPage(user, SHOP_GRADE, 1, PAGE_SIZE);
        actual.addAll(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE));
        assertEquals(expected, actual);

        assertAgitation(agitationMvc.getStatus(user, SHOP_GRADE, SHOP_ID), SHOP_GRADE, SHOP_ID);
        assertAgitation(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID), MODEL_GRADE, MODEL_ID);
        assertEquals(payAvailable, agitationMvc.getStatus(user, SHOP_GRADE, SHOP_ID).getData().get(PAY_AVAILABLE_KEY));
        assertEquals(payAvailable,
            agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID).getData().get(PAY_AVAILABLE_KEY));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasDuplicates(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            SHOP_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID + 1, ORDER_ID_SAAS_KEY, ORDER_ID),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID, ORDER_ID_SAAS_KEY, ORDER_ID)
            ),
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, SKU_SAAS_KEY, SKU),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, ORDER_ID_SAAS_KEY, ORDER_ID)
            )
        )));

        List<Agitation> expected = List.of(
            new Agitation(SHOP_GRADE, SHOP_ID),
            new Agitation(SHOP_GRADE, SHOP_ID + 1, Map.of(ORDER_ID_KEY, ORDER_ID)),
            new Agitation(MODEL_GRADE, MODEL_ID, Map.of(SKU_KEY, SKU))
        );

        // load agitations
        List<Agitation> actual = agitationMvc.getPage(user, SHOP_GRADE, 1, PAGE_SIZE);
        actual.addAll(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE));
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasWithDataStrict(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, Map.of(
            "agt0",
            "[{\\\"eid\\\":\\\"981711\\\",\\\"sku\\\":\\\"1134\\\"},{\\\"eid\\\":\\\"23434\\\"," +
                "\\\"sku\\\":\\\"1134\\\"}]",

            "agt5",
            "[{\\\"eid\\\":\\\"111\\\",\\\"oid\\\":4151},{\\\"eid\\\":\\\"4545\\\",\\\"sku\\\":\\\"1134\\\"}]"
        ));

        List<Agitation> expected = List.of(
            new Agitation(MODEL_GRADE, "981711", Map.of(SKU_KEY, "1134")),
            new Agitation(MODEL_GRADE, "23434", Map.of(SKU_KEY, "1134")),
            new Agitation(MODEL_QUESTION_ANSWER, "111", Map.of(ORDER_ID_KEY, "4151")),
            new Agitation(MODEL_QUESTION_ANSWER, "4545", Map.of(SKU_KEY, "1134"))
        );

        // load agitations
        List<Agitation> actual = agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE);
        actual.addAll(agitationMvc.getPage(user, SHOP_GRADE, 1, PAGE_SIZE));
        actual.addAll(agitationMvc.getPage(user, MODEL_QUESTION_ANSWER, 1, PAGE_SIZE));
        assertEquals(expected, actual);
    }

    @Test
    public void testAgitationKeyMocking() {
        assertEquals(
            Map.of(
                "agt3", "[{\\\"eid\\\":\\\"111\\\"},{\\\"eid\\\":\\\"112\\\"}]",
                "agt0", "[{\\\"eid\\\":\\\"333\\\"}]"
            ),
            PersAuthorSaasMocks.buildSimpleAgitations(Map.of(
                SHOP_GRADE, List.of("111", "112"),
                MODEL_GRADE, List.of("333")
            ))
        );

        // note: treeMap is required to keep results sorted on assert
        assertEquals(
            Map.of(
                "agt3", "[{\\\"eid\\\":\\\"111\\\"},{\\\"eid\\\":\\\"112\\\",\\\"oid\\\":\\\"222\\\"}]",
                "agt0", "[{\\\"eid\\\":\\\"333\\\",\\\"sku\\\":\\\"444\\\",\\\"unknownKey\\\":\\\"value\\\"}]"
            ),
            PersAuthorSaasMocks.buildAgitations(Map.of(
                SHOP_GRADE, List.of(
                    new TreeMap<>(Map.of(ENTITY_ID_SAAS_KEY, "111")),
                    new TreeMap<>(Map.of(ENTITY_ID_SAAS_KEY, "112", ORDER_ID_SAAS_KEY, "222"))
                ),
                MODEL_GRADE, List.of(
                    new TreeMap<>(Map.of(ENTITY_ID_SAAS_KEY, "333", SKU_SAAS_KEY, "444", "unknownKey", "value"))
                )
            ))
        );
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndCancelInvalid(AgitationUser user) {
        // can't cancel with system reasons

        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // cancel agitations
        Arrays.stream(AgitationCancelReason.values())
            .filter(AgitationCancelReason::isInternal)
            .forEach(reason -> {
                agitationMvc.cancel(
                    user,
                    Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1),
                    reason,
                    status().is4xxClientError());
            });

        // load agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationJsonPopup(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            SHOP_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID + 1, POPUP_SAAS_KEY, "0"),
                Map.of(ENTITY_ID_SAAS_KEY, SHOP_ID + 2, POPUP_SAAS_KEY, "1")
            ),
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, POPUP_SAAS_KEY, "1"),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 2, POPUP_SAAS_KEY, "false")
            )
        )));

        List<Agitation> expectedFull = List.of(
            new Agitation(SHOP_GRADE, SHOP_ID),
            new Agitation(SHOP_GRADE, SHOP_ID + 1, Map.of(POPUP_KEY, "0")),
            new Agitation(SHOP_GRADE, SHOP_ID + 2, Map.of(POPUP_KEY, "1"))
        );

        List<Agitation> expectedPopup = List.of(
            new Agitation(SHOP_GRADE, SHOP_ID),
            new Agitation(SHOP_GRADE, SHOP_ID + 2, Map.of(POPUP_KEY, "1"))
        );

        List<Agitation> expectedPopupModel = List.of(
            new Agitation(MODEL_GRADE, MODEL_ID + 1, Map.of(POPUP_KEY, "1")),
            new Agitation(MODEL_GRADE, MODEL_ID + 2, Map.of(POPUP_KEY, "false"))
        );

        // load agitations
        assertEquals(expectedFull, agitationMvc.getPage(user, SHOP_GRADE, 1, PAGE_SIZE));
        assertEquals(expectedPopup, agitationMvc.getPopup(user, SHOP_GRADE));
        assertEquals(expectedPopupModel, agitationMvc.getPopup(user, MODEL_GRADE));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndDelayWithPopup(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // cancel agitations
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), Duration.ofDays(2));

        // load agitations
        List<Agitation> popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitations(popup, MODEL_GRADE, MODEL_ID);

        // move time - now one more is available
        shiftCancelEndDateForAll(3);
        invalidateCache();

        popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitations(popup, MODEL_GRADE, MODEL_ID);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndHidePopup(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // cancel agitations
        agitationMvc.cancel(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), ANNOYING);

        // load agitations - hidden in popup
        List<Agitation> popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitations(popup, MODEL_GRADE, MODEL_ID);

        // shows in full list
        List<Agitation> expectedFull = List.of(
            new Agitation(MODEL_GRADE, MODEL_ID),
            new Agitation(MODEL_GRADE, MODEL_ID + 1)
        );
        assertEquals(expectedFull, agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE));

        // move time almost to an end- nothing happens
        shiftCancelEndDateForAll(AgitationService.INTERVAL_CANCEL_FOREVER_DAYS - 1);
        invalidateCache();

        // still not in popup
        popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitations(popup, MODEL_GRADE, MODEL_ID);

        // shift little bit more
        shiftCancelEndDateForAll(2);
        invalidateCache();

        // cancels are expired, both agitations are now active
        // can't happen really since cancels are stored longer then adds
        popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitations(popup, MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
        assertEquals(expectedFull, agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationDelayNTimes(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // delay agitation N-1 times
        String delayedAgitation = Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1);
        for (int idx = 0; idx < AgitationService.DELAYS_TO_MARK_ANNOYING - 1; ++idx) {
            agitationMvc.delay(user, delayedAgitation, Duration.ofDays(2));
        }

        assertTrue(agitationService.getLastCancels(user, delayedAgitation).isEmpty());

        // delay one more time
        agitationMvc.delay(user, delayedAgitation, Duration.ofDays(2));

        assertEquals(ANNOYING, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationDelayAfterCancel(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // cancel agitation
        String delayedAgitation = Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1);
        agitationMvc.cancel(user, delayedAgitation, CANCEL);

        assertEquals(CANCEL, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());

        // delay one more time
        agitationMvc.delay(user, delayedAgitation, Duration.ofDays(2));

        // still cancel
        assertEquals(CANCEL, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationDelayUnimportantAgitation(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitationPlain(user, PersAuthorSaasMocks.buildAgitations(Map.of(
            MODEL_GRADE, List.of(
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID),
                Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, IMPORTANT_SAAS_KEY, FLAG_FALSE)
            )
        )));

        // delay agitation
        String delayedAgitation = Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1);
        agitationMvc.delay(user, delayedAgitation, Duration.ofDays(2));

        // instantly annoying!
        assertEquals(ANNOYING, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationReset(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // delay agitation
        String delayedAgitation = Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1);
        agitationMvc.cancel(user, delayedAgitation, null); // also check method works without param

        assertEquals(CANCEL, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID);

        agitationMvc.reset(user, delayedAgitation);
        assertEquals(REVOKE, agitationService.getLastCancels(user, delayedAgitation).get(0).getReason());

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndDelay(AgitationUser user) {
        // three agitations: one simple, one delayed
        // forward time and call again - both should work

        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2)));

        // cancel agitations
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), Duration.ofDays(2));
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2), null);

        // load agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);

        // move time - now one more is available
        shiftCancelEndDateForAll(3);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);

        // move time - now all are available (default one too)
        shiftCancelEndDateForAll(AgitationService.INTERVAL_DELAY_DEFAULT_DAYS);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndDelayWithPageAndPreview(AgitationUser user) {
        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2)));

        // cancel agitations
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), Duration.ofDays(2));
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2), null);

        assertAgitations(agitationMvc.getPopup(user, AgitationType.values()), MODEL_GRADE, MODEL_ID);
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        Map<AgitationType, DtoPager<Agitation>> previewMap = ListUtils.toMap(
            agitationMvc.getPreview(user, PAGE_SIZE, MODEL_GRADE),
            AgitationPreviewDto::getTypeEnum,
            x -> x);
        assertAgitations(previewMap, MODEL_GRADE, 3, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationDelayForMaxDays(AgitationUser user) {
        // three agitations: one simple, one delayed
        // forward time and call again - both should work

        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // cancel agitations
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Duration.ofDays(AgitationService.INTERVAL_CANCEL_AND_ADD_STORE_DAYS), status().is2xxSuccessful());
        agitationMvc.delay(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1),
            Duration.ofDays(AgitationService.INTERVAL_CANCEL_AND_ADD_STORE_DAYS + 1), status().is4xxClientError());
    }


    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndNext(AgitationUser user) {
        // one agitation in saas, one completed
        // expect two agitations

        // generate saas data
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID)));

        // complete some agitations
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 2));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 3));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 4));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID + 3));
        agitationMvc.complete(user, Agitation.buildAgitationId(SHOP_GRADE, SHOP_ID));
        agitationMvc.complete(user, Agitation.buildAgitationId(SHOP_GRADE_TEXT, SHOP_ID + 1));

        // check agitations
        // load with popup to show all in single request
        List<Agitation> popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitationIds(popup,
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 1),
            Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID + 4),
            Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID + 2),
            Agitation.buildAgitationId(SHOP_GRADE_TEXT, SHOP_ID)
        );
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationNextDataTransfer(AgitationUser user) {
        // one agitation in saas, completed
        // expect agitation with same data

        // generate agitation data
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(
                    new TreeMap<>(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, SKU_SAAS_KEY, "test value")),
                    new TreeMap<>(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, SKU_SAAS_KEY, "value will be overwritten"))
                )
            )));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 1, true, null, null,
            Map.of(SKU_SAAS_KEY, "overriding value"), status().is2xxSuccessful());

        // complete agitation
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));

        // check agitations
        // load with popup to show all in single request
        List<Agitation> popup = agitationMvc.getPopup(user, AgitationType.values());
        assertAgitationIds(popup,
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID + 1),
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID));
        // data taken from forced agitation
        assertEquals(Map.of(SKU_SAAS_KEY, "overriding value"), popup.get(0).getData());
        // data taken from saas agitation
        assertEquals(Map.of(SKU_SAAS_KEY, "test value"), popup.get(1).getData());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationAdd(AgitationUser user) {
        // check agitation added and does not affects result
        // generate saas data
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // add some agitations, one duplicated
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 3, false);
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 2, false);
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 3, false);
        agitationMvc.addAgitation(user, SHOP_GRADE, SHOP_ID, false);
        agitationMvc.addAgitation(user,
            SHOP_GRADE,
            SHOP_ID + 1,
            false,
            null,
            null,
            Map.of(ORDER_ID_KEY, ORDER_ID),
            status().is2xxSuccessful());

        // check agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);

        assertEquals(0, agitationService.getForcedAgitations(user, null).size());

        List<Agitation> addedAgitations = agitationService.getAddedAgitations(user, false, null);
        assertEquals(4, addedAgitations.size());
        assertAgitationIds(addedAgitations,
            Agitation.buildAgitationId(SHOP_GRADE, SHOP_ID + 1),
            Agitation.buildAgitationId(SHOP_GRADE, SHOP_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 3),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2)
        );

        assertTrue(addedAgitations.get(0).getData().containsKey(ORDER_ID_KEY));
        assertEquals(ORDER_ID, addedAgitations.get(0).getData().get(ORDER_ID_KEY));

        // cancel one agitation
        agitationMvc.cancel(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1), CANCEL);
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID);

        // add again manually (reset canceling)
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 1, false);
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);

    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndForce(AgitationUser user) {
        // check agitation forced to saas
        // mock 2 agitations in saas, add 2 forced manually. One intersects
        // also add one short agitation and one delayed
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // add some agitations, one duplicated
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 1, true);
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 2, true);
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 3, true, null, Duration.ofHours(1));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 4, true, Duration.ofHours(3), Duration.ofHours(2));

        // check agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 3, MODEL_ID + 2, MODEL_ID + 1, MODEL_ID);

        // change time, check again (duration ends)
        shiftCreateDateForAll(2);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 2, MODEL_ID + 1, MODEL_ID);

        // change time, check again (delayed starts to act)
        shiftCreateDateForAll(2);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 4, MODEL_ID + 2, MODEL_ID + 1, MODEL_ID);

        // change time, check again (delayed and short-durated ends both)
        shiftCreateDateForAll(2);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 2, MODEL_ID + 1, MODEL_ID);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAddTooLongAgitation(AgitationUser user) {
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        // add some agitations, one duplicated
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 2, true, null,
            Duration.ofDays(AgitationService.INTERVAL_ADD_MAX_DAYS - 1));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 3, true, null,
            Duration.ofDays(AgitationService.INTERVAL_ADD_MAX_DAYS));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 4, true, null,
            Duration.ofDays(AgitationService.INTERVAL_ADD_MAX_DAYS + 1), status().is4xxClientError());
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 5, true,
            Duration.ofDays(AgitationService.INTERVAL_ADD_MAX_DAYS),
            Duration.ofDays(10));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 6, true,
            Duration.ofDays(AgitationService.INTERVAL_ADD_MAX_DAYS + 1),
            Duration.ofDays(1), status().is4xxClientError());

        // check agitations
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 3, MODEL_ID + 2, MODEL_ID, MODEL_ID + 1);

        // change time, check again (duration ends)
        shiftCreateDateForAllDays(AgitationService.INTERVAL_ADD_MAX_DAYS + 2);

        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 5, MODEL_ID, MODEL_ID + 1);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAddThenCancel(AgitationUser user) {
        // add agitation, then complete and see than it never appears visible again
        authorSaasMocks.mockAgitation(user, Map.of(MODEL_GRADE, List.of(MODEL_ID, MODEL_ID + 1)));

        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 2, true);

        // check agitation  added
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID + 2, MODEL_ID, MODEL_ID + 1);
        assertAgitation(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID + 2), MODEL_GRADE, MODEL_ID + 2);

        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2));

        // check agitation not added
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
        assertNull(agitationMvc.getStatus(user, MODEL_GRADE, MODEL_ID + 2));

        // shift agitation add and cancel on edge
        shiftCancelEndDateForAll(AgitationService.INTERVAL_ADD_MAX_DAYS - 1);
        shiftCreateDateForAllDays(AgitationService.INTERVAL_ADD_MAX_DAYS - 1);

        // check agitation still not added
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);

        // shift over edge of creation
        shiftCancelEndDateForAll(2);
        shiftCreateDateForAllDays(2);

        // check agitation still not visible
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);

        // disable cancel
        shiftCancelEndDateForAll(AgitationService.INTERVAL_CANCEL_FOREVER_DAYS);

        // check agitation is expired itself
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE),
            MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndNextAndForceAndCancel(AgitationUser user) {
        // should be in order next -> saas -> force
        // force 1, 2, 3 agitations
        // by next generate 3, 5, 6
        // in saas 2, 4, 6
        // result should be 3 (force), 2 (force), 1 (force), 5 (next), 4 (saas), 6 (saas)

        authorSaasMocks.mockAgitation(user,
            Map.of(MODEL_GRADE_TEXT, List.of(MODEL_ID + 2, MODEL_ID + 4, MODEL_ID + 6)));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 4));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 5));
        agitationMvc.addAgitation(user, MODEL_GRADE_TEXT, MODEL_ID + 1, true);
        agitationMvc.addAgitation(user, MODEL_GRADE_TEXT, MODEL_ID + 2, true);
        agitationMvc.addAgitation(user, MODEL_GRADE_TEXT, MODEL_ID + 3, true);

        // check result
        assertAgitations(agitationMvc.getPage(user, MODEL_GRADE_TEXT, 1, PAGE_SIZE),
            MODEL_GRADE_TEXT, MODEL_ID + 3, MODEL_ID + 2, MODEL_ID + 1, MODEL_ID + 5, MODEL_ID + 4, MODEL_ID + 6);
    }

    @Test
    public void testDataCleanup() {
        Map<String, String> source = Map.of("test", "1", PAY_AVAILABLE_KEY, "0");
        Map<String, String> sourceClean = Map.of("test", "1");

        assertEquals(source, agitationService.preprocessData(source, false));
        assertEquals(sourceClean, agitationService.preprocessData(source, true));

        assertTrue(sourceClean == agitationService.preprocessData(sourceClean, false));
        assertTrue(sourceClean == agitationService.preprocessData(sourceClean, true));
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testallAgitationsWithHistory(AgitationUser user) {
        // one agitation for MODEL completed, none in saas
        // one agitation for MODEL + 1 in saas
        // one agitation for MODEL + 2 in saas, completed
        // one agitation for MODEL + 3 added
        // one agitation for MODEL + 4 added, completed

        // mock saas answer to provide data for canceled agitation, than cancel it,
        // than change mock, so we do not receive this agitation from saas when checking
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, PAY_AVAILABLE_SAAS_KEY, "1"))
            )));
        // complete two times to check that data transfers from already completed agitation
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));

        // prepare other agitations
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(
                    Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, PAY_AVAILABLE_SAAS_KEY, "1"),
                    Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 2, PAY_AVAILABLE_SAAS_KEY, "1")
                )
            )));
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 3, true, null, null,
            Map.of(PAY_AVAILABLE_KEY, "1"), status().is2xxSuccessful());
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 4, true, null, null,
            Map.of(PAY_AVAILABLE_KEY, "1"), status().is2xxSuccessful());
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2));
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 4));

        // check batch entityId
        List<Agitation> paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);
        paid.sort(Comparator.comparing(Agitation::getId));
        assertAgitationIds(paid,
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1),
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2)
        );


        // check agitations
        // should receive canceled
        List<Agitation> saasAgitations = agitationMvc.getPage(user, MODEL_GRADE, 1, PAGE_SIZE)
            .stream()
            .filter(agitation -> agitation.getEntityId().equals(MODEL_ID))
            .collect(Collectors.toList());
        assertTrue(saasAgitations.isEmpty());
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "1"), paid.get(0).getData());

        // check agitations
        // should receive saas agitation
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID + 1);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "1"), paid.get(0).getData());

        // check agitations
        // should receive saas (canceled is duplicate)
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID + 2);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 2));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "1"), paid.get(0).getData());

        // check agitations
        // should receive forced agitation
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID + 3);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 3));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "1"), paid.get(0).getData());

        // check agitations
        // should receive forced (canceled is duplicate), but not next agitation, coz AgitationType
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID + 4);
        assertAgitationIds(paid,
            Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 4));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "1"), paid.get(0).getData());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testallAgitationsWithHistoryAgitationChain(AgitationUser user) {
        // model chain is 0 -> 1 -> 2

        // mock types 1 and 2 for different models
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE_TEXT, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID)),
                MODEL_GRADE_PHOTO, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1))
            )));

        // should return 1 and 2 when asked 0
        List<Agitation> paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID, MODEL_ID + 1);
        assertAgitationIds(paid,
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID + 1));

        // should return 1 and 2 asked 1
        paid = agitationMvc.getExisted(user, MODEL_GRADE_TEXT, MODEL_ID, MODEL_ID + 1);
        assertAgitationIds(paid,
            Agitation.buildAgitationId(MODEL_GRADE_TEXT, MODEL_ID),
            Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID + 1));

        // mock types 0 and 2 for same model
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID)),
                MODEL_GRADE_PHOTO, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID))
            )));

        // should return 0 when asked 0
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));

        // should return 2 when asked 1
        paid = agitationMvc.getExisted(user, MODEL_GRADE_TEXT, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE_PHOTO, MODEL_ID));

    }


    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testallAgitationsWithHistorySourcePriority(AgitationUser user) {
        // Priority on deduplication is agitation_add -> saas -> agitation_cancel

        // create cancel. Data is empty, coz there is no previous agitation with data
        agitationMvc.complete(user, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));

        // check that agitation came from agitation_cancel by data
        List<Agitation> paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        assertNull(paid.get(0).getData());

        // create SaaS response. Provide "marker" in Data
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID, PAY_AVAILABLE_SAAS_KEY, "saas"))
            )));

        // check that agitation came from SaaS by data
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "saas"), paid.get(0).getData());

        // create forced agitation. Provide "marker" in Data
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID, true, null, null,
            Map.of(PAY_AVAILABLE_KEY, "add"), status().is2xxSuccessful());

        // check that agitation came from agitation_add by data
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "add"), paid.get(0).getData());

        // create forced, than mock SaaS and still got agitation from agitation_add
        agitationMvc.addAgitation(user, MODEL_GRADE, MODEL_ID + 1, true, null, null,
            Map.of(PAY_AVAILABLE_KEY, "add"), status().is2xxSuccessful());
        authorSaasMocks.mockAgitationPlain(user,
            PersAuthorSaasMocks.buildAgitations(Map.of(
                MODEL_GRADE, List.of(Map.of(ENTITY_ID_SAAS_KEY, MODEL_ID + 1, PAY_AVAILABLE_SAAS_KEY, "saas"))
            )));
        paid = agitationMvc.getExisted(user, MODEL_GRADE, MODEL_ID + 1);
        assertAgitationIds(paid, Agitation.buildAgitationId(MODEL_GRADE, MODEL_ID + 1));
        assertEquals(Map.of(PAY_AVAILABLE_KEY, "add"), paid.get(0).getData());

    }

    public static Stream<Arguments> allUserTypes() {
        return Arrays.stream(AgitationUserType.values())
            .map(userType -> {
                switch (userType) {
                    case UID:
                        return AgitationUser.uid(32431314);
                    case YANDEXUID:
                        return AgitationUser.yandexUid("ABC1334134ZZZ");
                    default:
                        throw new IllegalArgumentException("Invalid user type: " + userType);
                }
            })
            .map(Arguments::of);
    }
}
