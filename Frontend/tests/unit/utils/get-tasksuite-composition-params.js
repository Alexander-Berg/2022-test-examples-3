const { getTasksuiteCompositionParams } = require('../../../src/shared/utils/get-tasksuite-composition-params');

describe('utils/getTasksuiteCompositionParams', () => {
    it('mode = default', () => {
        const jsdoc = {
            mode: 'default',
            val: {
                goodTasksCount: 4,
                badTasksCount: 4,
                assignmentsAcceptedCount: 4,
            },
        };
        const { goodTasksCount, badTasksCount, assignmentsAcceptedCount } = getTasksuiteCompositionParams(jsdoc);

        assert.strictEqual(goodTasksCount, 8);
        assert.strictEqual(badTasksCount, 1);
        assert.strictEqual(assignmentsAcceptedCount, 3);
    });

    it('mode = bright', () => {
        const jsdoc = {
            mode: 'bright',
            val: {
                goodTasksCount: 4,
                badTasksCount: 4,
                assignmentsAcceptedCount: 4,
            },
        };
        const { goodTasksCount, badTasksCount, assignmentsAcceptedCount } = getTasksuiteCompositionParams(jsdoc);

        assert.strictEqual(goodTasksCount, 3);
        assert.strictEqual(badTasksCount, 2);
        assert.strictEqual(assignmentsAcceptedCount, 1);
    });

    it('mode = custom', () => {
        const jsdoc = {
            mode: 'custom',
            val: {
                goodTasksCount: 4,
                badTasksCount: 4,
                assignmentsAcceptedCount: 4,
            },
        };
        const { goodTasksCount, badTasksCount, assignmentsAcceptedCount } = getTasksuiteCompositionParams(jsdoc);

        assert.strictEqual(goodTasksCount, 4);
        assert.strictEqual(badTasksCount, 4);
        assert.strictEqual(assignmentsAcceptedCount, 4);
    });
});
