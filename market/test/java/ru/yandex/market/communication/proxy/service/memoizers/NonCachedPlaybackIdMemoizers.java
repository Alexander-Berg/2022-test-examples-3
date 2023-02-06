package ru.yandex.market.communication.proxy.service.memoizers;

import com.google.common.base.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.communication.proxy.service.environment.EnvironmentService;
import ru.yandex.market.communication.proxy.service.environment.memoizers.PlaybackIdMemoizers;

@Component
@Profile("functionalTest")
public class NonCachedPlaybackIdMemoizers extends PlaybackIdMemoizers {

    private final Supplier<String> shopPlaybackId;
    private final Supplier<String> buyerPlaybackId;
    private final Supplier<String> beforeConversationPlaybackId;

    public NonCachedPlaybackIdMemoizers(EnvironmentService environmentService) {
        this.buyerPlaybackId = () -> environmentService.getValue(BUYER_PLAYBACK_ID, "");
        this.shopPlaybackId = () -> environmentService.getValue(SHOP_PLAYBACK_ID, "");
        this.beforeConversationPlaybackId = () -> environmentService.getValue(BEFORE_CONVERSATION_PLAYBACK_ID, "");
    }

    @Override
    public Supplier<String> getShopPlaybackId() {
        return shopPlaybackId;
    }

    @Override
    public Supplier<String> getBuyerPlaybackId() {
        return buyerPlaybackId;
    }

    @Override
    public Supplier<String> getBeforeConversationPlaybackId() {
        return beforeConversationPlaybackId;
    }
}
