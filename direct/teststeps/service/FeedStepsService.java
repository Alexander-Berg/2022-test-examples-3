package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedDefectIdsEnum;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.FeedSteps;

@Service
@ParametersAreNonnullByDefault
public class FeedStepsService {
    private final FeedSteps feedSteps;
    private final InfoHelper infoHelper;

    public FeedStepsService(FeedSteps feedSteps, InfoHelper infoHelper) {
        this.feedSteps = feedSteps;
        this.infoHelper = infoHelper;
    }

    public long createDefaultFeed(String login) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        return feedSteps.createDefaultFeed(clientInfo).getFeedId();
    }

    public void deleteFeed(String login, List<Long> feedIds) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        feedSteps.deleteFeed(clientInfo.getShard(), feedIds);
    }

    public Long processFeed(String login, Long feedId, UpdateStatus updateStatus, @Nullable FeedDefectIdsEnum defect) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        return feedSteps.processFeed(clientInfo, feedId, updateStatus, defect).getFeedId();
    }
}
