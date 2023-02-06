package ru.yandex.antifraud;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.antifraud.currency.CurrencyRateMap;
import ru.yandex.antifraud.lua_context_manager.CurrenciesRatesTuner;
import ru.yandex.antifraud.lua_context_manager.PrototypesManager;
import ru.yandex.antifraud.lua_context_manager.UaTraitsTuner;
import ru.yandex.antifraud.lua_context_manager.config.ImmutablePrototypesConfig;
import ru.yandex.antifraud.lua_context_manager.config.PrototypesConfigBuilder;
import ru.yandex.test.util.TestBase;

public class PrototypesTest extends TestBase {
    @Test
    public void test() throws Exception {
        System.setProperty("SERVICES_MAPPING_PATH", resource("services_mapping.json/0_upload_file").toAbsolutePath().toString());
        System.setProperty("FPAY_PUSH_NOTIFICATION_TEMPLATE_PATH", resource("PushNotification.json/new_data").toAbsolutePath().toString());

        final Path rulesConfigPath = resource("mail/so/daemons/antifraud/rules/channels.conf");
        final Path uaTraitsPath = resource("metrika/uatraits/data/browser.xml");
        final Path currenciesRatesPath = resource("currencies_rate.json.txt/0_upload_file");

        final ImmutablePrototypesConfig prototypesConfig = new PrototypesConfigBuilder(rulesConfigPath,
                rulesConfigPath.getParent()).build();
        final CurrencyRateMap currenciesRatesMap = CurrencyRateMap.make(currenciesRatesPath);

        final UaTraitsTuner uaTraitsTuner = Optional.ofNullable(uaTraitsPath).map(UaTraitsTuner::new).orElse(null);
        new PrototypesManager(
                prototypesConfig,
                new HashMap<>(),
                uaTraitsTuner,
                new CurrenciesRatesTuner(currenciesRatesMap));
    }
}
