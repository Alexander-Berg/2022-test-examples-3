package ru.yandex.market.tsum.clients.github;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.tsum.clients.github.GitHubClient.sanitizeBranchName;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 24.05.17
 */
public class GitHubClientTest {
    @Test
    public void testSanitizeBranchName() throws Exception {
        //A branch name can not:
        // Have a path component that begins with "."
        assertEquals("release", sanitizeBranchName(".release"));
        assertEquals("release/release/release", sanitizeBranchName("release/.release/.release"));
        // Have a double dot ".."
        assertEquals("r.e.l.e.a.s.e", sanitizeBranchName("r.e..l...e....a.....s....e"));
        // Have an ASCII control character, "~", "^", ":" or SP, anywhere
        // Contain a "\" (backslash)
        assertEquals("r-e-l-e-a-s-e", sanitizeBranchName("r~e^l\0e:a s\\e"));
        // End with a "/"
        assertEquals("release", sanitizeBranchName("release/"));
        // End with ".lock"
        assertEquals("lock.lock/Sher", sanitizeBranchName("lock.lock/Sher.lock"));

        assertEquals("привет", sanitizeBranchName("привет"));
        assertEquals("Market-experiments-2016.13", sanitizeBranchName("Market-experiments 2016.13"));
        assertEquals("Market-2016.74.hotfix", sanitizeBranchName("Market 2016.74.hotfix"));
    }
}
