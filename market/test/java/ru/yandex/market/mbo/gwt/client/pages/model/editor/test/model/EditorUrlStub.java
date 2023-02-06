package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditorUrl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author s-ermakov
 */
public class EditorUrlStub extends EditorUrl {

    private EditorUrlStub(String anchor, String parameters) {
        super(anchor, parameters);
    }

    @Override
    protected String decodeUrlPath(String path) {
        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static EditorUrlStub of(String anchor, String parameters) {
        return new EditorUrlStub(anchor, parameters);
    }
}
