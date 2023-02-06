package onetime.mocks;

import ru.yandex.market.markup2.dao.MarkupDao;
import ru.yandex.market.markup2.loading.MarkupLoader;

import java.util.Set;

/**
 * @author anmalysh
 */
public class ConfigListLoader extends MarkupLoader {

    private Set<Integer> configIds;

    public void setConfigIds(Set<Integer> configIds) {
        this.configIds = configIds;
    }

    @Override
    protected MarkupDao.TaskConfigResult getTaskConfigs() {
        if (configIds == null) {
            return super.getTaskConfigs();
        } else {
            return getMarkupDao().getTaskConfigsByIds(configIds, id -> getTasksCache().getTaskConfigGroup(id));
        }
    }
}
