package ru.yandex.market.loyalty.back.controller.perk;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.perk.TagStat;
import ru.yandex.market.loyalty.back.controller.PerkController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.config.qualifier.Tags;
import ru.yandex.market.loyalty.core.model.tags.TagsMatchResponse;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.tags.TagsClient;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestFor(PerkController.class)
public class PerkControllerTagsTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long UID = 234L;
    private static final long MSK_REGION_ID = 213L;

    @Autowired
    private MarketLoyaltyClient client;
    @Autowired
    private TagsClient tagsClient;
    @Autowired
    @Tags
    private RestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.ENABLE_USER_TAGS);
    }

    @Test
    public void testGetPerksWithTags() {
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.of(Optional.of(new TagsMatchResponse(List.of("1", "2", "3")))));

        PerkStatResponse perkStatResponse = client.perkStatusAll(UID, MSK_REGION_ID, false);
        long perksCount = Arrays.stream(PerkType.values())
                .filter(pt -> !pt.isDisabled())
                .filter(pt -> pt != PerkType.UNKNOWN)
                .count();
        assertThat(perkStatResponse.getStatuses(), hasSize((int) perksCount));
        assertThat(perkStatResponse.getTags(), containsInAnyOrder(
                new TagStat("1"),
                new TagStat("2"),
                new TagStat("3")
        ));
    }

    @Test
    public void testTagsRetries() {
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .then(invocation -> {
                    Thread.sleep(300); //conn + read timeout
                    throw new RuntimeException();
                });

        PerkStatResponse perkStatResponse = client.perkStatusAll(UID, MSK_REGION_ID, false);
        long perksCount = Arrays.stream(PerkType.values())
                .filter(pt -> !pt.isDisabled())
                .filter(pt -> pt != PerkType.UNKNOWN)
                .count();
        verify(restTemplate, times(3)).exchange(any(), eq(TagsMatchResponse.class));
        assertThat(perkStatResponse.getStatuses(), hasSize((int) perksCount));
        assertThat(perkStatResponse.getTags(), hasSize(0));
    }

    @Test
    public void testNotRetryOn429() {
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Request limit",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        PerkStatResponse perkStatResponse = client.perkStatusAll(UID, MSK_REGION_ID, false);
        long perksCount = Arrays.stream(PerkType.values())
                .filter(pt -> !pt.isDisabled())
                .filter(pt -> pt != PerkType.UNKNOWN)
                .count();
        verify(restTemplate, times(1)).exchange(any(), eq(TagsMatchResponse.class));
        assertThat(perkStatResponse.getStatuses(), hasSize((int) perksCount));
        assertThat(perkStatResponse.getTags(), hasSize(0));
    }
}
