package ru.yandex.mail.tests.sendbernar.models;


import ru.yandex.mail.tests.mbody.generated.NarodTransformerResult;

import java.io.IOException;

import static java.net.URLDecoder.decode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.mail.common.utils.ClassPath.fromClasspath;


public class DiskAttachHelper {
    private String link;
    private String dataPreview;
    private String name;
    private String size;
    private String html;

    public DiskAttachHelper() throws IOException {
        link = "https://yadi.sk/i/zd5aNOgOweZoU";
        dataPreview = new StringBuilder("https://downloader.disk.yandex.ru")
                .append("/preview/d3f2b66a2f2aa9fec15d3e6d5e7f5be466ba0304d774b552657e829ca2288150/inf")
                .append("P1Uu3IBvn2J_059WhlC9XydbkJ2sC_KEk2e_VtbYAOfuuRRO_ACnqYduWLfxvvz5VgHzs7xBXDvk4Qqe7B86gQ%3D%3D")
                .append("uid=0&filename=%D0%93%D0%BE%D1%80%D1%8B.jpg&disposition=inline&hash=&")
                .append("limit=0&content_type=image%2Fjpeg&tknv=v2&size=S&crop=0").toString();
        name = "Горы.jpg";
        size = "1762478";
        html = String.format(fromClasspath("inline/disk_attach.html"), link, dataPreview, name, size);
    }

    public String getHtml() {
        return html;
    }

    public void assertAttachInMessageIsSame(Message message) throws IOException {
        NarodTransformerResult result = message.getAttachments().get(0).getNarodTransformerResult().get(0);

        assertThat(result.getPartClassInfo().getPartClass(), equalTo("image"));
        assertThat(result.getSizeDescription(), equalTo(size));
        assertThat(result.getPreview(), equalTo(decode(dataPreview, "UTF-8")));
        assertThat(result.getUrl(), equalTo(link));
    }
}
