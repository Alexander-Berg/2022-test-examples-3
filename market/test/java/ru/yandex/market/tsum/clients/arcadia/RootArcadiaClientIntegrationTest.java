package ru.yandex.market.tsum.clients.arcadia;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@Ignore("integration test")
public class RootArcadiaClientIntegrationTest {
    //значения пропертей можно брать, например, отсюда:
    //https://yav.yandex-team.ru/secret/sec-01d4mwzv17f5a7ba3y3sgcm4r4/explore/version/head
    private static final ISVNAuthenticationManager AUTHENTICATION_MANAGER = createPrivateKeySvnAuth(
        "${tsum.robot.login}",
        "${tsum.arcadia.privateKey}",
        "${tsum.robot.password}"
    );
    private static final SVNClientManager SVN_CLIENT_MANAGER =
        SVNClientManager.newInstance(new DefaultSVNOptions(), AUTHENTICATION_MANAGER);

    public static final String ARCANUM_OAUTH_TOKEN = "";
    private static final RootArcadiaClient CLIENT = new RootArcadiaClient(
        "svn+ssh://arcadia.yandex.ru", "/arc/trunk/arcadia",
        ARCANUM_OAUTH_TOKEN, SVN_CLIENT_MANAGER, AUTHENTICATION_MANAGER);

    @Test
    public void testPathPresent() {
        boolean pathPresent = CLIENT.isPathPresent("/trunk/arcadia/market/infra/tsum");
        System.out.println(pathPresent);
        assertThat(pathPresent, Matchers.is(true));
    }

    private static ISVNAuthenticationManager createPrivateKeySvnAuth(
        String robotLogin, String privateKeyEncoded, String privateKeyPassword
    ) {
        String privateKeyDecoded = privateKeyEncoded.replace("|", "\n");
        String md5 = DigestUtils.md5Hex(privateKeyDecoded);
        File privateKeyFile = new File(FileUtils.getTempDirectoryPath(), md5);
        if (!privateKeyFile.exists()) {
            try {
                FileUtils.writeStringToFile(privateKeyFile, privateKeyDecoded, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create key file", e);
            }
        }

        return SVNWCUtil.createDefaultAuthenticationManager(
            null, robotLogin, null, privateKeyFile, privateKeyPassword, false
        );
    }

}
