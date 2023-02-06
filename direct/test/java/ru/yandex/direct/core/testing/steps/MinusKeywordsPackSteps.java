package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;

public class MinusKeywordsPackSteps {

    private final ClientSteps clientSteps;
    private final MinusKeywordsPackRepository minusKeywordsPackRepository;
    private final TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    @Autowired
    public MinusKeywordsPackSteps(ClientSteps clientSteps,
                                  MinusKeywordsPackRepository minusKeywordsPackRepository,
                                  TestMinusKeywordsPackRepository testMinusKeywordsPackRepository) {
        this.clientSteps = clientSteps;
        this.minusKeywordsPackRepository = minusKeywordsPackRepository;
        this.testMinusKeywordsPackRepository = testMinusKeywordsPackRepository;
    }

    public MinusKeywordsPackInfo createDefaultMinusKeywordsPack() {
        return createMinusKeywordsPack((MinusKeywordsPack) null);
    }

    public MinusKeywordsPackInfo createPrivateMinusKeywordsPack() {
        return createMinusKeywordsPack(privateMinusKeywordsPack());
    }

    public MinusKeywordsPackInfo createPrivateMinusKeywordsPack(ClientInfo clientInfo) {
        return createMinusKeywordsPack(privateMinusKeywordsPack(), clientInfo);
    }

    public MinusKeywordsPackInfo createLibraryMinusKeywordsPack() {
        return createMinusKeywordsPack(libraryMinusKeywordsPack());
    }

    public MinusKeywordsPackInfo createLibraryMinusKeywordsPack(ClientInfo clientInfo) {
        return createMinusKeywordsPack(libraryMinusKeywordsPack(), clientInfo);
    }

    public List<Long> createLibraryMinusKeywordsPacks(ClientInfo clientInfo, int count) {
        checkArgument(count > 0);
        return IntStream.range(0, count)
                .mapToObj(i -> createLibraryMinusKeywordsPack(clientInfo))
                .map(MinusKeywordsPackInfo::getMinusKeywordPackId)
                .collect(toList());
    }

    public MinusKeywordsPackInfo createMinusKeywordsPack(ClientInfo clientInfo) {
        return createMinusKeywordsPack(null, clientInfo);
    }

    public MinusKeywordsPackInfo createMinusKeywordsPack(MinusKeywordsPack pack, ClientInfo clientInfo) {
        return createMinusKeywordsPack(new MinusKeywordsPackInfo()
                .withClientInfo(clientInfo)
                .withMinusKeywordsPack(pack));
    }

    public MinusKeywordsPackInfo createMinusKeywordsPack(MinusKeywordsPack pack) {
        return createMinusKeywordsPack(new MinusKeywordsPackInfo().withMinusKeywordsPack(pack));
    }

    public MinusKeywordsPackInfo createMinusKeywordsPack(MinusKeywordsPackInfo packInfo) {
        if (packInfo.getMinusKeywordsPack() == null) {
            packInfo.withMinusKeywordsPack(libraryMinusKeywordsPack());
        }
        if (packInfo.getMinusKeywordPackId() == null) {
            if (packInfo.getClientId() == null) {
                clientSteps.createClient(packInfo.getClientInfo());
            }
            minusKeywordsPackRepository
                    .createLibraryMinusKeywords(packInfo.getShard(), packInfo.getClientId(),
                            singletonList(packInfo.getMinusKeywordsPack()));
        }
        return packInfo;
    }

    public MinusKeywordsPackInfo createAndLinkMinusKeywordsPack(AdGroupInfo adGroupInfo) {
        MinusKeywordsPackInfo minusKeywordsPack = createMinusKeywordsPack(adGroupInfo.getClientInfo());
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(adGroupInfo.getShard(),
                minusKeywordsPack.getMinusKeywordPackId(), adGroupInfo.getAdGroupId());
        return minusKeywordsPack;
    }

    public void linkLibraryMinusKeywordPackToAdGroup(long packId, long adGroupId, ClientInfo clientInfo) {
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), packId, adGroupId);
    }

    public void deleteMinusWordsPack(Long id, ClientInfo clientInfo) {
        testMinusKeywordsPackRepository.deleteMinusKeywordPack(clientInfo.getShard(), id);
    }

}
