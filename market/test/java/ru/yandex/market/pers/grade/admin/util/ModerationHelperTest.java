package ru.yandex.market.pers.grade.admin.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.admin.Const;
import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.grade.client.model.ModState;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModerationHelperTest extends MockedPersGradeAdminTest {
    @Autowired
    private ModerationHelper moderationHelper;

    @Test
    public void testOneRejectedBecomeApproved() {
        HttpServletRequest req = mockRequest();
        addReply(req, "66", ModState.REJECTED, ModState.APPROVED);
        Map<ModState, Collection<Long>> parsed = moderationHelper.parseParams(req);
        Assert.assertEquals(1, parsed.size());
        assertReplies(parsed, ModState.APPROVED, 66L);
    }

    @Test(expected = AssertionError.class)
    public void testThatBreakable() {
        HttpServletRequest req = mockRequest();
        addReply(req, "66", ModState.REJECTED, ModState.APPROVED);
        Map<ModState, Collection<Long>> parsed = moderationHelper.parseParams(req);
        assertReplies(parsed, // assertion here
            ModState.APPROVED);
    }

    @Test
    public void testRejectedWasLeft() {
        HttpServletRequest req = mockRequest();
        addReply(req, "66", ModState.REJECTED, ModState.REJECTED);
        Map<ModState, Collection<Long>> parsed = moderationHelper.parseParams(req);
        Assert.assertTrue(parsed.isEmpty());
    }

    @Test
    public void testAllByOne() {
        HttpServletRequest req = mockRequest();
        addReply(req, "66", ModState.APPROVED, ModState.REJECTED);
        addReply(req, "77", ModState.REJECTED, ModState.DELAYED);
        addReply(req, "88", ModState.DELAYED, ModState.REJECTED_BY_SHOP_CLAIM);
        addReply(req, "99", ModState.REJECTED_BY_SHOP_CLAIM, ModState.APPROVED);
        Map<ModState, Collection<Long>> parsed = moderationHelper.parseParams(req);
        Assert.assertEquals(4, parsed.size());
        assertReplies(parsed, ModState.APPROVED, 99L);
        assertReplies(parsed, ModState.REJECTED, 66L);
        assertReplies(parsed, ModState.DELAYED, 77L);
        assertReplies(parsed, ModState.REJECTED_BY_SHOP_CLAIM, 88L);
    }

    @Test
    public void testAllByTwoTimes() {
        HttpServletRequest req = mockRequest();
        addReply(req, "66", ModState.APPROVED, ModState.REJECTED);
        addReply(req, "666", ModState.APPROVED, ModState.REJECTED);
        addReply(req, "066", ModState.APPROVED, ModState.APPROVED);// should be killed

        addReply(req, "77", ModState.REJECTED, ModState.DELAYED);
        addReply(req, "777", ModState.REJECTED, ModState.DELAYED);
        addReply(req, "077", ModState.REJECTED, ModState.REJECTED);// should be killed

        addReply(req, "88", ModState.DELAYED, ModState.REJECTED_BY_SHOP_CLAIM);
        addReply(req, "888", ModState.DELAYED, ModState.REJECTED_BY_SHOP_CLAIM);
        addReply(req, "088", ModState.DELAYED, ModState.DELAYED);// should be killed

        addReply(req, "99", ModState.REJECTED_BY_SHOP_CLAIM, ModState.APPROVED);
        addReply(req, "999", ModState.REJECTED_BY_SHOP_CLAIM, ModState.APPROVED);
        addReply(req, "099", ModState.REJECTED_BY_SHOP_CLAIM, ModState.REJECTED_BY_SHOP_CLAIM);// should be killed

        Map<ModState, Collection<Long>> parsed = moderationHelper.parseParams(req);
        Assert.assertEquals(4, parsed.size());
        assertReplies(parsed, ModState.APPROVED, 99L, 999L);
        assertReplies(parsed, ModState.REJECTED, 66L, 666L);
        assertReplies(parsed, ModState.DELAYED, 77L, 777L);
        assertReplies(parsed, ModState.REJECTED_BY_SHOP_CLAIM, 88L, 888L);
    }

    private void assertReplies(Map<ModState, Collection<Long>> parsed, ModState modState, Long... replyIds) {
        Collection<Long> a = parsed.get(modState);
        List<Long> b = Arrays.asList(replyIds);
        Assert.assertTrue("expected " + b + " but actual " + a,
            CollectionUtils.isEqualCollection(
                a, b));
    }

    private void addReply(HttpServletRequest req, String gradeId, ModState old, ModState newState) {
        String radioValue = (old == ModState.APPROVED ? Const.GRADE_ID_APPROVED_PARAM
            : (old == ModState.REJECTED ? Const.GRADE_ID_REJECTED_PARAM
            : (old == ModState.REJECTED_BY_SHOP_CLAIM ? Const.GRADE_ID_REJECTED_BY_SHOP_PARAM :
            (old == ModState.DELAYED ? Const.GRADE_ID_DELAYED_PARAM : null))));
        String paramName = (newState == ModState.APPROVED ? Const.APPROVED
            : (newState == ModState.REJECTED ? Const.REJECTED
            : (newState == ModState.REJECTED_BY_SHOP_CLAIM ? Const.REJECTED_BY_SHOP
            : (newState == ModState.DELAYED ? Const.DELAYED
            : null))));

        mockParam(req, Const.GRADE_ID_PARAM, gradeId);
        mockParam(req, radioValue, gradeId);
        mockParam(req, gradeId, paramName);
    }

    private HttpServletRequest mockRequest() {
        HttpServletRequest mock = mock(HttpServletRequest.class);
        HashMap<String, String[]> parameters = new HashMap<>();
        when(mock.getParameterMap()).thenReturn(parameters);
        when(mock.getParameter(anyString())).then(inv -> {
            String key = inv.getArgument(0);
            return parameters.containsKey(key) ? parameters.get(key)[0] : null;
        });
        when(mock.getParameterValues(anyString())).then(inv -> {
            String key = inv.getArgument(0);
            return parameters.get(key);
        });
        return mock;
    }

    private void mockParam(HttpServletRequest req, String key, String value) {
        if (key == null || value == null) {
            return;
        }
        Map<String, String[]> map = req.getParameterMap();
        if (!map.containsKey(key)) {
            map.put(key, new String[]{value});
        } else {
            String[] current = map.get(key);
            List<String> res = new ArrayList<>(Arrays.asList(current));
            res.add(value);
            map.put(key, res.toArray(new String[0]));
        }
    }
}
