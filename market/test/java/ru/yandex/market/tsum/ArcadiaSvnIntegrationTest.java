package ru.yandex.market.tsum;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 31/10/2019
 */
public class ArcadiaSvnIntegrationTest {

    @Test
    @Ignore
    public void test() throws SVNException {
        ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(
            null, "robot-market-infra", null, new File("/tmp/rbt5/id_rsa"), "", false
        );
        /*ISVNAuthenticationManager auth = SVNWCUtil.createDefaultAuthenticationManager(
            null, "firov", null, new File("/Users/firov/.ssh/id_rsa"), "", false
        );*/

        SVNClientManager clientManager = SVNClientManager.newInstance(null, auth);

        SVNURL url = SVNURL.parseURIEncoded("svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia");
        clientManager.getLogClient().doLog(
            url, new String[]{"/"},
            SVNRevision.UNDEFINED,
            SVNRevision.create(-1),
            SVNRevision.create(-1),
            false,
            false,
            1L, logEntry -> {
            });
    }
}
