package ru.yandex.market.tsum.clients.mdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;

import yandex.cloud.api.iam.v1.IamTokenServiceGrpc;
import ru.yandex.market.tsum.clients.iam.IamClientAuthInterceptor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@Ignore("integration test")
public class MdbClientIntegrationTest {
    private static final String CLOUD_MDB_URL = "gw.db.yandex-team.ru";

    private static final String EXISTING_DB_NAME = "market_infra_graphite_pg";
    private static final String NON_EXISTING_DB_NAME = "bogus_name";
    private static final String EXISTING_ABC_SLUG = "marketinfra";
    private static final String NON_EXISTING_ABC_SLUG = "bogus-slug";
    private static final String EXISTING_FOLDER_ID = "fooiglq05bsfrq012h1k";

    private static final MdbClient MDB_CLIENT;

    static {
        // https://oauth.yandex-team.ru/authorize?response_type=token&client_id=8cdb2f6a0dca48398c6880312ee2f78d
        String mdbToken = getToken(".mdb/token");

        Channel channel = ManagedChannelBuilder.forTarget(CLOUD_MDB_URL).build();
        IamTokenServiceGrpc.IamTokenServiceBlockingStub iamClient =
            IamTokenServiceGrpc.newBlockingStub(channel);

        IamClientAuthInterceptor authInterceptor = new IamClientAuthInterceptor(mdbToken, iamClient);
        Channel mdbChannel = ManagedChannelBuilder.forTarget(CLOUD_MDB_URL)
            .intercept(authInterceptor)
            .build();
        MDB_CLIENT = new MdbClient(mdbChannel);
    }

    private static String getToken(String path) {
        try {
            // TODO поменять на Files.readString, когда не надо будет поддерживать Java 8
            //noinspection ReadWriteStringCanBeUsed
            return new String(
                Files.readAllBytes(FileSystems.getDefault().getPath(System.getenv("HOME"), path)),
                StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void existingFolder() {
        assertThat(MDB_CLIENT.getFolderIdByAbcSlugOptional(EXISTING_ABC_SLUG),
            equalTo(Optional.of(EXISTING_FOLDER_ID)));
    }

    @Test
    public void nonExistingFolder() {
        assertThat(MDB_CLIENT.getFolderIdByAbcSlugOptional(NON_EXISTING_ABC_SLUG),
            equalTo(Optional.empty()));
    }

    @Test
    public void existingDb() {
        assertThat(MDB_CLIENT.dbNameExists(EXISTING_FOLDER_ID, EXISTING_DB_NAME), is(true));
    }

    @Test
    public void nonExistingDb() {
        assertThat(MDB_CLIENT.dbNameExists(EXISTING_FOLDER_ID, NON_EXISTING_DB_NAME), is(false));
    }
}
