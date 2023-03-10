'use strict';

const _ = require('lodash');
const QEmitter = require('qemitter');

const lib = require('../lib/index');
const Stat = require('../lib/stat');
const hermionePlugin = require('../hermione');

function mkHermione_() {
    return _.extend(new QEmitter(), {
        events: {
            SESSION_START: 'startSession',
            SESSION_END: 'endSession',
            TEST_PASS: 'passTest',
            TEST_FAIL: 'failTest',
            RETRY: 'retry',
            TEST_PENDING: 'pendingTest',
            RUNNER_END: 'endRunner'
        }
    });
}

describe('hermione', () => {
    const sandbox = sinon.sandbox.create();

    let hermione;

    beforeEach(() => {
        hermione = mkHermione_();
        hermionePlugin(hermione, {enabled: true});
    });

    afterEach(() => sandbox.restore());

    it('should do nothing for disabled plugin', () => {
        hermione = mkHermione_();
        const onSpy = sinon.spy(hermione.on);

        hermionePlugin(hermione, {enabled: false});

        assert.notCalled(onSpy);
    });

    it('should start browser time on "SESSION_START" event', () => {
        sandbox.stub(Stat.prototype, 'markStartBrowserTime');
        hermione.emit(hermione.events.SESSION_START, sinon.stub(), {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.markStartBrowserTime, 'some-browser');
    });

    it('should mark browser time on "SESSION_END" event', () => {
        sandbox.stub(Stat.prototype, 'markEndBrowserTime');
        hermione.emit(hermione.events.SESSION_END, sinon.stub(), {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.markEndBrowserTime, 'some-browser');
    });

    it('should add passed test on "TEST_PASS" event', () => {
        sandbox.stub(Stat.prototype, 'addPassed');
        hermione.emit(hermione.events.TEST_PASS, {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.addPassed, 'some-browser');
    });

    it('should add failed test on "TEST_FAIL" event', () => {
        sandbox.stub(Stat.prototype, 'addFailed');
        hermione.emit(hermione.events.TEST_FAIL, {browserId: 'some-browser', err: {name: 'AssertionError'}});

        assert.calledWith(Stat.prototype.addFailed, 'some-browser');
    });

    it('should add broken test on "TEST_FAILED" event', () => {
        sandbox.stub(Stat.prototype, 'addBroken');
        hermione.emit(hermione.events.TEST_FAIL, {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.addBroken, 'some-browser');
    });

    it('should add retried test on "RETRY" event', () => {
        sandbox.stub(Stat.prototype, 'addRetry');
        hermione.emit(hermione.events.RETRY, {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.addRetry, 'some-browser');
    });

    it('should add skipped test on "TEST_PENDING" event', () => {
        sandbox.stub(Stat.prototype, 'addSkipped');
        hermione.emit(hermione.events.TEST_PENDING, {browserId: 'some-browser'});

        assert.calledWith(Stat.prototype.addSkipped, 'some-browser');
    });

    it('should show collected statistic data on "RUNNER_END" event', () => {
        sandbox.stub(lib, 'showReport');
        sandbox.stub(Stat.prototype, 'getStatistic').returns('some-statistic');

        hermione.emit(hermione.events.RUNNER_END);

        assert.calledOnce(lib.showReport);
        assert.calledWith(lib.showReport, 'some-statistic', sinon.match.object);
    });
});
