package ru.yandex.market.tsum.pipelines.apps.jobs.startrek;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ReadOnlyArrayList;
import ru.yandex.market.tsum.pipelines.apps.jobs.CheckStartrekTicketFieldJob;
import ru.yandex.market.tsum.pipelines.apps.resources.TicketFieldValueConfigResource;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckStartrekTicketFieldJobTest {

    private static final String FIELD_NAME = "regression";

    @InjectMocks
    private final CheckStartrekTicketFieldJob job = new CheckStartrekTicketFieldJob();
    @Mock
    private TicketFieldValueConfigResource ticketFieldValueConfigResource;
    @Mock
    private Issue issue;

    @Before
    public void setUp() {
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(Collections.emptyList());
        when(ticketFieldValueConfigResource.getBadValues()).thenReturn(Collections.emptyList());
        when(ticketFieldValueConfigResource.getFieldKey()).thenReturn(FIELD_NAME);
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(issue.getO(FIELD_NAME)).thenReturn(Option.empty());
    }

    @Test
    public void shouldFalseWhenFieldIsGoneAndAllowFalse() {
        when(issue.getO(FIELD_NAME)).thenReturn(Option.empty());
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenFieldIsGoneAndAllowTrue() {
        when(issue.getO(FIELD_NAME)).thenReturn(Option.empty());
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(true);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenFieldIsEmptyAndAllowFalse() {
        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.empty()));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenFieldIsEmptyAndAllowTrue() {
        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.empty()));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(true);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenSingleValueInFieldNotExistInAvailable() {
        final String fieldValue = "some value";
        final List<String> availableValues = Arrays.asList("value 1", "value 2");

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.of(fieldValue)));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldTrueWhenSingleValueInFieldExistInAvailable() {
        final String fieldValue = "some value";
        final List<String> availableValues = Arrays.asList("value 1", "value 2", fieldValue);

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.of(fieldValue)));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        boolean result = job.checkField(issue);
        assertTrue(result);
    }

    @Test
    public void shouldTrueWhenSingleValueInFieldNotExistInAvailableButAllowAnyNotBlankValue() {
        final String fieldValue = "some value";
        final List<String> availableValues = Arrays.asList("value 1", "value 2");

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.of(fieldValue)));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(true);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        boolean result = job.checkField(issue);
        assertTrue(result);
    }

    @Test
    public void shouldFalseWhenSingleValueInFieldExistInAvailableButFieldExistInBadValues() {
        final String fieldValue = "some value";
        final List<String> availableValues = Arrays.asList("value 1", fieldValue);
        final List<String> badValues = Collections.singletonList(fieldValue);

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.of(fieldValue)));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        when(ticketFieldValueConfigResource.getBadValues()).thenReturn(badValues);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenSingleValueInFieldNotExistInAvailableAndAllowAnyNotBlankValueButFieldExistInBadValues() {
        final String fieldValue = "some value";
        final List<String> availableValues = Arrays.asList("value 1", fieldValue);
        final List<String> badValues = Collections.singletonList(fieldValue);

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(Option.of(fieldValue)));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(true);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        when(ticketFieldValueConfigResource.getBadValues()).thenReturn(badValues);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldFalseWhenListValueInFieldNotExistInAvailable() {
        final List<String> availableValues = Collections.singletonList("val_1");
        final ReadOnlyArrayList fieldValues = new ReadOnlyArrayList(new String[]{"val_1", "val_2"});

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(fieldValues));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

    @Test
    public void shouldTrueWhenListValueInFieldIsExistInAvailable() {
        final List<String> availableValues = Arrays.asList("val_1", "val_2");
        final ReadOnlyArrayList fieldValues = new ReadOnlyArrayList(new String[]{"val_1", "val_2"});

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(fieldValues));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        boolean result = job.checkField(issue);
        assertTrue(result);
    }

    @Test
    public void shouldFalseWhenListValueInFieldExistInAvailableButAlsoFieldExistInBadValues() {
        final List<String> availableValues = Arrays.asList("val_1", "val_2");
        final ReadOnlyArrayList fieldValues = new ReadOnlyArrayList(new String[]{"val_1", "val_2"});
        final List<String> badValues = Collections.singletonList("val_2");

        when(issue.getO(FIELD_NAME)).thenReturn(Option.of(fieldValues));
        when(ticketFieldValueConfigResource.isShouldAllowAnyNotBlankValue()).thenReturn(false);
        when(ticketFieldValueConfigResource.getAvailableValues()).thenReturn(availableValues);
        when(ticketFieldValueConfigResource.getBadValues()).thenReturn(badValues);
        boolean result = job.checkField(issue);
        assertFalse(result);
    }

}
