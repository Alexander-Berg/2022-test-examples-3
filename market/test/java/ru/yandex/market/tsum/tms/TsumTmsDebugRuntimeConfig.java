package ru.yandex.market.tsum.tms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 11/01/2017
 */
@Configuration
@PropertySource({"classpath:tsum-tms-test.properties"})
@Import(TsumDebugRuntimeConfig.class)
@ComponentScan(basePackages = {"ru.yandex.market.tsum.tms.tasks"}, lazyInit = true)
@Lazy
public class TsumTmsDebugRuntimeConfig {
}
