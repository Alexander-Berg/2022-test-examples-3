package ru.yandex.market.core.contact.model;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Test;

import ru.yandex.market.core.contact.InnerRole;

import static org.junit.Assert.assertEquals;

/**
 * @author Ilya Smagin ilya-sm@yandex-team.ru on 8/24/15.
 */
public class ContactLinksDiffTest {

    private static final long CAMPAIGN_ID_1 = 1;
    private static final long CAMPAIGN_ID_2 = 2;
    private static final long CAMPAIGN_ID_3 = 3;

    @Test
    public void testPut() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(),
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()))))
        );

        assertEquals(Sets.newHashSet(CAMPAIGN_ID_1), diff);
    }

    @Test
    public void testCleanRole() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode())))),
                Sets.newHashSet()
        );

        assertEquals(Sets.newHashSet(CAMPAIGN_ID_1), diff);
    }


    @Test
    public void testCleanEmpty() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet())),
                Sets.newHashSet()
        );

        assertEquals(Sets.<Long>newHashSet(), diff);
    }

    @Test
    public void testSameEmpty() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet())),
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet()))
        );

        assertEquals(Sets.<Long>newHashSet(), diff);
    }

    @Test
    public void testFillEmpty() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet())),
                Sets.newHashSet(new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()))))
        );

        assertEquals(Sets.newHashSet(CAMPAIGN_ID_1), diff);
    }

    @Test
    public void testAdd() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(
                        new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode())))),
                Sets.newHashSet(
                        new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()))),
                        new ContactLink(200, CAMPAIGN_ID_2, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()))),
                        new ContactLink(300, CAMPAIGN_ID_3, Sets.newHashSet()))
        );

        assertEquals(Sets.newHashSet(CAMPAIGN_ID_2), diff);
    }

    @Test
    public void testCompexEmpty() {
        Set<Long> diff = ContactLinksDiff.apply(
                Sets.newHashSet(
                        new ContactLink(100, CAMPAIGN_ID_1, Sets.newHashSet())),
                Sets.newHashSet(
                        new ContactLink(200, CAMPAIGN_ID_2, Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()))),
                        new ContactLink(300, CAMPAIGN_ID_3, Sets.newHashSet()))
        );

        assertEquals(Sets.newHashSet(CAMPAIGN_ID_2), diff);
    }
}