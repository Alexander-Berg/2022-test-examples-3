package ru.yandex.market.checkout.carter.web.logging;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordConverter.getFeeFromFeeShow;

/**
 * Created by asafev on 13/06/2017.
 */
public class CartLogRecordConverterFeeShowTest {

    @Test
    public void getFeeFromCorrectFeeShowParseTest() {
        byte[] feeShow = new byte[]{
                '\u0010', '\u0005', '0', '.', '0', '1', '0',
                '\u0018', '\u0004', '1', '.', '6', '1',
                '\u001A', '\u0011', '9', '1', '6', '9', '5', '4', '9', '1', '3', '8', '5', '6', '3', '3', '4', '0', '8',
                '\u0022', '\u0016', '4', '5', '8', 'i', 'A', 'O', 'U', 'x', 'c', 'l', 'p', 'j', '9', 'D', '1', 'N',
                'O', 'M', 's', 'S', 'A'
        };
        String fee = getFeeFromFeeShow(feeShow);
        assertThat(fee, equalTo("1.61"));
    }

    @Test
    public void getFeeFromNullFeeShowParseTest() {
        byte[] feeShow = null;
        String fee = getFeeFromFeeShow(feeShow);
        assertThat(fee, nullValue());
    }

    @Test
    public void getFeeFromEmptyFeeShowParseTest() {
        byte[] feeShow = new byte[]{};
        String fee = getFeeFromFeeShow(feeShow);
        assertThat(fee, nullValue());
    }

    @Test
    public void getFeeFromFeeShowWithLettersParseTest() {
        byte[] feeShow = new byte[]{
                '\u0010', '\u0005', '0', '.', '0', '1', '0',
                '\u0018', '\u0004', 'A', 'B', 'C', 'D',
                '\u001A', '\u0011', '9', '1', '6', '9', '5', '4', '9', '1', '3', '8', '5', '6', '3', '3', '4', '0', '8',
                '\u0022', '\u0016', '4', '5', '8', 'i', 'A', 'O', 'U', 'x', 'c', 'l', 'p', 'j', '9', 'D', '1', 'N',
                'O', 'M', 's', 'S', 'A'
        };
        String fee = getFeeFromFeeShow(feeShow);
        assertThat(fee, nullValue());
    }
}
