package ru.yandex.direct.i18n.tanker.test;

import java.util.UUID;

import ru.yandex.direct.i18n.tanker.Branch;
import ru.yandex.direct.i18n.tanker.Tanker;
import ru.yandex.direct.i18n.tanker.TankerWithBranch;

public class TmpBranchEnv implements Env {
    private Branch branch;
    private Tanker tanker;

    public TmpBranchEnv(Tanker tanker) {
        this.branch = new Branch("TankerTest-" + UUID.randomUUID().toString(), "master");
        this.tanker = tanker;
        tanker.createBranch(branch);
    }

    @Override
    public void close() throws Exception {
        tanker.deleteBranch(branch.getName());
    }

    @Override
    public Branch getBranch() {
        return branch;
    }

    @Override
    public Tanker getTanker() {
        return tanker;
    }

    @Override
    public TankerWithBranch getTankerWithBranch() {
        return tanker.withBranch(branch.getName());
    }
}
