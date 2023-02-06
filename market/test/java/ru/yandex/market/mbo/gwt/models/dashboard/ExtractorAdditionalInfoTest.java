package ru.yandex.market.mbo.gwt.models.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ExtractorAdditionalInfoTest {

    @Test
    public void parseEmptyText() {
        ExtractorAdditionalInfo actual = ExtractorAdditionalInfo.parse("");
        Assertions.assertThat(actual)
            .isEqualToComparingFieldByFieldRecursively(new ExtractorAdditionalInfo());
    }

    @Test
    public void testParseSomeText() {
        ExtractorAdditionalInfo actual = ExtractorAdditionalInfo.parse("text");
        Assertions.assertThat(actual).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().setAnotherText("text")
        );
    }

    @Test
    public void testParseYtOperation() {
        String url = ExtractorAdditionalInfo.ytOperation("http://url");
        ExtractorAdditionalInfo actual = ExtractorAdditionalInfo.parse(url);
        Assertions.assertThat(actual).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url")
        );
    }

    @Test
    public void testParseRealYtOperation() {
        String url = "https://hahn.yt.yandex.net/?page=operation&mode=detail&id=54589c33-e35afc8d-3fe03e8-b0cce710";
        String text = ExtractorAdditionalInfo.ytOperation(url);
        ExtractorAdditionalInfo actual = ExtractorAdditionalInfo.parse(text);
        Assertions.assertThat(actual).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl(url)
        );
    }

    @Test
    public void testParseYtOperationWithAnotherSymbols() {
        String url = ExtractorAdditionalInfo.ytOperation("http://url");

        ExtractorAdditionalInfo actual1 = ExtractorAdditionalInfo.parse(url + " ");
        Assertions.assertThat(actual1).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url")
        );
        ExtractorAdditionalInfo actual2 = ExtractorAdditionalInfo.parse(url + "\n");
        Assertions.assertThat(actual2).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url")
        );
        ExtractorAdditionalInfo actual3 = ExtractorAdditionalInfo.parse(url + "\ntest");
        Assertions.assertThat(actual3).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url").setAnotherText("test")
        );
        ExtractorAdditionalInfo actual4 = ExtractorAdditionalInfo.parse(url + " test");
        Assertions.assertThat(actual4).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url").setAnotherText("test")
        );
        ExtractorAdditionalInfo actual5 = ExtractorAdditionalInfo.parse("test" + url);
        Assertions.assertThat(actual5).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url").setAnotherText("test")
        );
        ExtractorAdditionalInfo actual6 = ExtractorAdditionalInfo.parse("test\n" + url);
        Assertions.assertThat(actual6).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url").setAnotherText("test")
        );
        ExtractorAdditionalInfo actual7 = ExtractorAdditionalInfo.parse("test\n" + url + " test");
        Assertions.assertThat(actual7).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url").setAnotherText("test\n test")
        );
    }

    @Test
    public void parseDoubleUrl() {
        String url1 = ExtractorAdditionalInfo.ytOperation("http://url1");
        String url2 = ExtractorAdditionalInfo.ytOperation("http://url2");

        ExtractorAdditionalInfo actual1 = ExtractorAdditionalInfo.parse(url1 + "\n" + url2);
        Assertions.assertThat(actual1).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url1").addYtOperationUrl("http://url2")
        );

        ExtractorAdditionalInfo actual2 = ExtractorAdditionalInfo.parse(url2 + "\ntext\n" + url1);
        Assertions.assertThat(actual2).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url2").addYtOperationUrl("http://url1")
            .setAnotherText("text")
        );

        ExtractorAdditionalInfo actual3 = ExtractorAdditionalInfo.parse("hello\n" + url2 + "\ntext" +
            "\n" + url1 + "\nworld");
        Assertions.assertThat(actual3).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url2").addYtOperationUrl("http://url1")
                .setAnotherText("hello\ntext\nworld")
        );

        ExtractorAdditionalInfo actual4 = ExtractorAdditionalInfo.parse("hello\n" + url2 + "\n" + url1 + "\nworld");
        Assertions.assertThat(actual4).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url2").addYtOperationUrl("http://url1")
                .setAnotherText("hello\n\nworld")
        );

        ExtractorAdditionalInfo actual5 = ExtractorAdditionalInfo.parse(url1 + "\nhello world\n" + url1);
        Assertions.assertThat(actual5).isEqualToComparingFieldByFieldRecursively(
            new ExtractorAdditionalInfo().addYtOperationUrl("http://url1").addYtOperationUrl("http://url1")
                .setAnotherText("hello world")
        );
    }
}
