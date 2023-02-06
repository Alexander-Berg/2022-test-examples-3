package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AddVCardsDelegateToWorktimeTest {
    @Parameterized.Parameter
    public String worktime;
    @Parameterized.Parameter(value = 1)
    public String expectedWorktime;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{null, null},
                new Object[]{"1;2;3;4;5;6;7;8", "1#2#3#4#5#6;7#8"},
                new Object[]{"1;2;3;4;5;6;7;8;", "1#2#3#4#5#6;7#8"},
                new Object[]{"0;3;10;00;18;00;4;6;10;00;11;00", "0#3#10#00#18#00;4#6#10#00#11#00"});
    }

    @Test
    public void test() {
        String actualWorktime = AddVCardsDelegate.toVcardWorktime(worktime);

        assertThat(actualWorktime).isEqualTo(expectedWorktime);
    }
}
