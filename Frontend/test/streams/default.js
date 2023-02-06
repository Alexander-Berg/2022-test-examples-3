/* eslint-env mocha */
'use strict';

const assert = require('assert');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('YandexLogger. Streams. Default', () => {
    let clock;
    let lineStream;
    let qloudStream;
    let defaultStream;

    beforeEach(() => {
        clock = sinon.useFakeTimers(1000);

        lineStream = sinon.stub().returns('line');
        qloudStream = sinon.stub().returns('qloud');

        defaultStream = proxyquire('../../streams/default', {
            './line': lineStream,
            './qloud': qloudStream,
        });
    });

    afterEach(() => {
        clock.restore();

        delete process.env.QLOUD_LOGGER_STDERR_PARSER;
    });

    it('должен создать line stream', () => {
        process.env.QLOUD_LOGGER_STDERR_PARSER = 'line';

        let stream = defaultStream({ line: 'line-config', qloud: 'qloud-config' });

        assert.strictEqual(stream, 'line');

        sinon.assert.calledWithExactly(lineStream, 'line-config');
        sinon.assert.calledOnce(lineStream);

        sinon.assert.notCalled(qloudStream);
    });

    it('должен создать qloud stream', () => {
        process.env.QLOUD_LOGGER_STDERR_PARSER = 'json';

        let stream = defaultStream({ line: 'line-config', qloud: 'qloud-config' });

        assert.strictEqual(stream, 'qloud');

        sinon.assert.calledWithExactly(qloudStream, 'qloud-config');
        sinon.assert.calledOnce(qloudStream);

        sinon.assert.notCalled(lineStream);
    });
});
