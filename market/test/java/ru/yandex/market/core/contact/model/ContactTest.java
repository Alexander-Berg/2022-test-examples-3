package ru.yandex.market.core.contact.model;

import java.util.Set;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.contact.InnerRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ilya Smagin ilya-sm@yandex-team.ru on 8/26/15.
 */
class ContactTest {

    @Test
    void testCleanEmptyLinks() {
        Contact c = new Contact();
        Set<ContactRole> roles = Sets.newHashSet(new ContactRole(100, InnerRole.SHOP_ADMIN.getCode()));
        ContactLink nonEmptyLink = new ContactLink(100, 1, roles);
        ContactLink emptyLink = new ContactLink(200, 2, Sets.newHashSet());
        c.setLinks(Sets.newHashSet(nonEmptyLink, emptyLink));
        c.cleanEmptyLinks();

        assertThat(c.getLinks()).containsOnly(nonEmptyLink);
    }

    @Test
    void testCopyOf() {
        Contact origin = EnhancedRandomBuilder.aNewEnhancedRandom().nextObject(Contact.class);
        Contact copy = Contact.copyOf(origin);

        assertThat(copy)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreCollectionOrder(true)
                        .build())
                .isEqualTo(origin);
    }

}
