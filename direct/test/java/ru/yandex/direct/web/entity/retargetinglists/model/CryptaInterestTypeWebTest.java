package ru.yandex.direct.web.entity.retargetinglists.model;

import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;

@RunWith(Parameterized.class)
public class CryptaInterestTypeWebTest {
    @Parameterized.Parameters(name = "Тип InterestType: {0}")
    public static Collection<Object[]> data() {
        return StreamEx.of(CryptaInterestType.values())
                .append((CryptaInterestType) null)
                .map(interestType -> new Object[]{interestType})
                .toList();
    }

    @Parameterized.Parameter(0)
    public CryptaInterestType type;

    @Test
    public void fromMetrikaRetargetingConditionType() {
        CryptaInterestTypeWeb.fromCoreType(type);
    }
}
