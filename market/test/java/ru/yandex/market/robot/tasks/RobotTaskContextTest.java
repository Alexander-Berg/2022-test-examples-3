package ru.yandex.market.robot.tasks;

import ru.yandex.market.robot.db.sqlite.SqliteLocalStorage;
import ru.yandex.market.robot.shared.models.Entity;
import ru.yandex.market.robot.shared.models.RobotTaskInfo;
import ru.yandex.market.robot.shared.models.Source;

import java.util.HashMap;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 26.06.2013
 */
public class RobotTaskContextTest {

    public static RobotTaskContext createExampleContext() {
        return new RobotTaskContext(
            new RobotTaskInfo(),
            new Source(0, "http://ya.ru"),
            new Entity(),
            new HashMap<>(),
            new SqliteLocalStorage()
        );
    }
}