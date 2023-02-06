const _ = require('lodash');
const { getLastStatus } = require('../../../src/server/utils/statuses');

describe('utils/statuses', () => {
    describe('getLastStatus', () => {
        let expStub, statusesStub;

        beforeEach(() => {
            expStub = {
                'workflows': [
                    {
                        'id': '3b73a979-0d34-47df-813c-493b211ae35d',
                        'type': 'main',
                        'cdate': '2019-08-30T14:56:41.669Z',
                    },
                ],
            };
            statusesStub = [
                {
                    'stage': 'serp-fetch',
                    'status': 'in-progress',
                    'workflowId': '3b73a979-0d34-47df-813c-493b211ae35d',
                    'workflowType': 'main',
                    'isHidden': false,
                },
                {
                    'stage': 'serp-fetch',
                    'status': 'succeeded',
                    'workflowId': '3b73a979-0d34-47df-813c-493b211ae35d',
                    'workflowType': 'main',
                    'isHidden': false,
                },
                {
                    'stage': 'merge-ext-systems',
                    'status': 'in-progress',
                    'workflowId': '3b73a979-0d34-47df-813c-493b211ae35d',
                    'workflowType': 'main',
                    'isHidden': true,
                },
                {
                    'stage': 'merge-ext-systems',
                    // 'status': 'failed',
                    'status': 'succeeded',
                    'workflowId': '3b73a979-0d34-47df-813c-493b211ae35d',
                    'workflowType': 'main',
                    'isHidden': true,
                },
            ];
        });

        it('должен вернуть последний статус со значением isHidden: false', () => {
            expStub.statuses = statusesStub;
            const lastNotHiddenStatus = statusesStub[1];
            const result = getLastStatus(expStub.statuses, expStub.workflows);

            assert.deepEqual(lastNotHiddenStatus, result);
        });

        it('должен вернуть последний статус, если это failed, не зависимо от значения isHidden', () => {
            const lastStatus = _.last(statusesStub);
            lastStatus.status = 'failed';
            expStub.statuses = statusesStub;
            const result = getLastStatus(expStub.statuses, expStub.workflows);

            assert.deepEqual(lastStatus, result);
        });
    });
});

