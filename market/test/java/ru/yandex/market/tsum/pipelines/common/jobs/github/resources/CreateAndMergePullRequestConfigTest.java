package ru.yandex.market.tsum.pipelines.common.jobs.github.resources;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.resources.CreateAndMergePullRequestConfig.DEFAULT_AUTO_MERGE_REQUEST;

@RunWith(JUnit4.class)
public class CreateAndMergePullRequestConfigTest {

    private static final GithubRepo REPO = new GithubRepo("qwe");
    private static final BranchRef BRANCH = new BranchRef("rty");

    @Test
    public void emptyConfigIsEmpty() throws Exception {
        CreateAndMergePullRequestConfig config = CreateAndMergePullRequestConfig.builder().build();
        assertNull(config.getTargetBranch());
        assertEquals(DEFAULT_AUTO_MERGE_REQUEST, config.isAutoMergeRequest());
        assertThat(config.getTagUsers(), empty());
    }

    @Test
    public void withTargetBranch() throws Exception {
        CreateAndMergePullRequestConfig config =
            CreateAndMergePullRequestConfig.builder().withTargetBranch(BRANCH).build();
        assertSame(BRANCH, config.getTargetBranch());
        assertEquals(DEFAULT_AUTO_MERGE_REQUEST, config.isAutoMergeRequest());
        assertThat(config.getTagUsers(), empty());
    }

    @Test
    public void withAutoMergeRequest() throws Exception {
        CreateAndMergePullRequestConfig config = CreateAndMergePullRequestConfig.builder()
            .withAutoMergeRequest(!DEFAULT_AUTO_MERGE_REQUEST).build();
        assertNull(config.getTargetBranch());
        assertNotEquals(DEFAULT_AUTO_MERGE_REQUEST, config.isAutoMergeRequest());
        assertThat(config.getTagUsers(), empty());
    }

    @Test
    public void addSingleTagUser() throws Exception {
        CreateAndMergePullRequestConfig config = CreateAndMergePullRequestConfig.builder().addTagUser("user").build();
        assertNull(config.getTargetBranch());
        assertEquals(DEFAULT_AUTO_MERGE_REQUEST, config.isAutoMergeRequest());
        assertThat(config.getTagUsers(), is(singletonList("user")));
    }

    @Test
    public void testAddTagSavesMultipleValues() throws Exception {
        List<String> tagUsers =
            CreateAndMergePullRequestConfig.builder().addTagUser("qwe").addTagUser("rty").build().getTagUsers();
        assertEquals(tagUsers, asList("qwe", "rty"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnNull() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnEmptyString() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnSpace() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser("q w");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnTab() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser("q\tw");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnNewline() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser("q\nw");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagUserFailsOnAt() throws Exception {
        CreateAndMergePullRequestConfig.builder().addTagUser("@qwe");
    }

    @Test
    public void emptyConstructorForDeserializer() throws Exception {
        new CreateAndMergePullRequestConfig();
    }
}
