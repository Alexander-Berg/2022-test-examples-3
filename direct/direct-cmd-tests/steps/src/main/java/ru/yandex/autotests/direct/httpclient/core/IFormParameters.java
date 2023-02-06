package ru.yandex.autotests.direct.httpclient.core;

import org.apache.http.NameValuePair;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 27.05.15
 */
public interface IFormParameters {

    public List<NameValuePair> parameters();
}
