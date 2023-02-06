/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 16.06.2006</p>
 * <p>Time: 11:32:41</p>
 */
package ru.yandex.common.framework.xml;

import ru.yandex.common.util.xml.XmlConvertable;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class XmlMockObject extends MockObject implements XmlConvertable {
    public XmlMockObject(
            final String name, final long age, final double weight, final String description) {
        super(name, age, weight, description);
    }

    public void toXml(StringBuilder result) {
        result.append("<mock-object weight=\"")
                .append(getWeight())
                .append("\" age=\"")
                .append(getAge())
                .append("\">")
                .append("<name>")
                .append(getName())
                .append("</name>")
                .append("<description>")
                .append(getDescription())
                .append("</description>")
                .append("</mock-object>");
    }
}
