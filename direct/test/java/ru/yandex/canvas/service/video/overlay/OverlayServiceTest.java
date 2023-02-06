package ru.yandex.canvas.service.video.overlay;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.canvas.service.video.overlay.OverlayService.applyVastTemplate;

public class OverlayServiceTest {
    @Test
    public void applyVastTemplateTest() {
        Assert.assertEquals("http://cdn.com/file.js",
                applyVastTemplate("{{{file.js}}}",
                        ImmutableMap.of("file.js", "http://cdn.com/file.js")));

        Assert.assertEquals("<XML>%%{}{}http://cdn.com/file.js{}{}</XML>",
                applyVastTemplate("<XML>%%{}{}{{{file.js}}}{}{}</XML>",
                        ImmutableMap.of("file.js", "http://cdn.com/file.js")));

        Assert.assertEquals("first_link second_link nested_link",
                applyVastTemplate("{{{js}}} {{{my.second.js}}} {{{a.b.c.d.e.f.g.h}}}",
                        ImmutableMap.of(
                                "js", "first_link",
                                "my.second.js", "second_link",
                                "a.b.c.d.e.f.g.h", "nested_link"
                        )));
    }
}
