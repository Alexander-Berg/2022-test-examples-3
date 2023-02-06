package ru.yandex.market.yt.bidding;

import java.time.LocalDateTime;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер для {@link AuctionGenerationInfo}
 */
public class AuctionGenerationInfoMatcher {
    public static Matcher<AuctionGenerationInfo> hasGenerationId(long expectedValue) {
        return MbiMatchers.<AuctionGenerationInfo>newAllOfBuilder()
            .add(AuctionGenerationInfo::getGenerationId, expectedValue, "generationId")
            .build();
    }

    public static Matcher<AuctionGenerationInfo> hasYtCluster(String expectedValue) {
        return MbiMatchers.<AuctionGenerationInfo>newAllOfBuilder()
            .add(AuctionGenerationInfo::getYtCluster, expectedValue, "ytCluster")
            .build();
    }

    public static Matcher<AuctionGenerationInfo> hasGenerationName(String expectedValue) {
        return MbiMatchers.<AuctionGenerationInfo>newAllOfBuilder()
            .add(AuctionGenerationInfo::getGenerationName, expectedValue, "generationName")
            .build();
    }
    
    public static Matcher<AuctionGenerationInfo> hasPublishDateTime(LocalDateTime expectedValue) {
        return MbiMatchers.<AuctionGenerationInfo>newAllOfBuilder()
            .add(AuctionGenerationInfo::getPublishDateTime, expectedValue, "publishDateTime")
            .build();
    }
}
