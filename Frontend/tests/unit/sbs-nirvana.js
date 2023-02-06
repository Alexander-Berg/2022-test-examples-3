'use strict';

const config = require('config');
const proxyquire = require('proxyquire');

const Nirvana = require('./fixtures/nirvana');
const Logger = require('../../src/server/logger');

const SbsNirvana = proxyquire.load('../../src/server/sbs-nirvana', {
    './adapters/nirvana': Nirvana,
});

describe('sbs-nirvana', () => {
    let sbsNirvana, sandbox;

    beforeEach(() => {
        const token = '**************************************';
        const reqId = '**************************************';

        sandbox = sinon.createSandbox();
        sbsNirvana = new SbsNirvana(token, config.hosts.nirvana, new Logger(reqId, config.sbsRobotLogin));
    });

    afterEach(() => sandbox.restore());

    describe('prepareWorkflow:', () => {
        it('должен корректно задавать глобальные параметры графа', () => {
            const params = {
                ticketId: 'SIDEBYSIDE-1234',
                ytWorkspace: 'dev',
                workflowName: 'workflow title',
                templateWorkflowId: 'e13c26b8-5697-4158-b955-a64f7f073f4a',
                sbsName: 'experiment title',
                expUrl: '/experiment/12345',
                samadhiHost: 'test.sbs.yandex-team.ru',
            };
            const newWorkflowId = '07151bc9-191a-4a82-8eb9-0b209e2a7694';
            const uniqueNumber = 97324692836412;

            sandbox.stub(sbsNirvana, 'getUniqueNumber').callsFake(() => uniqueNumber);
            sandbox.stub(sbsNirvana, 'cloneTemplateWorkflow').callsFake(() => Promise.resolve(newWorkflowId));
            sandbox.spy(sbsNirvana, 'setGlobalParameters');

            return sbsNirvana.prepareWorkflow(params).then(() => {
                assert.calledWith(sbsNirvana.setGlobalParameters, newWorkflowId, [
                    { parameter: 'ticket', value: params.ticketId },
                    { parameter: 'yt-workspace', value: params.ytWorkspace },
                    { parameter: 'timestamp-cache-disabler', value: uniqueNumber },
                    { parameter: 'samadhi-host-override', value: params.samadhiHost },
                ]);
            });
        });
    });
});
