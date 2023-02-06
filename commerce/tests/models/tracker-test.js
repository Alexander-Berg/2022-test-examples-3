const { expect } = require('chai');
const nock = require('nock');
const mockery = require('mockery');
const sinon = require('sinon');
const { URL } = require('url');

const { tracker } = require('yandex-config');
let log = require('logger');

let TrackerModel = require('models/tracker');

const mockMailer = require('tests/helpers/mailer');

describe('`Tracker model`', () => {
    describe('`createPendingTicket`', () => {
        before(() => {
            mockMailer();
            TrackerModel = require('models/tracker');
            log = require('logger');
        });

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        beforeEach(() => {
            sinon.spy(log, 'error');
        });

        afterEach(() => {
            nock.cleanAll();
            log.error.restore();
        });

        it('should create new ticket', function *() {
            nock(new URL(tracker.options.endpoint).origin)
                .post('/v2/issues')
                .reply(201);

            yield TrackerModel.createPendingTicket(12345);

            expect(log.error.notCalled).to.be.true;
        });

        it('should process error when startrek responded with error', function *() {
            nock(new URL(tracker.options.endpoint).origin)
                .post('/v2/issues')
                .reply(500);

            yield TrackerModel.createPendingTicket(12345);

            expect(log.error.calledOnce).to.be.true;
        });
    });
});
