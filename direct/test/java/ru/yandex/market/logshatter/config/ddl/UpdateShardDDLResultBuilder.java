package ru.yandex.market.logshatter.config.ddl;

import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateShardDDLResult;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateShardDDLTask;

import java.util.Collections;
import java.util.List;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.01.17
 */
public abstract class UpdateShardDDLResultBuilder<T extends UpdateShardDDLResultBuilder> {
    private UpdateShardDDLTask task;

    public T withTask(UpdateShardDDLTask task) {
        this.task = task;
        return (T) this;
    }

    public abstract UpdateShardDDLResult build();

    protected UpdateShardDDLTask getTask() {
        return task;
    }

    public static class Success extends UpdateShardDDLResultBuilder<Success> {
        public UpdateShardDDLResult build() {
            return new UpdateShardDDLResult.Success(getTask(), Collections.emptyList());
        }
    }

    public static class Failure extends UpdateShardDDLResultBuilder {
        List<UpdateDDLException> exceptions;

        public Failure withExceptions(List<UpdateDDLException> connectionExceptions) {
            this.exceptions = connectionExceptions;
            return this;
        }

        public UpdateShardDDLResult build() {
            return new UpdateShardDDLResult.Failure(getTask(), exceptions);
        }
    }

    public static class PartialSuccess extends UpdateShardDDLResultBuilder {
        List<UpdateDDLException> exceptions;

        public PartialSuccess withExceptions(List<UpdateDDLException> connectionExceptions) {
            this.exceptions = connectionExceptions;
            return this;
        }

        public UpdateShardDDLResult build() {
            return new UpdateShardDDLResult.PartialSuccess(getTask(), exceptions);
        }
    }

    public static class ManualDDLRequired extends UpdateShardDDLResultBuilder {
        private List<DDL> manualDDLs;

        public ManualDDLRequired withManualDDLs(List<DDL> manualDDLs) {
            this.manualDDLs = manualDDLs;
            return this;
        }

        public UpdateShardDDLResult build() {
            return new UpdateShardDDLResult.ManualDDLRequired(getTask(), manualDDLs);
        }
    }
}
