package ru.yandex.personal.mail.search.metrics.scraper.services.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.metrics.basket.BasketQuery;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.serp.Serp;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.serp.SerpComponent;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.serp.SerpJudgement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerpEvaluationTest {
    private final Random r = new Random();

    @Test
    public void createMarkedComponents() {
        Evaluation ms = new SerpEvaluation();

        List<SerpComponent> components = new ArrayList<>();
        List<String> relevantIds = new ArrayList<>();
        List<SerpJudgement> expected = new ArrayList<>();

        long mark = 0;
        for (int i = 0; i < 100; i++) {
            mark += 1000 + r.nextInt(100_000);
            components.add(SerpComponent.searchComponent("", "", "", Instant.ofEpochSecond(mark)));

            if (r.nextBoolean()) {
                relevantIds.add(String.valueOf(mark));
                expected.add(SerpJudgement.PASSED);
            } else {
                relevantIds.add(String.valueOf(mark + 1));
                expected.add(SerpJudgement.FAILED);
            }
        }

        BasketQuery bq = new BasketQuery("", relevantIds);

        Serp serp = Serp.suggestResult(components, null);
        serp.setComponents(ms.markedSerpComponentns(serp, bq));

        List<SerpComponent> markedComponents = serp.getComponents();
        for (int i = 0; i < markedComponents.size(); i++) {
            assertEquals(expected.get(i), markedComponents.get(i).getTestResult());
        }
    }
}
