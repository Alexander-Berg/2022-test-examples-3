/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 06.06.2006</p>
 * <p>Time: 13:45:37</p>
 */
package ru.yandex.common.framework.xml;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.ServantInfo;
import ru.yandex.common.framework.core.XmlBuilder;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class XmlBuilderTest extends TestCase {
    private List<Object> data = new ArrayList<Object>();
    private List<ErrorInfo> errors = new ArrayList<ErrorInfo>();
    private ServantInfo servantInfo = new MockServantInfo();
    public static final int DATA_SIZE = 1000;

    protected void setUp() throws Exception {
        super.setUp();
        data.clear();
    }

    private void addData() {
        for (int i = 0; i < DATA_SIZE; i++) {
            data.add(new MockObject("test" + i, i * 1000000l, i * Math.random(),
                    "description description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description " + i));
        }
    }

    private void addFastData() {
        for (int i = 0; i < DATA_SIZE; i++) {
            data.add(new XmlMockObject("test" + i, i * 1000000l, i * Math.random(),
                    "description description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description description description description " +
                            "description description " + i));
        }
    }

    public void testStringXmlBuilder() throws Exception {
        addData();
        AbstractXmlBuilder builder = new StringXmlBuilder();
        builder.setServantInfo(servantInfo);
        process(builder);
    }

    public void testFastStringXmlBuilder() throws Exception {
        addFastData();
        AbstractXmlBuilder builder = new StringXmlBuilder();
        builder.setServantInfo(servantInfo);
        process(builder);
    }

    private String process(final XmlBuilder builder) {
        long st = System.currentTimeMillis();
        final String s = builder.build(data, errors);
        System.out.println("Processing time (" + builder.getClass().getSimpleName() +
                ") (" + s.length() +
                ") is " + (System.currentTimeMillis() - st) + " ms");
        return s;
    }

}
