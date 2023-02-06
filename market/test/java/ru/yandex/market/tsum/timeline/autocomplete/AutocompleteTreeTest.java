package ru.yandex.market.tsum.timeline.autocomplete;

import org.junit.Test;
import ru.yandex.misc.test.Assert;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 17.09.18
 */
public class AutocompleteTreeTest {
    private static AutocompleteTree autocompleteTree = new AutocompleteTree();

    @Test
    public void firstLevelTest() {
        String nannyDeploy = "nanny-deploy";
        String nanny = "nanny";
        String deployNanny = "deploy-nanny";
        String nannyService = "nanny-service";

        test(
            Arrays.asList(
                "experiments", deployNanny, nanny, nannyDeploy, nannyService, nannyService + ":someService"
            ),
            "nanny",
            Arrays.asList(
                nanny, nannyDeploy, nannyService, nannyService + ":", deployNanny
            )
        );
    }

    @Test
    public void thirdLevelTest() {
        String abc = "nanny-service:market:abc";
        String abcd = "nanny-service:market:abcd";
        String xyzabcd = "nanny-service:market:xyzabcd";
        String xyzabcdQwe = "nanny-service:market:xyzabcd:qwe";
        String xyzabcdAsd = "nanny-service:market:xyzabcd:asd";

        test(
            Arrays.asList(
                "experiments", xyzabcdAsd, abcd, xyzabcd, abc, xyzabcdQwe, "nanny-service:someService", "nanny-service:market:ab"
            ),
            "nanny-service:market:abc",
            Arrays.asList(
                abc, abcd, xyzabcd, xyzabcd + ":"
            )
        );
    }

    @Test
    public void endsWithColonTest() {
        String ab = "nanny-service:market:ab";
        String abc = "nanny-service:market:abc";
        String abcd = "nanny-service:market:abcd";
        String xyzabcd = "nanny-service:market:xyzabcd";
        String xyzabcdQwe = "nanny-service:market:xyzabcd:qwe";
        String xyzabcdAsd = "nanny-service:market:xyzabcd:asd";

        test(
            Arrays.asList(
                "experiments", xyzabcdAsd, abcd, xyzabcd, abc, xyzabcdQwe, "nanny-service:someService", "nanny-service:market:ab"
            ),
            "nanny-service:market:",
            Arrays.asList(
                ab, abc, abcd, xyzabcd, xyzabcd + ":"
            )
        );
    }

    private void test(List<String> input, String suffix, List<String> expected) {
        autocompleteTree.fillEntities(input);
        Assert.equals(
            expected,
            autocompleteTree.autoComplete(suffix)
        );
    }
}