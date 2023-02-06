package ru.yandex.direct.api.v5.ws.testbeans;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "SimpleFieldNamesEnumXML")
@XmlEnum
public enum SimpleFieldNamesEnum {
    @XmlEnumValue("Id")
    ID("Id"),
    @XmlEnumValue("Name")
    NAME("Name");
    protected final String value;

    SimpleFieldNamesEnum(String v) {
        value = v;
    }
}
