package ru.yandex.market.api.internal.report.parsers.json.filters;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.ThumbnailSize;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@WithContext
public class PickerParserTest extends BaseTest {
    private PickerParser parser;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new PickerParser();

        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void shouldCorrectUrl() {
        assertThat(
            parse("image-picker.json"),
            is("http://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1732171388-15266392--84c6daccab203dc43a44a5db6c56566a/1")
        );
    }

    @Test
    public void correctDefaultPhotoPickerSize() {
        ContextHolder.update(x -> x.setGenericParams(GenericParams.DEFAULT));
        assertThat(
            parse("image-picker.json"),
            is("http://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1732171388-15266392--84c6daccab203dc43a44a5db6c56566a/1")
        );
    }

    @Test
    public void correctCustomPhotoPickerSize() {
        ContextHolder.update(x -> x.setGenericParams(
            new GenericParamsBuilder()
                .setPhotoPickerSize(ThumbnailSize.W100xH100)
                .build()
            )
        );
        assertThat(
            parse("image-picker.json"),
            is("http://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1732171388-15266392--84c6daccab203dc43a44a5db6c56566a/100x100")
        );
    }

    private String parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
