package ru.yandex.market.crm.campaign.yt.utils;

import java.util.Collections;
import java.util.Comparator;

import com.google.common.collect.Ordering;

import ru.yandex.market.crm.mapreduce.domain.ImageLink;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BannerBlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.ModelBlockData;
import ru.yandex.market.crm.util.CrmCollections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author dimkarp93
 */
public class SubscriptionDataEqualsAsserter {

    private static final SubscriptionBlockComparator SUBSCRIPTION_BLOCK_COMPARATOR = new SubscriptionBlockComparator();

    public static void assertSubscription(CampaignUserData expected, CampaignUserData actual) {
        assertEquals("subscription data must have same blocks count", expected.getBlocks().size(),
                actual.getBlocks().size());

        Collections.sort(expected.getBlocks(), SUBSCRIPTION_BLOCK_COMPARATOR);
        Collections.sort(actual.getBlocks(), SUBSCRIPTION_BLOCK_COMPARATOR);

        CrmCollections.zip(expected.getBlocks(), actual.getBlocks(), SubscriptionDataEqualsAsserter::assertBlock);
    }

    private static void assertBlock(BlockData expected, BlockData actual) {
        assertEquals("subscription block must have same type", expected.getType(), actual.getType());
        switch (expected.getType()) {
            case BANNER:
                BannerBlockData pe = (BannerBlockData) expected;
                BannerBlockData pa = (BannerBlockData) actual;
                assertEquals("subscription promo block must have same text", pe.getText(), pa.getText());
                break;
            case MODEL:
                ModelBlockData me = (ModelBlockData) expected;
                ModelBlockData ma = (ModelBlockData) actual;

                assertEquals("subscription model block must have same models count", me.getModels().size(),
                        ma.getModels().size());

                Collections.sort(me.getModels(), Comparator.comparing(ModelInfo::getId));
                Collections.sort(ma.getModels(), Comparator.comparing(ModelInfo::getId));

                CrmCollections.zip(me.getModels(),
                        ma.getModels(),
                        SubscriptionDataEqualsAsserter::assertModelInfo);

                assertImageLink(me.getBanner(), ma.getBanner());
                break;
            default:
                throw new UnsupportedOperationException(expected.getType().name());
        }
    }

    private static void assertImageLink(ImageLink expected, ImageLink actual) {
        assertFalse("image link must both be null or not null", expected == null ^ actual == null);

        if (expected == null && actual == null) {
            return;
        }

        assertEquals("image link must have same img", expected.getImg(), actual.getImg());
        assertEquals("image link must have same url", expected.getLink(), actual.getLink());
        assertEquals("image link must have same alt", expected.getAlt(), actual.getAlt());
    }

    private static void assertModelInfo(ModelInfo expected, ModelInfo actual) {
        assertEquals("model info must have same model id", expected.getId(), actual.getId());
        assertEquals("model info must have same hid", expected.getHid(), actual.getHid());
    }

    private static final class SubscriptionBlockComparator implements Comparator<BlockData> {
        private static final Comparator<BlockData> TYPE_COMPARATOR =
                Comparator.comparing(sb -> sb.getType().ordinal());

        private static final Comparator<BlockData> BLOCK_CONTENT_COMPARATOR = (a, b) -> {
            if (a instanceof BannerBlockData && b instanceof BannerBlockData) {
                BannerBlockData pa = (BannerBlockData) a;
                BannerBlockData pb = (BannerBlockData) b;
                return pa.getText().compareTo(pb.getText());
            } else if (a instanceof ModelBlockData && b instanceof ModelBlockData) {
                ModelBlockData ma = (ModelBlockData) a;
                ModelBlockData mb = (ModelBlockData) b;
                return ma.getId().compareTo(mb.getId());
            } else {
                throw new IllegalArgumentException("Impossible. Something went wrong");
            }
        };


        @Override
        public int compare(BlockData o1, BlockData o2) {
            return Ordering.from(TYPE_COMPARATOR).compound(BLOCK_CONTENT_COMPARATOR).compare(o1, o2);
        }
    }
}
