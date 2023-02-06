/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as proxyquire from 'proxyquire';
import { fake } from 'sinon';
import * as moderatorResponseProcessor from '../../../services/moderation/moderatorResponseProcessor';

const test = anyTest as TestInterface<{
    logError: sinon.SinonSpy;
    parseModeratorResponses: typeof moderatorResponseProcessor.parseModeratorResponses;
}>;

test.beforeEach('parseModeratorResponses: returns empty array on error', t => {
    const logError = fake();
    const { parseModeratorResponses } = proxyquire('../../../services/moderation/moderatorResponseProcessor', {
        '../log': {
            default: { error: logError },
        },
    }) as typeof moderatorResponseProcessor;

    t.context = {
        logError,
        parseModeratorResponses,
    };
});

test('parseModeratorResponses: returns empty array on error', t => {
    const { parseModeratorResponses } = t.context;

    t.deepEqual(parseModeratorResponses(null, 'myTable'), []);
});

test('parseModeratorResponses: logs table name', t => {
    const { logError, parseModeratorResponses } = t.context;

    parseModeratorResponses(null, 'myTable');
    t.is(logError.firstCall.lastArg.tableName, 'myTable');
});

test('parseModeratorResponses: parses empty table', t => {
    const { logError, parseModeratorResponses } = t.context;

    t.deepEqual(parseModeratorResponses('', 'myTable'), []);
    t.true(logError.notCalled);
});
