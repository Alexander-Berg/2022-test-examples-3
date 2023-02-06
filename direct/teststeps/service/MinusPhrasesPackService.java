package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.stereotype.Service;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;

import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;

@Service
@ParametersAreNonnullByDefault
public class MinusPhrasesPackService {
    private final MinusKeywordsPackSteps minusKeywordsSteps;
    private final InfoHelper infoHelper;

    public MinusPhrasesPackService(MinusKeywordsPackSteps minusKeywordsSteps, InfoHelper infoHelper) {
        this.minusKeywordsSteps = minusKeywordsSteps;
        this.infoHelper = infoHelper;
    }

    public Long createLibraryMinusWordsPack(String login, List<String> minusKeyWords, String packName) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        var pack = libraryMinusKeywordsPack()
                .withName(packName)
                .withMinusKeywords(minusKeyWords);
        return minusKeywordsSteps.createMinusKeywordsPack(pack, clientInfo).getMinusKeywordPackId();
    }

    public void deleteMinusWordsPack(String login, Long id) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        minusKeywordsSteps.deleteMinusWordsPack(id, clientInfo);
    }

    public void linkLibraryMinusKeywordPackToAdGroup(String login, Long packId, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        minusKeywordsSteps.linkLibraryMinusKeywordPackToAdGroup(packId, adGroupId, clientInfo);
    }
}
