package ru.yandex.crypta.graph.engine.exp.stats;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.engine.exp.stats.ops.CollectStatsByMergeKeyReducer;
import ru.yandex.crypta.graph.engine.exp.stats.ops.ComponentScore;
import ru.yandex.crypta.graph2.dao.Dao;
import ru.yandex.crypta.graph2.dao.yql.Yql;
import ru.yandex.crypta.graph2.dao.yql.YqlConfig;
import ru.yandex.crypta.graph2.dao.yt.YtConfig;
import ru.yandex.crypta.graph2.dao.yt.ops.YtOpsParams;
import ru.yandex.crypta.graph2.utils.NativeLibHelper;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;


public class TestRun {
    public static void main(String[] args) {

        Yt yt = YtConfig.getYt(true);
        Yql yql = YqlConfig.getYql();
        NativeLibHelper.setLocalJavaLibraryPath();
        Dao dao = new Dao(yt, yql, new YtOpsParams());

        run(dao);
    }

    static public void run(Dao dao) {

        YPath workdir = YPath.simple("//home/crypta/team/atanna/scoring_strategy_native");

        YPath result = workdir.child("result");
        dao.ytCypress().createTableWithSchema(result, ComponentScore.class);

        dao.ytOps().reduceSync(
                Cf.list(
                        YPath.simple("//home/crypta/production/state/graph/v2exp/matching/workdir/prepare/components_by_merge_key")
                ),
                Cf.list(result),
                Cf.list("merge_key"),
                new CollectStatsByMergeKeyReducer()
        );
   }
}
