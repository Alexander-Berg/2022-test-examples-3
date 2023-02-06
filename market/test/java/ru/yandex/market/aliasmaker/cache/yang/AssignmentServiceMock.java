package ru.yandex.market.aliasmaker.cache.yang;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.aliasmaker.models.AssignmentInfo;

/**
 * @author shadoff
 * created on 12/15/20
 */
public class AssignmentServiceMock extends AssignmentServiceImpl {
    private Map<String, AssignmentInfo> infoMap;
    private boolean isProd = false;

    public AssignmentServiceMock() {
        super(null, null, null);
        infoMap = new HashMap<>();
    }

    @Override
    protected AssignmentInfo callYangApi(String assignmentId) {
        return infoMap.get(assignmentId);
    }

    public void addAssignmentInfo(String id, AssignmentInfo assignmentInfo) {
        infoMap.put(id, assignmentInfo);
    }

    public void addDefaultAssignmentInfo(String id, String workerId) {
        AssignmentInfo assignmentInfo = new AssignmentInfo(workerId, "ACTIVE", null);
        infoMap.put(id, assignmentInfo);
    }

    @Override
    public boolean isProd() {
        return isProd;
    }

    public void setProd(boolean prod) {
        isProd = prod;
    }
}
