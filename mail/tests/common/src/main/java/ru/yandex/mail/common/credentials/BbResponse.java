package ru.yandex.mail.common.credentials;

import java.io.IOException;

import org.apache.commons.io.IOUtils;


public class BbResponse {
    private String response;

    private BbResponse(String response) {
        this.response = response;
    }

    public static BbResponse from(String path) {
        try {
            return new BbResponse(IOUtils.toString(BbResponse.class.getClassLoader().getResourceAsStream(path)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return response;
    }
}
