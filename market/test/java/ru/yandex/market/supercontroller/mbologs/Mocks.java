package ru.yandex.market.supercontroller.mbologs;

import org.mockito.Mockito;
import ru.yandex.market.supercontroller.mbologs.dao.Generations;
import ru.yandex.market.supercontroller.mbologs.model.TableInfo;

import java.util.Arrays;
import java.util.List;

/**
 * @author amaslak
 */
public class Mocks {

    public static final List<String> SESSIONS = Arrays.asList("20131010_1011", "20131010_1012", "20131010_1013");

    private Mocks() {
    }

    public static Generations mockGenerations() {
        Generations generationsMock = Mockito.mock(Generations.class);
        Mockito.doCallRealMethod().when(generationsMock).fillTableInfo(
            Mockito.any(TableInfo.class), Mockito.anyBoolean()
        );
        Mockito.doAnswer(i -> SESSIONS).when(generationsMock).getAllSessionIds(Mockito.anyString());
        return generationsMock;
    }

}
