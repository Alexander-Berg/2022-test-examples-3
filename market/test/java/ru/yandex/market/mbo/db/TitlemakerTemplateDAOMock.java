package ru.yandex.market.mbo.db;

import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class TitlemakerTemplateDAOMock extends TitlemakerTemplateDao {

    private final TovarTreeDao tovarTreeDao;
    private final Map<Long, TMTemplate> templateMap = new HashMap<>();

    public TitlemakerTemplateDAOMock(TovarTreeDao tovarTreeDao) {
        super(null, null);
        this.tovarTreeDao = tovarTreeDao;
    }

    public void addTemplate(Long hid, TMTemplate tmTemplate) {
        tmTemplate.setHid(hid);
        templateMap.put(hid, tmTemplate);
    }

    @Override
    public TMTemplate loadTemplateByHid(long hid) {
        return templateMap.get(hid);
    }


    @Override
    public List<TMTemplate> loadSubtreeTemplatesByHid(long hid) {
        return tovarTreeDao.loadTovarTree().findByHid(hid).findAll(tc -> true).stream()
            .map(tcn -> templateMap.get(tcn.getData().getHid()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
