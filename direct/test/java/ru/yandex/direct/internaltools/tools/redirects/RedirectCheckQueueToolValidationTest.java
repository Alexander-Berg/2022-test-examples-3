package ru.yandex.direct.internaltools.tools.redirects;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.tools.redirects.model.RedirectCheckQueueParameters;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectCheckQueueToolValidationTest {
    private RedirectCheckQueueTool tool;

    @Before
    public void before() {
        tool = new RedirectCheckQueueTool(null, null);
    }

    @Test
    public void testValidationCorrectAll() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withBannerIds("123, 456, 789")
                .withCampaignIds("9876, 6543, 321");
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationCorrectOnlyBannerIds() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withBannerIds("123, 456, 789");
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationCorrectOnlyCampaignIds() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withCampaignIds("9876, 6543, 321");
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationInvalidEmpty() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters();
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testValidationInvalidBadInt() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withCampaignIds("1234, asdf, 12345");
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testValidationInvalidNegativeInt() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withCampaignIds("12345, -1, 4321");
        ValidationResult<RedirectCheckQueueParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isTrue();
    }
}
