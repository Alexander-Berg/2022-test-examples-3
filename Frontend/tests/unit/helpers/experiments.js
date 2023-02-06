const { isStatusExistInList, checkIfExpDeprecated } = require('../../../src/server/helpers/experiments');

describe('helpers/experiments', () => {
    let sandbox;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
    });

    afterEach(() => sandbox.restore());

    describe('isStatusExistInList:', () => {
        let statusesList, workflowId;
        beforeEach(() => {
            workflowId = 'bccf77dc-3568-11e7-89a6-0025909427cc';
            statusesList = [
                { workflowId, stage: 'pool-start', status: 'in-progress', progress: null },
                { workflowId, stage: 'screen-filter', status: 'in-progress', progress: null },
            ];
        });

        it('должен вернуть false, если статуса с переданными параметрами нет в списке', () => {
            const params = {
                workflowId,
                status: 'in-progress',
                stage: 'pool-ready',
            };
            assert.isFalse(isStatusExistInList(statusesList, params));
        });

        it('должен вернуть true, если статус с переданными параметрами есть в списке', () => {
            const params = {
                workflowId,
                status: 'in-progress',
                stage: 'pool-start',
            };
            assert.isTrue(isStatusExistInList(statusesList, params));
        });
    });

    describe('checkIfExpDeprecated', () => {
        it('должен вернуть true, если эксперимент входит в черный список', () => {
            const blacklist = [13, 654, 645, 643, 677, 3000, 5000];
            const checkedExpId = 13;

            assert.equal(checkIfExpDeprecated(blacklist, checkedExpId), true);
        });

        it('должен вернуть false, если эксперимент не входит в черный список', () => {
            const blacklist = [13, 654, 645, 643, 677, 3000, 5000];
            const checkedExpId = 3213;

            assert.equal(checkIfExpDeprecated(blacklist, checkedExpId), false);
        });
    });
});
