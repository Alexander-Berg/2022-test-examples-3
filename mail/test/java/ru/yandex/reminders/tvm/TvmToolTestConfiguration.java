package ru.yandex.reminders.tvm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.passport.tvmauth.TvmToolSettings;

@Configuration
public class TvmToolTestConfiguration {
    private static final String TVM_TOOL_PORT = "tvmtool.port";
    private static final String TVM_TOOL_TOKEN = "tvmtool.authtoken";

    private static String readFile(String filename) throws IOException {
        try (val fr = new FileReader(filename);
             val br = new BufferedReader(fr)) {
            return br.readLine();
        }
    }

    @Bean
    public TvmToolData getData() throws IOException {
        return new TvmToolData(Integer.parseInt(readFile(TVM_TOOL_PORT)), readFile(TVM_TOOL_TOKEN));
    }

    @Bean
    @Primary
    public TvmClient getClient(TvmToolData data) {
        return getClient("reminders", data);
    }

    @Bean(name = "dummyClient")
    public TvmClient getDummyClient(TvmToolData data) {
        return getClient("dummy", data);
    }

    @Bean(name = "callmebackClient")
    public TvmClient getCallmebackClient(TvmToolData data) {
        return getClient("callmeback", data);
    }

    private static TvmClient getClient(String alias, TvmToolData data) {
        val settings = new TvmToolSettings(alias);

        settings.setHostname("localhost");
        settings.setAuthToken(data.getToken());
        settings.setPort(data.getPort());

        return new TvmClientImpl(ru.yandex.passport.tvmauth.NativeTvmClient.create(settings));
    }
}
