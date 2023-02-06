package ru.yandex.market.abo.gen.ml;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.ml.core.NirvanaMLCoreManager;
import ru.yandex.market.abo.core.ml.core.SuspiciousOffer;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 * @date 27.03.18.
 */
public class NirvanaTicketGeneratorTest {
    public static final int AMOUNT_UNIQUE_SHOP_IDS = 1;

    @InjectMocks
    private NirvanaTicketGenerator nirvanaGenerator;

    @Mock
    private NirvanaMLCoreManager nirvanaMLCoreManager;

    private List<SuspiciousOffer> offers;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        nirvanaGenerator.configure(new GeneratorProfile(100, 50, ""));
    }

    @Test
    public void testGenerateOnRandomShopIds() {
        offers = new Random().ints(1000)
                .mapToObj(random -> new SuspiciousOffer(random, "any", 0.0))
                .collect(Collectors.toList());

        when(nirvanaMLCoreManager.loadResult()).thenReturn(offers);
        List<Hypothesis> hypotheses = nirvanaGenerator.generate();
        assertEquals(nirvanaGenerator.maxPerDay, hypotheses.size());
    }

    @Test
    public void testGenerateOnIdenticalShopIds() {
        offers = new Random().ints(1000)
                .mapToObj(r -> new SuspiciousOffer(1, "any", 0.0))
                .collect(Collectors.toList());

        when(nirvanaMLCoreManager.loadResult()).thenReturn(offers);
        List<Hypothesis> hypotheses = nirvanaGenerator.generate();
        assertEquals(AMOUNT_UNIQUE_SHOP_IDS, hypotheses.size());
    }
}
