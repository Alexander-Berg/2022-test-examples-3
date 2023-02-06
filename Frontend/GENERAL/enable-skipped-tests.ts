import { Command, flags } from '@oclif/command';
import { Env, exec } from '@yandex-int/frontend.ci.utils';
import { configureApiController, UnsafeReviewRequestsApi } from '@yandex-int/si.ci.arcanum-api';

import { getChangedPackagesSinceCommit } from '../utils/getChangedPackages';

const getArcIssueKeys = async(prNumber: number): Promise<string> => {
    const reviewRequestsApi = configureApiController(
        UnsafeReviewRequestsApi, undefined, process.env.ARCANUM_API_TOKEN
    );

    const { body: { data: issues = [] } } = await reviewRequestsApi.getReviewRequestIssues(prNumber);

    return issues.toString();
};

// документация по testcop - https://github.yandex-team.ru/search-interfaces/testcop#testcop
export default class EnableSkippedTestsCommand extends Command {
    static description = 'Расскипывает тесты через testcop';

    static flags = {
        prNumber: flags.string({
            description: 'Номер PR\'а, по которому расскипываем тест',
            required: true,
        }),
        branchName: flags.string({
            description: 'Имя ветки',
            required: true,
        }),
        since: flags.string({
            description: 'SHA коммита, от которого вычисляется набор листьев для расскипывания тестов в них',
            required: true,
        }),
    };

    async run() {
        const { flags: { since, branchName, prNumber } } = this.parse(EnableSkippedTestsCommand);
        const changedPackages = getChangedPackagesSinceCommit(since);

        const testcopProjects = changedPackages
            .map(changedPackage => changedPackage.name.split('/').slice(-1))
            .join(',');

        if (!testcopProjects) {
            return;
        }

        const cmdOptions = [
            `--projects=${testcopProjects}`,
            `--branch=${branchName}`,
            `--pull-request-number=${prNumber}`
        ];

        if (Env.getVCS() === 'arc') {
            const issues = await getArcIssueKeys(Number(prNumber));

            if (!issues) { // true for TRIVIAL review-requests
                return;
            }

            cmdOptions.push(`--merge-commit-title=${issues}`);
        }

        exec(`npx testcop-cli enable ${cmdOptions.join(' ')}`);
    }
}
