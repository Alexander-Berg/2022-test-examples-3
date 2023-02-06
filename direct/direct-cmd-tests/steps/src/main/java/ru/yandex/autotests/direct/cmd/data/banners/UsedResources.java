package ru.yandex.autotests.direct.cmd.data.banners;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UsedResources {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("urls")
    private List<URL> urls;

    private class URL {
        @SerializedName("urformatls")
        private String format;

        @SerializedName("url")
        private String url;

        public String getFormat() {
            return format;
        }

        public URL withFormat(String format) {
            this.format = format;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public URL withUrl(String url) {
            this.url = url;
            return this;
        }
    }

    public Long getId() {
        return id;
    }

    public UsedResources withId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public UsedResources withName(String name) {
        this.name = name;
        return this;
    }

    public List<URL> getUrls() {
        return urls;
    }

    public UsedResources withUrls(List<URL> urls) {
        this.urls = urls;
        return this;
    }
}
