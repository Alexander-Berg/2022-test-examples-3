package ru.yandex.market.jmf.module.comment.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.module.comment.ModuleCommentConfiguration;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.mail.test.ModuleMailTestConfiguration;
import ru.yandex.market.jmf.module.xiva.ModuleXivaTestConfiguration;
import ru.yandex.market.jmf.ui.test.UiTestConfiguration;

@Import({
        ModuleCommentConfiguration.class,
        UiTestConfiguration.class,
        ModuleMailTestConfiguration.class,
        ModuleXivaTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.module.comment.test.impl")
public class ModuleCommentTestConfiguration {

    @Bean
    public CommentTestUtils commentTestUtils(DbService dbService) {
        return new CommentTestUtils(dbService);
    }
}
