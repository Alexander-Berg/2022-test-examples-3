package ru.yandex.market.jmf.module.comment.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleCommentTestConfiguration.class)
public class InternalModuleCommentTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleCommentTestConfiguration() {
        super("module/comment/test");
    }
}
