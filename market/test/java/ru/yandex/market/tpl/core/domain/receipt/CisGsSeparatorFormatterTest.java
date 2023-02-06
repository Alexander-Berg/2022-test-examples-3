package ru.yandex.market.tpl.core.domain.receipt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CisGsSeparatorFormatter.class)
public class CisGsSeparatorFormatterTest {

    @Autowired
    private CisGsSeparatorFormatter formatter;

    private void doTest(String cis) {
        assertThat(formatter.unescapeGsSeparators(formatter.escapeGsSeparators(cis))).isEqualTo(cis);
    }

    @Test
    void testSimple() {
        doTest("1234asd!@#$\"'+\\=");
    }

    @Test
    void testGs() {
        doTest("1234\u001Dasd");
    }

    @Test
    void testGsWithSameGsEscape() {
        doTest("\\u001D1234\u001Dasd");
    }

    @Test
    void testRealCis() {
        doTest("010463010903025521ddJhtf0f<CNeS\u001D910094\u001D920FUi4Pvk4OLDamXViP5nGt+7xdGe1EO8cwjxyKg" +
                "Snndro96/ukY+Iy7zNlJj+5FRUtEl6pLHp9NkKUiHxPMZtg==");
    }

    @Test
    void testRealCisWithSameGsEscape() {
        doTest("010463010903025521ddJhtf0f<CNeS\u001D910094\u001D920FUi4Pvk4OLDamXViP5nGt+7xdGe1EO8cwjxyKg" +
                "Snndro96/ukY+Iy7zNlJj+5FRU\\u001DtEl6pLHp9NkKUiHxPMZtg==");
    }
}
