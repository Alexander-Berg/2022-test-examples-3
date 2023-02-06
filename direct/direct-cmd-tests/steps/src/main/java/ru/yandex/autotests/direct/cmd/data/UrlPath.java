package ru.yandex.autotests.direct.cmd.data;

public enum UrlPath {
    WELCOME_PAGE("/dna/welcome/");

    private String urlPath;


    UrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public String toString() {
        return urlPath;
    }
}
