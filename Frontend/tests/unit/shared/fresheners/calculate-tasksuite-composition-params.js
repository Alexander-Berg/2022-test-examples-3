const calculateTasksuiteCompositionParams = require('../../../../src/shared/utils/calculate-tasksuite-composition-params');

describe('/shared/utils/calculate-tasksuite-composition-params.js', () => {
    it('goodTasks=undefined, badTasks=undefined', () => {
        const { mode, goodTasksCount, badTasksCount, assignmentsAcceptedCount } = calculateTasksuiteCompositionParams(undefined, undefined);
        assert.strictEqual(mode, 'default');
        assert.isNull(goodTasksCount);
        assert.isNull(badTasksCount);
        assert.isNull(assignmentsAcceptedCount);
    });

    it('goodTasks=4, badTasks=3', () => {
        const { mode, goodTasksCount, badTasksCount, assignmentsAcceptedCount } = calculateTasksuiteCompositionParams('4', '3');
        assert.strictEqual(mode, 'default');
        assert.isNull(goodTasksCount);
        assert.isNull(badTasksCount);
        assert.isNull(assignmentsAcceptedCount);
    });

    it('goodTasks=1, badTasks=1', () => {
        const { mode, goodTasksCount, badTasksCount, assignmentsAcceptedCount } = calculateTasksuiteCompositionParams('1', '1');
        assert.strictEqual(mode, 'bright');
        assert.isNull(goodTasksCount);
        assert.isNull(badTasksCount);
        assert.isNull(assignmentsAcceptedCount);
    });

    it('goodTasks=2, badTasks=1', () => {
        const { mode, goodTasksCount, badTasksCount, assignmentsAcceptedCount } = calculateTasksuiteCompositionParams('2', '1');
        assert.strictEqual(mode, 'custom');
        assert.strictEqual(goodTasksCount, 2);
        assert.strictEqual(badTasksCount, 1);
        assert.strictEqual(assignmentsAcceptedCount, 3);
    });
});
