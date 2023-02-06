package ru.yandex.market.core.contact.view;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactWithEmail;

import static org.hamcrest.MatcherAssert.assertThat;

class ContactConverterTest {

    private ContactConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ContactConverter();
    }

    @Test
    void it_must_convert_id_field() {
        // Given
        final Contact contact = createTestContact();
        contact.setId(123);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "id");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("123"));
    }


    @Test
    void it_must_convert_firstName_field() {
        // Given
        final Contact contact = createTestContact();
        contact.setFirstName("Иван");

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "first-name");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("Иван"));
    }

    @Test
    void it_must_convert_secondName_field() {
        // Given
        final Contact contact = createTestContact();
        contact.setSecondName("Иванович");

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "second-name");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("Иванович"));
    }

    @Test
    void it_must_convert_lastName_field() {
        // Given
        final Contact contact = createTestContact();
        contact.setLastName("Иванов");

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "last-name");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("Иванов"));
    }

    @Test
    void it_must_convert_userId_field_when_userId_is_positive() {
        // Given
        final Contact contact = createTestContact();
        contact.setUserId(1L);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "uid");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("1"));
    }

    @Test
    void it_must_not_convert_userId_field_when_userId_is_null() {
        // Given
        final Contact contact = createTestContact();
        contact.setUserId(null);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "uid");
        assertThat(values, Matchers.hasSize(0));
    }

    @Test
    void it_must_convert_marketOnly_field_and_return_true_when_value_is_set_to_true() {
        // Given
        final Contact contact = createTestContact();
        contact.setMarketOnly(true);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "is-market-only");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("true"));
    }

    @Test
    void it_must_convert_marketOnly_field_and_return_false_when_value_is_set_to_false() {
        // Given
        final Contact contact = createTestContact();
        contact.setMarketOnly(false);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "is-market-only");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("false"));
    }

    @Test
    void it_must_convert_marketOnly_field_and_return_false_when_value_is_not_set() {
        // Given
        final Contact contact = createTestContact();

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "is-market-only");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("false"));
    }

    @Test
    void it_must_convert_adv_agree_field() {
        // Given
        final Contact contact = createTestContact();
        contact.setAdvAgree(true);

        // When
        final Element element = converter.convert(contact);

        // Then
        final List<String> values = extractValues(element, "adv-agree");
        assertThat(values, Matchers.hasSize(1));
        assertThat(values, Matchers.contains("true"));
    }

    private ContactWithEmail createTestContact() {
        final ContactWithEmail contact = new ContactWithEmail();
        contact.setEmails(Collections.emptySet());
        contact.setLinks(Collections.emptySet());
        return contact;
    }

    private List<String> extractValues(Element element, String path) {
        try {
            final XPath xPath = XPath.newInstance("/contact/" + path + "/value/text()");
            final List<Text> list = xPath.selectNodes(new Document(element));
            return list.stream().map(Text::getText).collect(Collectors.toList());
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        }
    }
}
