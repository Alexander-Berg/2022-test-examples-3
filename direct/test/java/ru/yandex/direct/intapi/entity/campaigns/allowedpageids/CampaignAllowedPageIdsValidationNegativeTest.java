package ru.yandex.direct.intapi.entity.campaigns.allowedpageids;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.intapi.IntApiException;

@RunWith(Parameterized.class)
public class CampaignAllowedPageIdsValidationNegativeTest {
    private CampaignAllowedPageIdsValidation validation = new CampaignAllowedPageIdsValidation();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameter
    public List<String> requestIds;

    @Parameterized.Parameter(1)
    public String expectedErrorMessage;

    @Parameterized.Parameters(name = "message {1} ")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {null, "{\"code\":\"BAD_PARAM\",\"message\":\"campaign_ids cannot be null\"}"}, // full message
                {Collections.emptyList(), "campaign_ids must contain from 1 to 10000 items"},
                {Arrays.asList("123", null, "345"), "campaign_ids[1] cannot be null"},
                {Arrays.asList("123", "", "345"), "campaign_ids[1] cannot be empty"},
                {Collections.singletonList("123.4"), "campaign_ids[0] must be whole non-negative number"},
                {Collections.singletonList("0"), "campaign_ids[0] can't be \\\"0\\\""},
                {Collections.singletonList("000"), "campaign_ids[0] can't be \\\"0\\\""},
                {Collections.singletonList("-1"), "campaign_ids[0] must be whole non-negative number"},
                {Collections.singletonList("asdf"), "campaign_ids[0] must be whole non-negative number"},
                {Collections.singletonList("123,4"), "campaign_ids[0] must be whole non-negative number"},
                {Collections.singletonList(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString()),
                        "campaign_ids[0] must be a long value"},
                {Collections.singletonList(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).toString()),
                        "campaign_ids[0] must be whole non-negative number"},
        });
    }

    @Test
    public void listValidation() {
        thrown.expect(IntApiException.class);
        thrown.expectMessage(expectedErrorMessage);
        validation.checkAndParseCampaignIds(requestIds);
    }
}
