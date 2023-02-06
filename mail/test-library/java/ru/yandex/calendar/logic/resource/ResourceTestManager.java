package ru.yandex.calendar.logic.resource;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.ResourceFields;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.resource.schedule.ResourceScheduleDao;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.misc.TranslitUtils;

/**
 * @author Stepan Koltsov
 */
public class ResourceTestManager {
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private ResourceScheduleDao resourceScheduleDao;

    public void clearResourcesAndOffices(PassportDomain domain) {
        eventResourceDao.deleteEventResourcesByDomain(domain);
        resourceScheduleDao.deleteResourceSchedulesByResourceIds(
                resourceDao.findResourcesByDomain(domain).map(ResourceFields.ID.getF()));
        resourceDao.deleteResourcesByDomain(domain);
        resourceDao.deleteOfficesByDomain(domain);
    }

    private Office office(String name, PassportDomain domain) {
        Office o = new Office();
        o.setName(name);
        o.setNameEn(TranslitUtils.translit(name));
        o.setDomain(domain.getDomain().getDomain());
        return o;
    }

    public ListF<Office> saveSomeOffices(final PassportDomain domain) {
        String[] offices = {
                "Москва, Красная Роза",
                "Москва, Красная Роза",
                "Москва, Самокатная",
                "Москва, Новые черемушки",
                "Москва, Останкино",
                "Санкт-Петербург, БЦ Бенуа",
                "Екатеринбург",
                "Украина, Одесса",
                "Украина, Киев",
                "Украина, Симферополь",
                "США, Калифорния",
                "Датацентр Вавилова",
                "Датацентр Нижегородская",
                "Датацентр Угрешская",
                "Датацентр Ивантеевка",
                "Надомник",
                "Датацентр Мытищи",
                "Планы на временую пересадку",
                "Москва, Красная Роза-1",
                "Новосибирск",
                "Казань",
                "Виртуальный офис",
                "Турция, Стамбул",
        };
        return Cf.x(offices).map(new Function<String, Office>() {
            public Office apply(String officeName) {
                return resourceDao.saveOffice(office(officeName, domain));
            }
        });
    }
} //~
