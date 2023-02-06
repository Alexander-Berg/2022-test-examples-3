package ru.yandex.direct.core.entity.sitelink.repository;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class SitelinkRepositoryCalcHashTest {

    @Parameterized.Parameter()
    public Sitelink firstSitelink;

    @Parameterized.Parameter(1)
    public Sitelink secondSitelink;

    @Parameterized.Parameter(2)
    public boolean isDuplicate;

    @Parameterized.Parameters(name = "firstSitelink {0} and secondSitelink {1} isDuplicate {2}")
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {defaultSitelink(), defaultSitelink(), true},
                {defaultSitelink().withTurboLandingId(1L), defaultSitelink().withTurboLandingId(1L), true},

                //доп поля сайтлинков НЕ влияют на расчет хеша
                {defaultSitelink(), defaultSitelink().withId(2L), true},
                {defaultSitelink(), defaultSitelink().withOrderNum(2L), true},
                {defaultSitelink(), defaultSitelink().withHash(BigInteger.TEN), true},

                //основные поля сайтлинков влияют на расчет хеша
                {defaultSitelink(), defaultSitelink().withTitle("title2"), false},
                {defaultSitelink(), defaultSitelink().withHref("href2"), false},
                {defaultSitelink(), defaultSitelink().withDescription("desc2"), false},

                //турболендинги влияют на расчет хеша
                {defaultSitelink(), defaultSitelink().withTurboLandingId(1L), false},
                {defaultSitelink().withTurboLandingId(1L), defaultSitelink().withTurboLandingId(2L), false}
        });
    }

    @Test
    public void calcHashTest() {
        BigInteger firstHash = SitelinkRepository.calcHash(firstSitelink);
        BigInteger secondHash = SitelinkRepository.calcHash(secondSitelink);

        assertThat("расчет хеша должен соответствовать ожиданию", firstHash.equals(secondHash), equalTo(isDuplicate));
    }

    private static Sitelink defaultSitelink() {
        return new Sitelink()
                .withId(1L)
                .withTitle("title")
                .withHref("href")
                .withDescription("desc");
    }
}
