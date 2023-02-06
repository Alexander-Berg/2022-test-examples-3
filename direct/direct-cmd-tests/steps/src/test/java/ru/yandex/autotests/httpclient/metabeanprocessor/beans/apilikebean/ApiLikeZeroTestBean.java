package ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean;

import java.util.List;

/**
 * Created by shmykov on 10.02.15.
 */
public class ApiLikeZeroTestBean {

    private List<ApiLikeFirstTestBean> list;

    public List<ApiLikeFirstTestBean> getList() {
        return list;
    }

    public void setList(List<ApiLikeFirstTestBean> list) {
        this.list = list;
    }
}
