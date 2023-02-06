package ru.yandex.market.billing.tasks.distribution;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.distribution.share.model.DistributionPartner;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributionReportResponseParserTest {
    private static final long CLID1 = 2496765;
    private static final long CLID2 = 2487083;

    @Test
    public void testParse() throws Exception {
        DistributionClient.Response response = new ObjectMapper().readerFor(DistributionClient.Response.class)
                .readValue(this.getClass().getResource("distributionReportResponse.json"));
        var result = new DistributionReportResponseParser().parseResult(response);
        assertThat(result).hasSize(2);
        var clid1O = result.stream()
                .filter(p -> p.getClid() == CLID1).findFirst()
                .map(DistributionPartner.Builder::build);
        var clid2O = result.stream()
                .filter(p -> p.getClid() == CLID2).findFirst()
                .map(DistributionPartner.Builder::build);
        assertThat(clid1O).isNotEmpty();
        assertThat(clid2O).isNotEmpty();
        assertThat(clid1O.get()).isEqualTo(
                DistributionPartner.builder()
                        .setClid(CLID1)
                        .setClidTypeId(100021)
                        .setClidType("Промокоды Маркета")
                        .setPayable(true)
                        .setPackId(45947)
                        .setPackDomain("t.me/marketaffchat")
                        .setPackComment("")
                        .setPackCreateDate(LocalDate.of(2021, 9, 8))
                        .setSetId(396514)
                        .setSoftId(1046)
                        .setUserLogin("distr-test-selt-1")
                .build());
        assertThat(clid2O.get()).isEqualTo(
                DistributionPartner.builder()
                        .setClid(CLID2)
                        .setClidTypeId(100021)
                        .setClidType("Промокоды Маркета")
                        .setPayable(true)
                        .setPackId(43442)
                        .setPackDomain("marketaff.ru")
                        .setPackComment("")
                        .setPackCreateDate(LocalDate.of(2021, 8, 6))
                        .setSetId(393866)
                        .setSoftId(1046)
                        .setUserLogin("gd-di-test")
                        .build());
    }

}