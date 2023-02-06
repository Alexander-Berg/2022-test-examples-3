const assert = require('assert');
// Doesn't work as expected with jest
// See https://stackoverflow.com/questions/51218760/how-do-i-stub-a-module-with-jest-and-proxyquire
const proxyquire = require('proxyquire');

const tryRequireModule = (env = {}, { execSync } = {}) => {
    Object.assign(process.env, env);
    execSync || (execSync = () => Buffer.from('trunk'));
    // extractIssueKeys || (extractIssueKeys = v => [`extracted:${v}`]);

    return proxyquire('./stand-name', {
        child_process: {
            execSync,
            '@global': true,
        },
        // '@yandex-int/si.ci.tracker-utils': { extractIssueKeys },
    });
};

describe('stand-name', () => {
    const originalEnv = process.env;

    beforeEach(() => {
        process.env = {};
    });

    afterEach(() => {
        process.env = originalEnv;
    });

    describe('on dev machine', () => {
        it('should return extracted ticket from branch with ticket', () => {
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.description.QUEUE-12\n' }), 'queue-12');
        });

        it('should return stand name for branch with two tickets', () => {
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.QUEUE-123.QUEUE-567\n' }), 'queue-123');
        });

        it('should not return stand name for branch with ticket', () => {
            assert.strictEqual(tryRequireModule({}, { execSync: () => '-user...TRIVIAL-\n' }), 'user-trivial');
        });

        it('should call arc if git failed to get stand name', () => {
            const calledWith = []; // TODO: use sinon.stub()
            const execSync = (...v) => {
                calledWith.push(v);
                if (calledWith.length === 1) { // Broke the first call (a git call) to force calling arc
                    return '\n';
                }

                return 'user.description.QUEUE-12\n';
            };

            assert.strictEqual(tryRequireModule({}, { execSync }), 'queue-12', 'branch name does not equal');
            assert.ok(/git rev-parse.*--abbrev-ref/.test(calledWith[0]), 'git command does not match');
            assert.ok(/arc branch.*grep "\^\*/.test(calledWith[1]), 'arc command does not match');
        });

        it('should return stand name for branch with ticket', () => {
            process.env.TRENDBOX_BRANCH = 'PASSED_IN_ENV';

            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.description.QUEUE-12\n' }), 'queue-12');
        });

        it('should return stand name from env STAND if passed explicitly', () => {
            process.env.STAND = 'STAND_ENV';
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.TRIVIAL\n' }), 'STAND_ENV');
        });
    });

    describe('on ci machine', () => {
        beforeEach(() => {
            process.env.CI = true;
            process.env.TRENDBOX_BRANCH = 'PASSED_IN_ENV';
        });

        it('should return stand name for branch if env is empty', () => {
            process.env.TRENDBOX_BRANCH = '';
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.QUEUE-123.QUEUE-567\n' }), 'queue-123');
        });

        it('should return stand name for branch passed in env', () => {
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.TRIVIAL\n' }), 'passed-in-env');
        });

        it('should return stand name from env STAND if passed explicitly', () => {
            process.env.STAND = 'STAND_ENV';
            assert.strictEqual(tryRequireModule({}, { execSync: () => 'user.TRIVIAL\n' }), 'STAND_ENV');
        });
    });
});
