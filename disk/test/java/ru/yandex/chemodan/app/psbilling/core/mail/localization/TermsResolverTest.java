package ru.yandex.chemodan.app.psbilling.core.mail.localization;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.inside.utils.Language;

public class TermsResolverTest extends AbstractPsBillingCoreTest {

    @Autowired
    private TermsResolver termsResolver;

    @Test
    public void testOneForm() {
        Assert.assertEquals("вашей организации", termsResolver.getOneFormOfTerm("yourOrganization", Language.RUSSIAN));
    }

    @Test
    public void testManyForm() {
        Assert.assertEquals("вашей организации", termsResolver.getManyFormOfTerm("yourOrganization", Language.RUSSIAN
                , 1));
        Assert.assertEquals("вашим организациям", termsResolver.getManyFormOfTerm("yourOrganization",
                Language.RUSSIAN, 2));
    }

    @Test
    public void testManyForms() {
        Assert.assertEquals("год", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 1));
        Assert.assertEquals("2 года", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 2));
        Assert.assertEquals("4 года", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 4));
        Assert.assertEquals("5 лет", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 5));
        Assert.assertEquals("6 лет", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 6));
        Assert.assertEquals("100500 лет", termsResolver.getManyFormOfTerm("year", Language.RUSSIAN, 100500));

        Assert.assertEquals("4 месяца", termsResolver.getManyFormOfTerm("month", Language.RUSSIAN, 4));
        Assert.assertEquals("5 месяцев", termsResolver.getManyFormOfTerm("month", Language.RUSSIAN, 5));

        Assert.assertEquals("4 недели", termsResolver.getManyFormOfTerm("week", Language.RUSSIAN, 4));
        Assert.assertEquals("5 недель", termsResolver.getManyFormOfTerm("week", Language.RUSSIAN, 5));

        Assert.assertEquals("4 дня", termsResolver.getManyFormOfTerm("day", Language.RUSSIAN, 4));
        Assert.assertEquals("5 дней", termsResolver.getManyFormOfTerm("day", Language.RUSSIAN, 5));

        Assert.assertEquals("4 часа", termsResolver.getManyFormOfTerm("hour", Language.RUSSIAN, 4));
        Assert.assertEquals("5 часов", termsResolver.getManyFormOfTerm("hour", Language.RUSSIAN, 5));

        Assert.assertEquals("4 минуты", termsResolver.getManyFormOfTerm("minute", Language.RUSSIAN, 4));
        Assert.assertEquals("5 минут", termsResolver.getManyFormOfTerm("minute", Language.RUSSIAN, 5));

        Assert.assertEquals("4 десятиминутки", termsResolver.getManyFormOfTerm("10minutes", Language.RUSSIAN, 4));
        Assert.assertEquals("5 десятиминуток", termsResolver.getManyFormOfTerm("10minutes", Language.RUSSIAN, 5));
    }
}
