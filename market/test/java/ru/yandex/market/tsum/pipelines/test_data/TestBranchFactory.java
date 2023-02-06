package ru.yandex.market.tsum.pipelines.test_data;

import ru.yandex.market.tsum.clients.github.model.Branch;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.06.17
 */
public class TestBranchFactory {
    private TestBranchFactory() {
    }

    public static Branch branch(String name) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setHtmlLink("https://github.yandex-team.ru/market/market/" + name);
        return branch;
    }
}
