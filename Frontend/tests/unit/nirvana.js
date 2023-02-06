'use strict';

const config = require('config');
const proxyquire = require('proxyquire');

const Logger = require('../../src/server/logger');

const Nirvana = proxyquire.load('../../src/server/adapters/nirvana', {
    'request-promise': () => Promise.resolve(),
});

describe('nirvana', () => {
    let nirvana, sandbox;

    beforeEach(() => {
        const token = '**************************************';
        const reqId = '**************************************';

        sandbox = sinon.createSandbox();
        nirvana = new Nirvana(token, config.hosts.nirvana, new Logger(reqId, config.sbsRobotLogin));
    });

    afterEach(() => sandbox.restore());

    describe('stopWorkflowIfRunningOrWaiting:', () => {
        it('не должен пытаться остановить несуществующий граф', () => {
            sandbox.stub(nirvana, 'getExecutionState').callsFake(() => Promise.resolve({}));
            sandbox.stub(nirvana, 'stopWorkflow').callsFake(() => Promise.resolve({}));

            const workflowId = 'wf-100-500';

            return nirvana.stopWorkflowIfRunningOrWaiting(workflowId).then(() =>
                assert.isTrue(nirvana.stopWorkflow.notCalled),
            );
        });

        it('если граф в состоянии running, то должен его остановить', () => {
            sandbox.stub(nirvana, 'getExecutionState').callsFake(() => Promise.resolve({ status: 'running' }));
            sandbox.stub(nirvana, 'stopWorkflow').callsFake(() => Promise.resolve({}));

            const workflowId = 'wf-100-500';

            return nirvana.stopWorkflowIfRunningOrWaiting(workflowId).then(() =>
                assert.calledWith(nirvana.stopWorkflow, workflowId),
            );
        });

        it('если граф в состоянии waiting, то должен его остановить', () => {
            sandbox.stub(nirvana, 'getExecutionState').callsFake(() => Promise.resolve({ status: 'waiting' }));
            sandbox.stub(nirvana, 'stopWorkflow').callsFake(() => Promise.resolve({}));

            const workflowId = 'wf-100-500';

            return nirvana.stopWorkflowIfRunningOrWaiting(workflowId).then(() =>
                assert.calledWith(nirvana.stopWorkflow, workflowId),
            );
        });
    });
});
