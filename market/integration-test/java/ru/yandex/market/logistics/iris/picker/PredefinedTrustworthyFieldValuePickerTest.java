package ru.yandex.market.logistics.iris.picker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistics.iris.core.domain.item.Item;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.picker.predefined.BarcodesFieldValuePicker;
import ru.yandex.market.logistics.iris.picker.predefined.PredefinedTrustworthyFieldValuePicker;
import ru.yandex.market.logistics.iris.picker.predefined.PriorityBasedFieldValuePicker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PredefinedTrustworthyFieldValuePickerTest {

    @Mock
    private BarcodesFieldValuePicker barcodesFieldValuePickerMock;
    @Mock
    private PriorityBasedFieldValuePicker priorityBasedFieldValuePickerMock;

    @InjectMocks
    private PredefinedTrustworthyFieldValuePicker picker;

    @Test
    public void priorityBasedPickerIsSelected() {
        Item item = new Item(null, null);
        picker.pick(PredefinedFields.LIFETIME_DAYS_FIELD, item);

        verify(priorityBasedFieldValuePickerMock, times(1)).pick(PredefinedFields.LIFETIME_DAYS_FIELD, item);
        verifyZeroInteractions(barcodesFieldValuePickerMock);
    }


    @Test
    public void barcodesBasedPickerIsSelected() {
        Item item = new Item(null, null);
        picker.pick(PredefinedFields.BARCODES, item);

        verify(barcodesFieldValuePickerMock, times(1)).pick(item);
        verifyZeroInteractions(priorityBasedFieldValuePickerMock);
    }
}
