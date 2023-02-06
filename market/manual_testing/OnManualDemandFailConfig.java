package ru.yandex.market.tsum.pipelines.mbo.jobs.manual_testing;

import java.util.UUID;

import org.springframework.data.annotation.PersistenceConstructor;

import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.ResourceField;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.ResourceInfo;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.TextField;

/**
 * @author s-ermakov
 */
@ResourceInfo(title = "Manual demand config")
public class OnManualDemandFailConfig implements Resource {

    @ResourceField(
        title = "Fail command",
        description = "Строка, которая должна быть в ченджлоге, чтобы джоба упала. " +
            "Например: '/stop', '/fail', '/manual', 'stop_the_world' и другое."
    )
    @TextField(required = true)
    private String failCommand;

    @ResourceField(
        title = "Fail message",
        description = "Сообщение, которое будут отображаться пользователю, если ченджлог содержит fail command."
    )
    @TextField(defaultValue = "Changelog contains fail command.", multiline = true)
    private String failMessage;

    @PersistenceConstructor
    public OnManualDemandFailConfig() {
    }

    public String getFailCommand() {
        return failCommand;
    }

    public String getFailMessage() {
        return failMessage;
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("04d42bae-18e5-482f-b639-f4aac8f81819");
    }
}
