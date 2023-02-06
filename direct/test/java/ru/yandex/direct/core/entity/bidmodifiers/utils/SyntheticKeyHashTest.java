package ru.yandex.direct.core.entity.bidmodifiers.utils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;

import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.mapper.Common.computeSyntheticKeyHash;

@RunWith(Parameterized.class)
public class SyntheticKeyHashTest {

    @Parameterized.Parameter
    public BidModifierType type;

    @Parameterized.Parameter(1)
    public long campaignId;

    @Parameterized.Parameter(2)
    @Nullable
    public Long adGroupId;

    @Parameterized.Parameter(3)
    public String expectedHash;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BidModifierType.MOBILE_MULTIPLIER, 13422150, null, "14975775456974624117"},
                {BidModifierType.RETARGETING_MULTIPLIER, 13969838, null, "7337111261210466263"},
                {BidModifierType.DEMOGRAPHY_MULTIPLIER, 9704219, null, "4840167121742221105"},
                {BidModifierType.GEO_MULTIPLIER, 28473925, null, "1604922045903589745"},

                {BidModifierType.MOBILE_MULTIPLIER, 10865835, 774683372L, "6407125620674637013"},
                {BidModifierType.RETARGETING_MULTIPLIER, 14264764, 890064744L, "4294131534664951487"},
                {BidModifierType.DEMOGRAPHY_MULTIPLIER, 14209184, 890024945L, "10863870774484340599"},
        });
    }

    @Test
    public void testSyntheticKeyHash() {
        Assert.assertThat(computeSyntheticKeyHash(type, campaignId, adGroupId), is(new BigInteger(expectedHash)));
    }
}
