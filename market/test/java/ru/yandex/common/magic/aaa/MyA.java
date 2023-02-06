/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 23.03.2007
 * Time: 16:56:21
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.aaa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ashevenkov
 */
public class MyA implements AOperations {

    private AInfo aInfo = new AInfo();
    private List<Long> childs = new LinkedList<Long>();
    private Map<Long, AInfo> ops = new HashMap<Long, AInfo>();

    public MyA() {
    }

    public MyA(AInfo aInfo, Map<Long, AInfo> ops) {
        this.aInfo = aInfo;
        this.ops = ops;
    }

    public void setId(long id) {
        aInfo.setId(id);
    }

    public long getId() {
        return aInfo.getId();
    }

    public Set<? extends AOperations> getChilds() {
        Set<MyA> result = new HashSet<MyA>();
        for (Long child : childs) {
            AInfo info = ops.get(child);
            MyA myA = new MyA(info, ops);
            result.add(myA);
        }
        return result;
    }

    public void setChilds(Set<? extends AOperations> operationses) {
        childs.clear();
        for (AOperations operationse : operationses) {
            long opId = operationse.getId();
            childs.add(opId);
            if(!ops.keySet().contains(opId)) {
                ops.put(opId, new AInfo(opId));
            }
        }
    }
}
