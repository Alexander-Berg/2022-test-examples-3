package ru.yandex.direct.intapi.entity.campaigns.allowedpageids;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.intapi.IntApiException;

public class CampaignAllowedPageIdsValidationSizeNegativeTest {
    private CampaignAllowedPageIdsValidation validation = new CampaignAllowedPageIdsValidation();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void pageIdsSizeNegative() {
        List<String> pageIds = IntStream.range(1, 1002).boxed().map(String::valueOf).collect(Collectors.toList());
        thrown.expect(IntApiException.class);
        thrown.expectMessage("{\"code\":\"BAD_PARAM\",\"message\":\"page_ids must contain from 0 to 1000 items\"}");
        validation.checkAndParsePageIds(pageIds);

    }
}
