package ru.yandex.direct.bannersystem.model.exporttable;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// ssh -L 9874:bssoap.yandex.ru:81
// wget -qO - 'http://bssoap.yandex.ru:81/export/export-table.cgi?table_name=Uniword' | jq
@RunWith(Parameterized.class)
public class UniwordRecordTest {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"по", "stop,bt-wide,bt-lonely-bad,bt-nastenka-wide,bt-cp-bad,net-keyword-bad", true},
                {"чего", "stop", true},
                {"ту", "bt-wide,bt-nastenka-wide,bt-cp-bad,stop", true},
                {"тому", "bt-wide,stop,bt-lonely-bad", true},
                {"ст", "end-sentence-exclusion", false},
                {"p0rno", "porno,tragic", false},
        });
    }

    @Parameterized.Parameter(0)
    public String data;

    @Parameterized.Parameter(1)
    public String options;

    @Parameterized.Parameter(2)
    public boolean isStopWord;

    @Test
    public void isStopWord() {
        assertThat(new UniwordRecord(data, options).isStopWord(), is(isStopWord));
    }
}

