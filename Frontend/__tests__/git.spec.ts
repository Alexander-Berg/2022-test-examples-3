import { Git } from '../git';

const since = 'd65402f0e778f1f7fedd2b1c23e6123e55bb82df';
const till = 'HEAD';

/**
 * В основе история коммитов из маленького репозитория
 * @see: https://github.yandex-team.ru/lego/anulus/tree/13c6002d2c1e6bc388986d1dd4a826625bbfafb5
 */
describe('git', () => {
    const OLD_ENV = process.env;
    beforeEach(() => {
        jest.resetModules();
        process.env = { ...OLD_ENV };
        process.env.IVER_GIT_DIR = 'fixture/.git-test';
    });
    afterAll(() => {
        process.env = OLD_ENV;
    });

    test('Работает получение списка измененных файлов', async() => {
        const vcs = new Git(since, till, []);
        const diff = await vcs.getDiff();
        expect(diff.length).toEqual(5);
    });

    test('Возвращаем корректный since (последнее merge событие)', async() => {
        const vcs = new Git(since, till, []);
        const sinceSha = await vcs.calculateSince();
        expect(await vcs.runGit(`log --format=%B -n 1 ${sinceSha}`)).toContain('Merge pull request #123');
    });

    test('getCommitsFromBranch', async() => {
        const vcs = new Git(
            '65fc98d0d422914d45e06552ba4eb22b6743c473',
            '0b9ca5cba5b0af502e35fded4db8afaf946e450e',
            [],
        );
        const commits = await vcs.getCommitsFromBranch('.');
        expect(commits.length).toEqual(2);
    });

    test('getMergedCommits', async() => {
        const vcs = new Git('6ef9ea9105b13c3a955bd9bcbd9eea803baf5632', '', []);
        const commits = await vcs.getMergedCommits('tasks/daily/');
        expect(commits.length).toEqual(1);
        expect(commits[0].directCommits.length).toEqual(1);
        expect(commits[0].directCommits[0].files.length).toEqual(3);
        expect(commits[0].prTitle).toEqual('ISL-9523: Добавить признак категории для метрики');
    });
});
