package ru.yandex.market.crm.campaign.http.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.campaign.domain.grouping.group.GroupNode;
import ru.yandex.market.crm.campaign.domain.grouping.group.SegmentGroup;
import ru.yandex.market.crm.campaign.domain.segment.SegmentNode;
import ru.yandex.market.crm.campaign.dto.segment.SegmentDto;
import ru.yandex.market.crm.campaign.services.grouping.group.SegmentGroupDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.PagedResult;
import ru.yandex.market.crm.core.domain.ReactTableRequest;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;
import ru.yandex.market.crm.core.services.segments.SegmentsDAOImpl;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class SegmentGroupControllerTest extends AbstractControllerMediumTest {

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private SegmentService segmentService;

    @Inject
    private SegmentGroupDAO segmentGroupDAO;

    @Inject
    private SegmentsDAOImpl segmentDAO;

    @Test
    public void testAddGroup() throws Exception {
        var groups = segmentGroupDAO.listAll();
        var initialSize = groups.size();

        String groupName = "testGroup";
        requestCreateGroup(groupName).andExpect(status().isOk());
        groups = segmentGroupDAO.listAll();

        Assertions.assertEquals(initialSize + 1, groups.size());
        Assertions.assertEquals(groupName, groups.get(0).getName());
    }

    @Test
    public void testUpdateGroup() throws Exception {
        var groups = segmentGroupDAO.listAll();
        var initialSize = groups.size();

        String groupName = "testGroup";
        requestCreateGroup(groupName).andExpect(status().isOk());

        groups = segmentGroupDAO.listAll();
        var group = groups.get(0);
        String newName = "newName";
        group.setName(newName);

        mockMvc.perform(put("/api/segment_groups/{id}", group.getId())
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(group)))
                .andDo(print()).andExpect(status().isOk()).andReturn();

        groups = segmentGroupDAO.listAll();

        Assertions.assertEquals(initialSize + 1, groups.size());
        Assertions.assertEquals(newName, groups.get(0).getName());
        Assertions.assertEquals(group.getId(), groups.get(0).getId());
    }

    @Test
    public void testDeleteGroup() throws Exception {
        var groups = segmentGroupDAO.listAll();
        var initialSize = groups.size();

        String groupName = "testGroup";
        requestCreateGroup(groupName).andExpect(status().isOk());

        groups = segmentGroupDAO.listAll();
        var group = groups.get(0);
        String newName = "newName";
        group.setName(newName);
        mockMvc.perform(delete("/api/segment_groups/{id}", group.getId()))
                .andExpect(status().isOk());

        groups = segmentGroupDAO.listAll();

        Assertions.assertEquals(initialSize, groups.size());
    }

    @Test
    public void testGetListSegmentGroups() throws Exception {
        var groupList = new ArrayList<SegmentGroup>();
        int groupCount = 10;
        for (long i = 1; i <= groupCount; i++) {
            var group = jsonDeserializer
                    .readObject(new TypeReference<SegmentGroup>() {},
                            requestCreateGroup("test_group_" + i)
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString()
                    );
            groupList.add(group);
        }

        for (long i = 0; i < groupCount / 2; i++) {
            Segment segment = new Segment("segment_" + i);
            segment.setName("Segment - " + i + " name");
            segment.setConfig(new SegmentGroupPart());

            var currentGroup = groupList.get((int) i * 2);
            segment.setGroupId(currentGroup.getId());
            currentGroup.setHasSegments(true);

            segmentDAO.createOrUpdateSegment(segment);
        }

        var result = mockMvc.perform(get("/api/segment_groups"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        var segmentGroups = jsonDeserializer.readObject(
                new TypeReference<List<SegmentGroup>>() {},
                result.getResponse().getContentAsString());

        Assertions.assertEquals(segmentGroups.size(), groupCount);

        for (int i = 0; i < groupCount; i++) {
            var actualGroup = segmentGroups.get(i);
            var expectedGroup = groupList.get(groupCount - i - 1);

            Assertions.assertEquals(expectedGroup.getId(), actualGroup.getId());
            Assertions.assertEquals(expectedGroup.getName(), actualGroup.getName());
            Assertions.assertEquals(expectedGroup.isHasSegments(), actualGroup.isHasSegments());
        }
    }

    @Test
    public void testGetListSegmentsOfGroup() throws Exception {
        var segmentCount1 = 5;
        var segmentCount2 = 10;
        SegmentGroup group1 = new SegmentGroup();
        group1.setName("test_group_1");
        group1.setId(1);

        SegmentGroup group2 = new SegmentGroup();
        group2.setName("test_group_2");
        group2.setId(2);

        group1 = jsonDeserializer.readObject(new TypeReference<>() {},
                requestCreateGroup(group1.getName()).andReturn().getResponse().getContentAsString());
        group2 = jsonDeserializer.readObject(new TypeReference<>() {},
                requestCreateGroup(group2.getName()).andReturn().getResponse().getContentAsString());

        createSegmentsInGroup(segmentCount1, group1, 100);

        createSegmentsInGroup(segmentCount2, group2, 200);

        var mvcResult = mockMvc.perform(post("/api/segment_groups/" + group1.getId() + "/list_segments")
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(new ReactTableRequest())))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        var result = jsonDeserializer.readObject(
                new TypeReference<PagedResult<SegmentDto>>() {},
                mvcResult.getResponse().getContentAsString());

        Assertions.assertEquals(segmentCount1, result.getPageInfo().getElementCount());

        for (var segment : result.getElements()) {
            Assertions.assertEquals(group1.getId(), segment.getGroup().getId());
        }
    }

    @Test
    public void testGetFiltered() throws Exception {
        var segmentCount1 = 5;
        var segmentCount2 = 20;
        SegmentGroup group1 = new SegmentGroup();
        group1.setName("group - 10");
        group1.setId(1);

        SegmentGroup group2 = new SegmentGroup();
        group2.setName("group_20");
        group2.setId(2);

        group1 = jsonDeserializer.readObject(new TypeReference<>() {},
                requestCreateGroup(group1.getName()).andReturn().getResponse().getContentAsString());
        group2 = jsonDeserializer.readObject(new TypeReference<>() {},
                requestCreateGroup(group2.getName()).andReturn().getResponse().getContentAsString());

        createSegmentsInGroup(segmentCount1, group1, 200);

        createSegmentsInGroup(segmentCount2, group2, 100);

        var mvcResult = mockMvc.perform(get("/api/segment_groups/segments?linking_mode=DIRECT_ONLY&name_part=- 10"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        var result = jsonDeserializer.readObject(
                new TypeReference<List<GroupNode<SegmentNode>>>() {},
                mvcResult.getResponse().getContentAsString());

        for (var group : result) {
            Assertions.assertTrue(
                    group.getName().contains("- 10") ||
                            group.getChildren().stream()
                                    .allMatch(segment -> segment.getName().contains("- 10"))
            );
        }
    }

    private void createSegmentsInGroup(int segmentCount2, SegmentGroup group, int startId) {
        for (long i = 1; i <= segmentCount2; i++) {
            Segment segment = new Segment("segment_" + i + startId);
            segment.setName("Segment - " + i + " name");
            segment.setConfig(new SegmentGroupPart());
            segment.setGroupId(group.getId());
            segmentDAO.createOrUpdateSegment(segment);
        }
    }

    @Nonnull
    private ResultActions requestCreateGroup(String groupName) throws Exception {
        var group = new SegmentGroup();
        group.setName(groupName);

        return mockMvc.perform(post("/api/segment_groups/")
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(group)))
                .andDo(print());
    }
}
