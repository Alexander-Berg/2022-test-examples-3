package ru.yandex.market.sre.services.tms.eventdetector.dao.repository;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.ServiceIndicator;
import ru.yandex.market.sre.services.tms.eventdetector.dataloaders.graphite.GraphiteLoader;
import ru.yandex.market.sre.services.tms.eventdetector.enums.IndicatorSource;
import ru.yandex.market.sre.services.tms.eventdetector.model.graphite.GraphiteResponseItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ServiceIndicatorRepositoryTest {

    @Test
    public void getAll() {
        List<ServiceIndicator> list = ServiceIndicatorRepository.getAll();
        long uniqIds = list.stream().map(ServiceIndicator::getId).distinct().count();
        assertEquals(uniqIds, list.size());
    }

    @Test
    @Ignore("For development checks only")
    public void checkTargets() {
        List<ServiceIndicator> list = ServiceIndicatorRepository.getAll();
        list.forEach(indicator -> {
            if (indicator.getIndicatorSource() == IndicatorSource.SOLOMON) {
                return;
            }
            String link = link(indicator);
            try {
                String json = checkTarget(link);
                System.out.println(json);
                GraphiteResponseItem.parse(json);
            } catch (Exception e) {
                e.printStackTrace();
                fail(indicator.getId() + ": " + e.getLocalizedMessage());
            }
        });
    }

    private String link(ServiceIndicator indicator) {
        return "https://market-graphite.yandex-team.ru/render?from=-1hours&format=json&until=now-5minutes&target=" + indicator.getTargets().get(1);
    }

    private String checkTarget(String link) throws Exception {
        try {
            System.out.println(link);
            return GraphiteLoader.sendGet(link);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
