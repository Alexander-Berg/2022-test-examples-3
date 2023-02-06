package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundDetailsInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class OutboundDetailsInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private OutboundDetailsInfoRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/outbound_info_setup.xml")
    void findByOutboundId() {
        List<OutboundDetailsInfo> found = new ArrayList<>(repository.findByOutboundId("OID1"));

        softly.assertThat(found)
            .as("Asserting the found list size")
            .hasSize(2);

        OutboundDetailsInfo first = found.stream().filter(info -> info.getId().equals(100500L)).findFirst().get();

        softly.assertThat(first.getVendorId())
            .as("Asserting the first item vendor id")
            .isEqualTo(201000);
        softly.assertThat(first.getArticle())
            .as("Asserting the first item article")
            .isEqualTo("THISISARTICLE0");
        softly.assertThat(first.getDeclared())
            .as("Asserting the first item declared")
            .isEqualTo(56);
        softly.assertThat(first.getCreated())
            .as("Asserting the first item created")
            .isEqualTo(LocalDateTime.parse("2018-04-05T18:55:56.430000"));
        softly.assertThat(first.getOutboundId())
            .as("Asserting the first item outbound id")
            .isEqualTo("OID1");

        OutboundDetailsInfo second = found.stream().filter(info -> info.getId().equals(100502L)).findFirst().get();

        softly.assertThat(second.getVendorId())
            .as("Asserting the second item vendor id")
            .isEqualTo(201002);
        softly.assertThat(second.getArticle())
            .as("Asserting the second item article")
            .isEqualTo("THISISARTICLE2");
        softly.assertThat(second.getDeclared())
            .as("Asserting the second item declared")
            .isEqualTo(56);
        softly.assertThat(second.getCreated())
            .as("Asserting the second item created")
            .isEqualTo(LocalDateTime.parse("2018-04-05T18:55:56.430000"));
        softly.assertThat(second.getOutboundId())
            .as("Asserting the second item outbound id")
            .isEqualTo("OID1");
    }
}
