import {execSync} from 'child_process';

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
function isValidCommitMsg(msg) {
    const isMasterMerge = msg.startsWith("Merge branch 'master' into");
    const isBranchMerge = msg.startsWith('Merge remote-tracking branch');
    const hasTicket = /[A-Z]+-\d+/.test(msg);

    return hasTicket || isMasterMerge || isBranchMerge;
}

describe('Validate commit messages', () => {
    it('has no invalid commit messages', () => {
        // Если бы был git v2.22+ то могли бы сделать
        // `git branch --show-current`
        const localBranch = execSync('git rev-parse --abbrev-ref HEAD').toString().replace(/\n$/, '');
        const branch = process.env.CURRENT_GIT_BRANCH || localBranch;

        // avoid checks for release branches
        if (branch.startsWith('release/')) return;

        // нужна история
        execSync('git fetch --all');

        const stdout = execSync('git ls-remote origin master');
        const [remoteCommit, fullRef] = stdout.toString().split(/\t\n|\t|\n/);

        execSync(`git update-ref ${fullRef} ${remoteCommit}`);

        const messages = execSync(`git cherry -v master ${branch}`)
            .toString()
            .split('\n')
            .map(msg => msg.replace(/^.\s[0-9a-zA-Z]+\s/, ''));

        messages.pop(); // последний всегда ''

        expect(() => {
            const invalidMsgs = messages.filter(m => !isValidCommitMsg(m));

            const l = invalidMsgs.length;

            if (l === 0) return;

            const line1 = `You have invalid commit message${l === 1 ? '' : 's'}:\n`;
            const rest = invalidMsgs.map(m => `- "${m}"`);
            throw new Error([line1, ...rest].join('\n'));
        }).not.toThrow();
    });
});
