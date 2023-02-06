const _ = require('lodash');
const message = require('../../../../src/shared/fresheners/messages');
const {
    DEFAULT_VALUES,
    fillWorkflowType,
    fillRunInYang,
    fillUseAutoHoneypots,
    fillNotificationMode,
    fillOwners,
    fillApproveMode,
} = require('../../../../src/shared/fresheners/common-freshners');

const EXP = {
    type: 'poll',
    params: {
        title: 'title',
        poll: [],
        overlap: '100',
        poolId: '2108698',
        sandboxPoolId: '72494',
        workflowType: 'stable',
    },
};

describe('common freshners', () => {
    let expStub;
    beforeEach(() => {
        expStub = _.cloneDeep(EXP);
    });

    describe('fillWorkflowType', () => {
        it('должен установить значение по умолчанию, если оно не заданно', () => {
            const expected = { exp: { workflowType: DEFAULT_VALUES.workflowType } };
            assert.deepEqual(fillWorkflowType({}), expected);
        });
    });

    describe('fillRunInYang', () => {
        const userStub = {
            isAdmin: false,
            login: 'login',
            permissions: {
                general: {
                    'can-use-yang': false,
                },
            },
        };

        it('должен установить значение по умолчанию, если оно не заданно', () => {
            const expected = { runInYang: DEFAULT_VALUES.runInYang };
            assert.deepEqual(fillRunInYang({}, {}).exp, expected);
        });

        it('должен изменить на значение по умолчанию, если у пользователя нет прав', () => {
            const expStub = { runInYang: 'yes' };
            const expected = { runInYang: DEFAULT_VALUES.runInYang };
            assert.deepEqual(fillRunInYang(expStub, userStub).exp, expected);
        });

        it('должен вернуть сообщение если значение измененно', () => {
            const expStub = { runInYang: 'yes' };
            const { messages } = fillRunInYang(expStub, userStub);
            assert.equal(messages[0], message.CAN_NOT_RUN_IN_YANG);
        });
    });

    describe('fillUseAutoHoneypots', () => {
        it('должен установить значение по умолчанию, если оно не заданно', () => {
            const expected = { useAutoHoneypots: DEFAULT_VALUES.useAutoHoneypots };
            const { exp } = fillUseAutoHoneypots({});
            assert.deepEqual(exp, expected);
        });

        it('должен вернуть сообщение если добавлено значение по умолчанию', () => {
            const { messages } = fillUseAutoHoneypots({});
            assert.equal(messages[0], message.USE_AUTO_HONEYPOT);
        });
    });

    describe('fillNotificationMode', () => {
        it('должен установить значение по умолчанию, если оно не задано', () => {
            const expected = { exp: { notificationMode: DEFAULT_VALUES.notificationMode } };
            assert.deepEqual(fillNotificationMode({}), expected);
        });

        it('должен привести значение к новому формату, если поле задано строкой', () => {
            const expected = { exp: { notificationMode: { preset: 'stOnly', workflowNotificationChannels: ['email'] } } };
            assert.deepEqual(fillNotificationMode({ notificationMode: 'stOnly' }), expected);
        });
    });

    describe('fillOwners', () => {
        it('должен установить значение по умолчанию, если оно не заданно', () => {
            const { exp } = fillOwners(expStub.params);
            assert.equal(exp.owners, DEFAULT_VALUES.owners);
        });
    });

    describe('fillApproveMode', () => {
        it('не должен менять значение, если оно задано', () => {
            const expStub = { approveMode: 'manual' };
            const { exp } = fillApproveMode(expStub);
            assert.equal(exp.approveMode, 'manual');
        });

        it('если значение не задано, должен проставить auto', () => {
            const expStub = {};
            const { exp } = fillApproveMode(expStub);
            assert.equal(exp.approveMode, 'auto');
        });
    });
});
