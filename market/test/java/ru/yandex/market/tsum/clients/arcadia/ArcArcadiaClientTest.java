package ru.yandex.market.tsum.clients.arcadia;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import io.grpc.ManagedChannelBuilder;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.arc.api.Repo;
import ru.yandex.arc.api.Shared;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.grpc.trace.TraceClientInterceptor;

@Ignore
public class ArcArcadiaClientTest extends TestCase {
    private final ArcArcadiaClient client = new ArcArcadiaClient(
        ManagedChannelBuilder.forTarget("api.arc-vcs.yandex-team.ru:6734")
            .intercept(new TraceClientInterceptor(Module.ARCADIA))
            .build(),
        "**"
    );

    @Test
    public void testLog() {
        Shared.Commit head = client.getHead("trunk", "market/infra/tsum");
        System.out.println(head);
    }

    @Test
    public void testCommit() {
        Shared.Commit commit = client.getCommit("0d793c61df26f3bcf3bbc94221013846b613c1c0");
        System.out.println(commit);
        System.out.println(commit.getAuthor());
        System.out.println(Instant.ofEpochSecond(commit.getTimestamp().getSeconds()));

        List<Repo.DiffstatResponse.DiffstatFile> diffStatFiles = client.getDiffStatFiles(
            "0d793c61df26f3bcf3bbc94221013846b613c1c0"
        );
        System.out.println(diffStatFiles);

    }

    @Test
    public void testBranches() {
        Iterator<Repo.ListRefsResponse> branches = client.getBranches("trunk", null, null);
        System.out.println(branches.next().getRefsList().get(0));
    }
}
