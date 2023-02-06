package ru.yandex.market.api.common.client;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.assertFalse;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class SemanticVersionTest extends UnitTestBase {

    @Test
    public void shouldCompareVersions() {

        Assert.assertTrue(new SemanticVersion(1, 2, 3).compareTo(new SemanticVersion(1, 2, 4)) < 0);
        assertFalse(new SemanticVersion(1, 2, 3).compareTo(new SemanticVersion(1, 2, 4)) >= 0);

        Assert.assertTrue(new SemanticVersion(1, 2, 3).compareTo(new SemanticVersion(1, 2, 3)) == 0);

        Assert.assertTrue(new SemanticVersion(1, 2, 5).compareTo(new SemanticVersion(1, 2, 4)) > 0);
        assertFalse(new SemanticVersion(1, 2, 5).compareTo(new SemanticVersion(1, 2, 4)) <= 0);
    }
}
