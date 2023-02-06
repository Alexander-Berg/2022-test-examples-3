package ru.yandex.market.indexer.problem;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.ArgumentMatcher;

import ru.yandex.market.indexer.problem.model.FeedIndexationInfo;

/**
 * {@link ArgumentMatcher}, проверяющий что в переданном аргументе присутствуют только ожидаемые идентификаторы фида.
 */
public class FeedIndexationInfoCollectionMatcher implements ArgumentMatcher<Collection<FeedIndexationInfo>> {

    private final Collection<Long> feedIds;

    public FeedIndexationInfoCollectionMatcher(Collection<Long> feedIds) {
        this.feedIds = feedIds;
    }

    @Override
    public boolean matches(Collection<FeedIndexationInfo> argument) {
        if (argument == null) {
            return false;
        }
        List<Long> argumentIds = argument.stream()
                .map(FeedIndexationInfo::getFeedId)
                .collect(Collectors.toList());
        return new HashSet<>(feedIds).equals(new HashSet<>(argumentIds));
    }
}
