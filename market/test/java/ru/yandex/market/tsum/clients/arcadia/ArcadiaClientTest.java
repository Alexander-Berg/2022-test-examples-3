package ru.yandex.market.tsum.clients.arcadia;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/07/2018
 */
public class ArcadiaClientTest {
    @Test
    public void getDiffStats() throws Exception {
        SVNDiffClient diffClient = Mockito.mock(SVNDiffClient.class);

        Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            OutputStream outputStream = (OutputStream) args[6];
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(
                "Index: report/src/rich_request.cpp\n" +
                    "===================================================================\n" +
                    "--- report/src/rich_request.cpp\t(revision 3796302)\n" +
                    "+++ report/src/rich_request.cpp\t(revision 3796324)\n" +
                    "@@ -149,7 +149,8 @@\n" +
                    "     const TRequestCategoriesClassificationFactors& " +
                    "TRichRequest::getRequestCategoriesClassificationFactors() const {\n" +
                    "         static const TRequestCategoriesClassificationFactors empty;\n" +
                    "\n" +
                    "-        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || Params.Pp().Get() " +
                    "== EPlacement::MARKET_API_ADVISOR) {\n" +
                    "+        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || Params.Pp().Get() " +
                    "== EPlacement::MARKET_API_ADVISOR ||\n" +
                    "+                Params.ReportOutputLogic().Get() != OL_PRIME) {\n" +
                    "             return empty;\n" +
                    "         }"
            );
            writer.close();
            outputStream.close();
            return null;
        }).when(diffClient).doDiff(
            Mockito.any(), Mockito.any(SVNRevision.class), Mockito.any(SVNRevision.class),
            Mockito.any(SVNRevision.class),
            Mockito.any(SVNDepth.class), Mockito.anyBoolean(), Mockito.any()
        );

        Mockito.when(diffClient.getOperationsFactory()).thenReturn(Mockito.mock(SvnOperationFactory.class));

        TrunkArcadiaClient arcadiaClient = new TrunkArcadiaClient("svn+ssh://arcadia.yandex.ru", "", "",
            SVNClientManager.newInstance(null, "username", "password"), SVNWCUtil.createDefaultAuthenticationManager(
            "username", "password"));
        ArcadiaDiffStats diffStats = arcadiaClient.getDiffStats(42, diffClient);

        Assert.assertEquals(new ArcadiaDiffStats(2, 1, 1), diffStats);
    }
}
