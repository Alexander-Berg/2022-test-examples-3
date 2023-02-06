package ru.yandex.market.crm.bre.http.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.bre.test.AbstractControllerTest;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.services.segments.SegmentsDAO;
import ru.yandex.market.crm.core.test.utils.BlackboxHelper;
import ru.yandex.market.crm.core.test.utils.PlatformHelper;
import ru.yandex.market.crm.external.blackbox.response.UserInfo.Sex;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.platform.api.Edge;
import ru.yandex.market.crm.platform.api.IdsGraph;
import ru.yandex.market.crm.platform.api.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.BlackboxHelper.profile;

/**
 * @author apershukov
 */
public class SegmentsControllerTest extends AbstractControllerTest {

    private static User user(IdsGraph.Builder graph) {
        return User.newBuilder()
                .setIdsGraph(graph)
                .build();
    }

    private static Edge edge(int node1, int node2) {
        return Edge.newBuilder()
                .setNode1(node1)
                .setNode2(node2)
                .build();
    }

    private static final long PUID = 111;
    private static final String EMAIL = "user@yandex.ru";
    private static final String YUID = "111";

    @Inject
    private SegmentsDAO segmentsDAO;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private PlatformHelper platformHelper;

    @Inject
    private BlackboxHelper blackboxHelper;

    /**
     * Когда переданный идентификатор удовлетворяет условию и в Платформе есть пользователь
     * с таким идентфикатором в ответе ручки возвращается что пользователь входит в сегмент
     */
    @Test
    public void testWhenSpecifiedIdBelongsAndPlatformHasUser() throws Exception {
        Segment segment = prepareSegment();

        platformHelper.prepareUser(
                UidType.PUID,
                String.valueOf(PUID),
                user(
                        IdsGraph.newBuilder()
                                .addNode(Uids.create(UidType.PUID, PUID))
                                .addNode(Uids.create(UidType.EMAIL, EMAIL))
                                .addEdge(edge(0, 1))
                )

        );

        blackboxHelper.setUpResolveUserInfoByUid(
                PUID,
                profile(EMAIL, Sex.MALE)
        );

        Map<String, Boolean> response = requestSegments(Uid.asPuid(PUID), segment.getId());
        assertEquals(1, response.size());
        assertTrue(response.get(segment.getId()));
    }

    /**
     * Когда переданный идентификатор удовлетворяет условию и в Платформе нет пользователя
     * с таким идентфикатором в ответе ручки все равно возвращается что пользователь
     * входит в сегмент
     */
    @Test
    public void testWhenSpecifiedIdBelongsAndPlatformHasNoUser() throws Exception {
        Segment segment = prepareSegment();
        platformHelper.prepareUser(UidType.PUID, String.valueOf(PUID), null);

        blackboxHelper.setUpResolveUserInfoByUid(
                PUID,
                profile(EMAIL, Sex.MALE)
        );

        Map<String, Boolean> response = requestSegments(Uid.asPuid(PUID), segment.getId());
        assertEquals(1, response.size());
        assertTrue(response.get(segment.getId()));
    }

    /**
     * Когда переданный идентификатор не удовлетворяет условию но имеет непосредственно связанный
     * с ним идентификатор, удовлетворяющий условию, в ответе ручки сказано что пользователь
     * входит в сегмент.
     */
    @Test
    public void testWhenSpecifiedIdDoesNotBelongToSegmentBuItsLinkedIdDoes() throws Exception {
        Segment segment = prepareSegment();

        platformHelper.prepareUser(
                UidType.EMAIL,
                EMAIL,
                user(
                        IdsGraph.newBuilder()
                                .addNode(Uids.create(UidType.PUID, PUID))
                                .addNode(Uids.create(UidType.EMAIL, EMAIL))
                                .addEdge(edge(0, 1))
                )

        );

        blackboxHelper.setUpResolveUserInfoByUid(
                PUID,
                profile(EMAIL, Sex.MALE)
        );

        Map<String, Boolean> response = requestSegments(Uid.asEmail(EMAIL), segment.getId());
        assertEquals(1, response.size());
        assertTrue(response.get(segment.getId()));
    }

    /**
     * Когда переданный идентификатор не удовлетворяет условию, но при этом имеет транзитивно связанный
     * идентификатор, удовлетворяющий условию, в ответе ручки сказано что пользователь не входит в сегмент
     */
    @Test
    public void testWhenSpecifiedIdDoesNotBelongToSegmentBunItsTransitiveLinkedIdDoes() throws Exception {
        Segment segment = prepareSegment();

        platformHelper.prepareUser(
                UidType.EMAIL,
                EMAIL,
                user(
                        IdsGraph.newBuilder()
                                .addNode(Uids.create(UidType.PUID, PUID))
                                .addNode(Uids.create(UidType.YANDEXUID, YUID))
                                .addNode(Uids.create(UidType.EMAIL, EMAIL))
                                .addEdge(edge(0, 1))
                                .addEdge(edge(1, 2))
                )

        );

        blackboxHelper.setUpResolveUserInfoByUid(
                PUID,
                profile(EMAIL, Sex.MALE)
        );

        Map<String, Boolean> response = requestSegments(Uid.asEmail(EMAIL), segment.getId());
        assertEquals(1, response.size());
        assertFalse(response.get(segment.getId()));
    }

    /**
     * Пользователь успешно находится в Платформе по переданному yuid'у
     */
    @Test
    public void testCalculateSegmentByYuid() throws Exception {
        Segment segment = prepareSegment();

        platformHelper.prepareUser(
                UidType.YANDEXUID,
                YUID,
                user(
                        IdsGraph.newBuilder()
                                .addNode(Uids.create(UidType.PUID, PUID))
                                .addNode(Uids.create(UidType.YANDEXUID, YUID))
                                .addEdge(edge(0, 1))
                )

        );

        blackboxHelper.setUpResolveUserInfoByUid(
                PUID,
                profile(EMAIL, Sex.MALE)
        );

        Map<String, Boolean> response = requestSegments(Uid.asYuid(YUID), segment.getId());
        assertEquals(1, response.size());
        assertTrue(response.get(segment.getId()));
    }

    /**
     * В случае если передан идентификатор несуществующего сегмента
     * ручка возвращает 400
     */
    @Test
    public void test400OnUnknownSegmentId() throws Exception {
        makeRequest(Uid.asPuid(PUID), "iddqd")
                .andExpect(status().isBadRequest());
    }

    private Segment prepareSegment() {
        Segment segment = segment(
                passportGender("m")
        );

        segment.setId("seg_" + LocalDateTime.now());

        segmentsDAO.createOrUpdateSegment(segment);
        return segment;
    }

    private ResultActions makeRequest(Uid uid, String segmentId) throws Exception {
        return mockMvc.perform(get("/segments/belongs_to_segments")
            .param("id_type", uid.getType().name())
            .param("id_value", uid.getValue())
            .param("segments", segmentId))
            .andDo(print());
    }

    private Map<String, Boolean> requestSegments(Uid uid, String segmentId) throws Exception {
        MvcResult result = makeRequest(uid, segmentId)
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {},
                result.getResponse().getContentAsString()
        );
    }
}
