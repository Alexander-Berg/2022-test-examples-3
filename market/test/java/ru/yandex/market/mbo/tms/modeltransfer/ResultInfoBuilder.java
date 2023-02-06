package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;

import java.util.Date;
import java.util.Random;

/**
 * @author dmserebr
 * @date 20.08.18
 */
public class ResultInfoBuilder {
    private ResultInfo resultInfo;

    private static Random random = new Random();

    public static ResultInfoBuilder newBuilder(ResultInfo.Status status) {
        ResultInfoBuilder builder = new ResultInfoBuilder();
        builder.resultInfo = new ResultInfo();
        builder.resultInfo.setId(random.nextLong());
        builder.resultInfo.setStarted(new Date());
        builder.resultInfo.setStatus(status);
        return builder;
    }

    public ResultInfoBuilder resultType(ResultInfo.Type type) {
        resultInfo.setResultType(type);
        return this;
    }

    public ResultInfoBuilder started(Date started) {
        resultInfo.setStarted(started);
        return this;
    }

    public ResultInfoBuilder completed(Date completed) {
        resultInfo.setCompleted(completed);
        return this;
    }

    public ResultInfo build() {
        return resultInfo;
    }
}
