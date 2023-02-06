package ru.yandex.autotests.direct.utils.beans;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class ParentBean {
    private String field1;
    private String field2;
    private ChildBean childBean;

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public ChildBean getChildBean() {
        return childBean;
    }

    public void setChildBean(ChildBean childBean) {
        this.childBean = childBean;
    }
}
