package ru.yandex.chemodan.app.psbilling.core.products.selectors;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;

public class ExperimentIsActiveSelectorTest extends AbstractPsBillingCoreTest {
    @Autowired
    private ProductLineSelectorFactory productLineSelectorFactory;
    @Autowired
    private ExperimentsManager experimentsManagerMock;
    private static final String EXP = "EXP";

    @Test
    public void available_contextExp() {
        testImpl(true, false, true);
    }

    @Test
    public void available_expManagerExp() {
        testImpl(false, true, true);
    }

    @Test
    public void available_bothExp() {
        testImpl(true, true, true);
    }

    @Test
    public void notAvailable_noExps() {
        testImpl(false, false, false);
    }

    private void testImpl(boolean expInContext, boolean expInManager, boolean availableExpected) {
        SelectionContext selectionContext;
        if (expInContext) {
            selectionContext = new SelectionContext(Cf.list(EXP));
        } else {
            selectionContext = new SelectionContext();
        }
        if (expInManager) {
            Mockito.when(experimentsManagerMock.getFlags(uid.getUid()))
                    .thenReturn(Cf.list(EXP));
        }

        ProductLineEntity line = psBillingProductsFactory.createProductLine(UUID.randomUUID().toString());

        Assert.assertEquals(availableExpected,
                productLineSelectorFactory.experimentIsActiveSelector(EXP).isAvailable(line, uidO, Option.empty(),
                        selectionContext).isAvailable());
    }
}
