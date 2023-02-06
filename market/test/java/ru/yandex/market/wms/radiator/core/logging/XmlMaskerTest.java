package ru.yandex.market.wms.radiator.core.logging;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class XmlMaskerTest {

    @Test
    void maskSingleTextXmlElement() {

        assertThat(
                XmlMasker.maskSingleTextXmlElement("<foo></foo>", "bar"), is(equalTo("<foo></foo>"))
        );

        assertThat(
                XmlMasker.maskSingleTextXmlElement(
                        "<foo><token>secret</token></foo>", "token"),
                is(equalTo("<foo><token>[censored]</token></foo>"))
        );
    }

}
