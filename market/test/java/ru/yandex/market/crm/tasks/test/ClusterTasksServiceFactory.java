package ru.yandex.market.crm.tasks.test;

import java.util.Collection;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;
import ru.yandex.market.mcrm.tx.TxService;

/**
 * @author apershukov
 */
@Component
public class ClusterTasksServiceFactory {

    private final ClusterTasksDAO tasksDAO;
    private final TaskIncidentsDAO taskIncidentsDAO;
    private final JsonDeserializer jsonDeserializer;
    private final JsonSerializer jsonSerializer;
    private final TxService txService;

    public ClusterTasksServiceFactory(ClusterTasksDAO tasksDAO,
                                      TaskIncidentsDAO taskIncidentsDAO,
                                      JsonDeserializer jsonDeserializer,
                                      JsonSerializer jsonSerializer,
                                      TxService txService) {
        this.tasksDAO = tasksDAO;
        this.taskIncidentsDAO = taskIncidentsDAO;
        this.jsonDeserializer = jsonDeserializer;
        this.jsonSerializer = jsonSerializer;
        this.txService = txService;
    }

    public ClusterTasksService create(Collection<Task<Void, ?>> tasks) {
        return new ClusterTasksService(
                tasksDAO,
                taskIncidentsDAO,
                jsonDeserializer,
                jsonSerializer,
                txService,
                tasks
        );
    }
}
