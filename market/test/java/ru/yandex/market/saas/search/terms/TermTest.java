package ru.yandex.market.saas.search.terms;

import org.junit.Test;

import ru.yandex.market.saas.search.common.SaasAttr;
import ru.yandex.market.saas.search.term.Term;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("checkstyle:MagicNumber")
public class TermTest {

    private static final SaasAttr<SaasAttr.IntKind, SaasAttr.Search, SaasAttr.NoGroup, SaasAttr.NoProperty> INT_FIELD
            = SaasAttr.intAttr("i_field").search();

    private static final SaasAttr<SaasAttr.StringKind, SaasAttr.Search, SaasAttr.NoGroup, SaasAttr.NoProperty> STR_FIELD
            = SaasAttr.stringAttr("s_field").search();

    private static final SaasAttr<SaasAttr.StringKind, SaasAttr.Zone, SaasAttr.NoGroup, SaasAttr.NoProperty> ZONE_FIELD
            = SaasAttr.stringAttr("z_field").zone();

    @Test
    public void testTerms() {
        assertEquals("i_field:42", toStr(Term.eq(INT_FIELD, 42)));

        assertEquals(" ( i_field:42 && s_field:hello ) ",
                toStr(Term.and(Term.eq(INT_FIELD, 42), Term.eq(STR_FIELD, "hello"))));

        // Prefix search
        assertEquals(" ( i_field:42 && s_field:hello* ) ",
                toStr(Term.and(Term.eq(INT_FIELD, 42), Term.eq(STR_FIELD, "hello*"))));

        assertEquals(" ( i_field:42 && s_field:\"hello world\" ) ",
                toStr(Term.and(Term.eq(INT_FIELD, 42), Term.eq(STR_FIELD, "hello world"))));

        // NOTE: for now, quotes are stripped. More research on quotes handling is needed (if anyone needs it).
        assertEquals(" ( i_field:42 && s_field:\"hello world\" ) ",
                toStr(Term.and(Term.eq(INT_FIELD, 42), Term.eq(STR_FIELD, "hello 'world"))));

        assertEquals(" ( i_field:42 | i_field:43 ) ",
                toStr(Term.or(Term.eq(INT_FIELD, 42), Term.eq(INT_FIELD, 43))));

        assertEquals(" ( i_field:42 && i_field:43 ~~ s_field:bad ~~ s_field:\"very bad\" ) ", toStr(
                Term.and(Term.eq(INT_FIELD, 42),
                        Term.not(Term.eq(STR_FIELD, "bad")),
                        Term.not(Term.eq(STR_FIELD, "very bad")),
                        Term.eq(INT_FIELD, 43))));

        assertEquals(" ( i_field:42 /0 s_field:test ) ", toStr(
                Term.distance(0, Term.eq(INT_FIELD, 42),
                        Term.eq(STR_FIELD, "test"))));

        assertEquals("z_field:anything", toStr(Term.contains(ZONE_FIELD, "anything")));

        assertEquals("z_field:\"hello world\"", toStr(Term.contains(ZONE_FIELD, "hello world")));

        assertEquals("z_field:(hello world) ", toStr(Term.zoneText(ZONE_FIELD, "hello world")));

        try {
            Term.or(Term.not(Term.eq(INT_FIELD, 42)));
            fail("Or on not must throw exception");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            Term.not(Term.not(Term.eq(INT_FIELD, 42)));
            fail("Not not must throw exception");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            toStr(Term.and(Term.not(Term.eq(INT_FIELD, 42))));
            fail("And with single Not must throw exception on rendering");
        } catch (IllegalStateException ignored) {
        }
    }

    private String toStr(Term term) {
        StringBuilder sb = new StringBuilder();
        term.mkString(sb);
        return sb.toString();
    }
}
