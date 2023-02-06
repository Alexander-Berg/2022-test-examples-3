package ru.yandex.market.notification.safe.model.vo;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Unit-тесты для {@link SpamCheckInfo}.
 *
 * @author Vladislav Bauer
 */
public class SpamCheckInfoTest {

    @Test
    public void testBasicMethods() {
        var data = new SpamCheckInfo(1, 2);
        var sameData = new SpamCheckInfo(1, 2);
        var otherData = new SpamCheckInfo(2, 1);

        checkBasicMethods(data, sameData, otherData);
    }

    private <T> void checkBasicMethods(@Nonnull T object, @Nonnull T same, @Nonnull T other) {
        assertThat(object.toString(), not(isEmptyOrNullString()));

        assertThat(object.equals(new Object()), equalTo(false));
        assertThat(object, not(equalTo(other)));
        assertThat(object, equalTo(same));
        assertThat(object.hashCode(), equalTo(same.hashCode()));
        assertThat(object.toString(), equalTo(same.toString()));
    }
}
