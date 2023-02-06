package ru.yandex.market.tsum.sox.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.sox.validation.validators.SoxPatternValidator;
import ru.yandex.market.tsum.sox.validation.validators.SoxSandboxTtlValidator;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 08/11/2018
 */
public class SoxValidationServiceTest {
    private Map<Class<? extends SoxValidator>, SoxValidator> validators = new HashMap<>();

    @Before
    public void setup() {
        validators.put(SoxPatternValidator.class, new SoxPatternValidator());
        validators.put(SoxSandboxTtlValidator.class, new SoxSandboxTtlValidator(null));
    }

    @Test
    public void validatesValid() {
        List<SoxValidationRule> rules = SoxValidation.forTargets("yandex-package-a", "yandex-package-b")
            .addRule("Rule A", SoxValidation.pattern().withPattern("Собран пакет yandex-package-a").build())
            .create();

        SoxValidationService service = new SoxValidationService((validators::get), rules);

        Issue issue = createMockIssue("Собран пакет yandex-package-a успешно");
        SoxValidationResult result = service.validate("yandex-package-a", issue);

        Assert.assertEquals(0, result.getProblems().size());
        Assert.assertTrue(result.isValid());
    }

    @Test
    public void validatesNotValid() {
        List<SoxValidationRule> rules = SoxValidation.forTargets("yandex-package-a", "yandex-package-b")
            .addRule(
                "Rule A",
                SoxValidation.pattern().withPattern("Собран пакет yandex-package-a").build()
            )
            .addRule(
                "Rule B",
                SoxValidation.sandboxTtl()
                    .withCommentPattern("Собран пакет yandex-package-b")
                    .withLinkText("")
                    .withTtlDays(1)
                    .build()
            )
            .create();

        SoxValidationService service = new SoxValidationService((validators::get), rules);

        Issue issue = createMockIssue("Собран пакет yandex-package-a успешно");
        SoxValidationResult result = service.validate("yandex-package-a", issue);

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(1, result.getProblems().size());
        Assert.assertEquals(
            "Правило \"Rule B\" не выполнено (Не найден комментарий удовлетворяющий паттерну)",
            result.getProblems().get(0)
        );
    }

    @Test
    public void validatesNotExisting() {
        List<SoxValidationRule> rules = SoxValidation.forTargets("yandex-package-a", "yandex-package-b")
            .addRule("Rule A", SoxValidation.pattern().withPattern("Собран пакет yandex-package-a").build())
            .create();

        SoxValidationService service = new SoxValidationService((validators::get), rules);

        Issue issue = createMockIssue();
        SoxValidationResult result = service.validate("MARKET_SANDBOX_RESOURCE", issue);

        Assert.assertEquals(0, result.getProblems().size());
        Assert.assertFalse(result.isValid());
    }

    private Issue createMockIssue(String... comments) {
        Issue issue = mock(Issue.class);
        ArrayListF<Comment> issueComments = Arrays.stream(comments)
            .map(this::toComment)
            .collect(Collectors.toCollection(ArrayListF::new));

        when(issue.getComments()).thenReturn(issueComments.iterator());

        return issue;
    }

    private Comment toComment(String text) {
        Comment comment = mock(Comment.class);
        when(comment.getText()).thenReturn(Option.of(text));
        return comment;
    }
}
