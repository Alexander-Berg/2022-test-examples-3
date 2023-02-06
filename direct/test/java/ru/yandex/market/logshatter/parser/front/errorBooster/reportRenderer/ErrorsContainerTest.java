package ru.yandex.market.logshatter.parser.front.errorBooster.reportRenderer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ErrorsContainerTest {

    @Test
    public void prepareStacktrace() {
        ErrorsContainer errorsContainer = new ErrorsContainer();

        assertEquals(
            "at Object.blocks.content_type_search (/images_touch_phone/pages-touch/common/common.renderer.js:37361:30)",
            errorsContainer.prepareStacktrace("at Object.blocks.content_type_search (/place/db/iss3/instances/14155_renderer_imgs-serp_vla_JxJH4ILJwsH/templates/YxWeb/images_touch_phone/pages-touch/common/common.renderer.js:37361:30)")
        );

        assertEquals(
            "at Object.contextPos (/templates-web4/.build/src/lib/Context/index.js:6:66)",
            errorsContainer.prepareStacktrace("at Object.contextPos (/place/db/iss3/instances/ns2uqv3qf57jmnre_production_report_renderer_hamster_web_dCIKIjtpTIU/templates-bstr/1551782287/templates-web4/.build/src/lib/Context/index.js:6:66)")
        );

        assertEquals(
            "Object.getThumbUrl (/video3/node_modules/video/blocks-common/thumb-url/thumb-url.vanilla-common.js:162:15",
            errorsContainer.prepareStacktrace("Object.getThumbUrl (/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_YaghW4YOV1R/templates/YxWeb/video3/node_modules/video/blocks-common/thumb-url/thumb-url.vanilla-common.js:162:15")
        );

        assertEquals(
            "Object.blocks.adapter-entity-card__items (/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js:29874:74)",
            errorsContainer.prepareStacktrace("Object.blocks.adapter-entity-card__items (/place/db/iss3/instances/odi2jad3ae2lfgpb_production_report_renderer_hamster_web_jLa5ByD6qHE/templates-bstr/1551782312/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js:29874:74)")
        );
    }

    @Test
    public void prepareRawMessage() {
        ErrorsContainer errorsContainer = new ErrorsContainer();

        assertEquals(
            "70%;o",
            errorsContainer.prepareRawMessage("70%;o")
        );

        assertEquals(
            "==",
            errorsContainer.prepareRawMessage("\n\n==\n\n")
        );

        assertEquals(
            "https://yandex.ru/?search=привет как дела",
            errorsContainer.prepareRawMessage("https://yandex.ru/?search=привет%20как%20дела")
        );
    }
}
