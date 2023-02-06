package ru.yandex.market.tsum.test_data;

import java.util.Date;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;
import org.mockito.Mockito;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 09.02.18
 */
public class TestRepositoryCommitFactory {
    private TestRepositoryCommitFactory() {
    }

    public static RepositoryCommit commit(String revision) {
        return commit(revision, "algebraic");
    }

    public static RepositoryCommit commit(String revision, String author) {
        return commit(revision, author, "commit message");
    }

    public static RepositoryCommit commit(String revision, String author, String message) {
        return commit(revision, author, message, new Date());
    }

    public static RepositoryCommit commit(String revision, String author, String message, Date createdDate) {
        RepositoryCommit repositoryCommit = Mockito.mock(RepositoryCommit.class);
        Mockito.when(repositoryCommit.getSha()).thenReturn(revision);

        User user = Mockito.mock(User.class);
        Mockito.when(user.getLogin()).thenReturn(author);
        Mockito.when(repositoryCommit.getAuthor()).thenReturn(user);

        Commit commit = Mockito.mock(Commit.class);
        Mockito.when(commit.getMessage()).thenReturn(message);

        CommitUser commitUser = Mockito.mock(CommitUser.class);
        Mockito.when(commitUser.getDate()).thenReturn(createdDate);
        Mockito.when(commit.getCommitter()).thenReturn(commitUser);

        Mockito.when(repositoryCommit.getCommit()).thenReturn(commit);
        return repositoryCommit;
    }
}
