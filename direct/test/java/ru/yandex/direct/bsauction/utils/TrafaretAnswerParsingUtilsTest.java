package ru.yandex.direct.bsauction.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import ru.yandex.yabs.server.proto.rank.TTrafaretRankAnswer;

import static org.assertj.core.api.Assertions.assertThat;

public class TrafaretAnswerParsingUtilsTest {

    @Test
    public void parseJson_success() throws InvalidProtocolBufferException {
        TTrafaretRankAnswer actual = TrafaretAnswerParsingUtils.parseJson(
                "{\"Targets\":[{\"TargetID\":1,\"Clickometer\":[{\"TrafaretID\":8,\"Position\":0,\"X\":1000000,\"Bid\":4198700020739,\"Cpc\":731019385349},{\"TrafaretID\":1,\"Position\":0,\"X\":826000,\"Bid\":539445206,\"Cpc\":539445206},{\"TrafaretID\":-1,\"Position\":0,\"X\":1000000,\"Bid\":192438500,\"Cpc\":78892500},{\"TrafaretID\":-1,\"Position\":1,\"X\":742160,\"Bid\":19100400,\"Cpc\":19100400},{\"TrafaretID\":-1,\"Position\":2,\"X\":656773,\"Bid\":19100400,\"Cpc\":19100400},{\"TrafaretID\":-1,\"Position\":3,\"X\":610544,\"Bid\":19100400,\"Cpc\":19100400}],\"LegacyFields\":{\"PlaceID\":0,\"BannerID\":0,\"PhraseID\":0,\"Price\":300000,\"MinPrice\":300000,\"ContextStopFlag\":0,\"TragicContextFlag\":0,\"SumECtr\":24400,\"ECtr\":616,\"PremiumECtr\":13690,\"RightShows\":0,\"RightClicks\":0,\"PremiumShows\":0,\"PremiumClicks\":0,\"MaxCPMInRight\":4407}}]}");
        assertThat(actual.getTargets(0).getUnknownFields().asMap())
                .describedAs("There are no support of unknownFields in proto3 used in Arcadia")
                .isEmpty();
    }
}
