package ru.yandex.market.tsum.pipelines.test_data;

import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;

import ru.yandex.market.tsum.clients.github.model.ExtendedPullRequest;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.05.17
 */
public class TestPullRequestFactory {
    private TestPullRequestFactory() {
    }

    public static ExtendedPullRequest pullRequest() {
        return pullRequest(1);
    }

    public static ExtendedPullRequest pullRequest(int number) {
        return pullRequest(number, 1, "feature", 1, "master");
    }

    public static ExtendedPullRequest pullRequest(String head, String base) {
        return pullRequest(1, head, 1, base);
    }

    public static ExtendedPullRequest pullRequest(int number, String head, String base) {
        return pullRequest(number, 1, head, 1, base);
    }

    public static ExtendedPullRequest pullRequest(int headRepoId, String head, int baseRepoId, String base) {
        return pullRequest(1, headRepoId, head, baseRepoId, base);
    }

    public static ExtendedPullRequest pullRequest(int number, int headRepoId, String head, int baseRepoId,
                                                  String base) {
        ExtendedPullRequest pullRequest = new ExtendedPullRequest();
        pullRequest.setNumber(number);
        PullRequestMarker headMarker = new PullRequestMarker();
        headMarker.setRef(head);
        Repository sourceRepo = createRepository(headRepoId);
        headMarker.setRepo(sourceRepo);
        pullRequest.setHead(headMarker);
        PullRequestMarker baseMarker = new PullRequestMarker();
        baseMarker.setRef(base);
        baseMarker.setRepo(createRepository(baseRepoId));
        pullRequest.setBase(baseMarker);
        pullRequest.setId(number);
        return pullRequest;
    }

    private static Repository createRepository(int id) {
        Repository sourceRepo = new Repository();
        sourceRepo.setId(id);
        return sourceRepo;
    }

}
