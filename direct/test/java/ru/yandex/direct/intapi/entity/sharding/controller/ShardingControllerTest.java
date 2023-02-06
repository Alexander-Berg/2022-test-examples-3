package ru.yandex.direct.intapi.entity.sharding.controller;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.sharding.ShardingController;
import ru.yandex.direct.intapi.entity.sharding.model.GenerateObjectIdsRequest;
import ru.yandex.direct.intapi.entity.sharding.model.GenerateObjectIdsResponse;
import ru.yandex.direct.utils.JsonUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class ShardingControllerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GET_OBJECT_IDS_URL = "/sharding/generateObjectIds";
    @Autowired
    private ShardingController controller;

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    private MockMvc mockMvc;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void generatePharseIds() throws Exception {
        checkGetIds("phid", () -> shardHelper.generatePhraseIds(1));
    }

    @Test
    public void generateBannerIds() throws Exception {
        checkGetIds("bid", () -> shardHelper.generateBannerIds(Collections.singletonList(1L)));
    }

    @Test
    public void generateAdGroupIds() throws Exception {
        checkGetIds("pid", () -> shardHelper.generateAdGroupIds(clientInfo.getClientId().asLong(), 1));
    }

    @Test
    public void generateAdditionalTargetingIds() throws Exception {
        //!! в perl'овом SHARD_KEYS называется просто "id"
        checkGetIds("adgroup_additional_targeting_id", () -> shardHelper.generateAdGroupAdditionalTargetingIds(1));
    }

    @Test
    public void generateAditionsItemIds() throws Exception {
        checkGetIds("additions_item_id", () -> shardHelper.generateAdditionItemIds(1));
    }

    @Test
    public void generateBannerCreativeIds() throws Exception {
        checkGetIds("banner_creative_id", () -> shardHelper.generateBannerCreativeIds(1));
    }

    @Test
    public void generateImageIds() throws Exception {
        checkGetIds("image_id", () -> shardHelper.generateImageIds(1));
    }

    @Test
    public void generateMobileContentIds() throws Exception {
        checkGetIds("mobile_content_id", () -> shardHelper.generateMobileContentIds(1));
    }

    @Test
    public void generateMobileAppTrackerId() throws Exception {
        checkGetIds("mobile_app_tracker_id", () -> shardHelper.generateMobileAppTrackerIds(1));
    }

    @Test
    public void generateImagesPoolIds() throws Exception {
        checkGetIds("banner_images_pool_id", () ->
                shardHelper.generateBannerPoolImageIds(clientInfo.getClientId().asLong(), 1));
    }

    @Test
    public void generateSlIds() throws Exception {
        checkGetIds("sl_id", () -> shardHelper.generateSitelinkIds(1));
    }

    @Test
    public void generateSitelinkSetIds() throws Exception {
        checkGetIds("sitelinks_set_id", () ->
                shardHelper.generateSitelinkSetIds(clientInfo.getClientId().asLong(), 1));
    }

    @Test
    public void generateRetargetingIds() throws Exception {
        checkGetIds("ret_id", () -> shardHelper.generateRetargetingIds(1));
    }

    @Test
    public void generateRetCondIds() throws Exception {
        checkGetIds("ret_cond_id", () -> shardHelper.generateRetargetingConditionIds(
                Collections.singletonList(clientInfo.getClientId().asLong())));
    }

    @Test
    public void generateVcardIds() throws Exception {
        checkGetIds("vcard_id", () -> shardHelper.generateVcardIds(clientInfo.getClientId().asLong(), 1));
    }

    @Test
    public void generateHierarchicalMultiplierIds() throws Exception {
        checkGetIds("hierarchical_multiplier_id", () -> shardHelper.generateHierarchicalMultiplierIds(1));
    }

    @Test
    public void generateDynamicRuleIds() throws Exception {
        checkGetIds("dyn_id", () -> shardHelper.generateDynamicIds(1));
    }

    @Test
    public void generatePerformanceFilterIds() throws Exception {
        checkGetIds("perf_filter_id", () -> shardHelper.generatePerformanceFilterIds(1));
    }

    private void checkGetIds(String type, Supplier<List<Long>> idGenerator) throws Exception {
        int desiredCount = 50;
        Long previosId = idGenerator.get().get(0);
        GenerateObjectIdsRequest request = new GenerateObjectIdsRequest(
                type,
                desiredCount,
                clientInfo.getClientId());


        String response = makeRequest(JsonUtils.toJson(request));

        Long nextId = idGenerator.get().get(0);
        GenerateObjectIdsResponse ids = JsonUtils.fromJson(response, GenerateObjectIdsResponse.class);
        List<Long> givenIds = ids.getIds();

        assertThat("Вернулось требуемое количество id", givenIds.size(), is(desiredCount));
        assertThat("Минимальный возвращенный идентификатор больше эталонного предыдущего",
                givenIds.get(0), greaterThan(previosId));
        assertThat("Максимальный возвращенный идентификатор меньше эталонного следующего",
                givenIds.get(givenIds.size() - 1), lessThan(nextId));
    }

    private String makeRequest(String content) throws Exception {
        return mockMvc.perform(post(GET_OBJECT_IDS_URL)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(UTF_8.name()))

                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}

