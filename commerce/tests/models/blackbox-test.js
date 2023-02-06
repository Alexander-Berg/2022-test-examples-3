const { expect } = require('chai');
const sinon = require('sinon');
const nock = require('nock');
const ip = require('ip');
const config = require('yandex-config');

const Blackbox = require('models/blackbox');
const log = require('logger');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;
const nockTvm = require('tests/helpers/nockTvm');

describe('Blackbox model', () => {
    beforeEach(() => {
        sinon.spy(log, 'warn');
    });

    afterEach(() => {
        log.warn.restore();
        nock.cleanAll();
    });

    describe('`sessionId`', () => {
        it('should request to blackbox', function *() {
            nock('http://blackbox.test.host')
                .get('/blackbox')
                .query({
                    method: 'sessionid',
                    format: 'json',
                    custom: 'some value',
                    attributes: '27,28,1008',
                    emails: 'getdefault'
                })
                .reply(200, { message: 'blackbox sessionId response' });

            const blackbox = new Blackbox({
                connection: { api: 'blackbox.test.host' },
                attributes: '27,28,1008'
            });

            const actual = yield blackbox.sessionId({ custom: 'some value' });

            expect(actual).to.deep.equal({ message: 'blackbox sessionId response' });
            expect(log.warn.called).to.be.false;
        });

        it('should write warn when request is slowly', function *() {
            nock('http://blackbox.test.host')
                .get('/blackbox')
                .query(true)
                .delay(51)
                .reply(200, {});

            const blackbox = new Blackbox({
                connection: { api: 'blackbox.test.host' },
                attributes: '27,28'
            });

            yield blackbox.sessionId();
            expect(log.warn.calledOnce).to.be.true;
        });
    });

    describe('`userInfo`', () => {
        it('should request to blackbox', function *() {
            nock('http://blackbox.test.host')
                .get('/blackbox')
                .query({
                    method: 'userinfo',
                    format: 'json',
                    custom: 'some value',
                    attributes: '27,28'
                })
                .reply(200, { message: 'blackbox userinfo response' });

            const blackbox = new Blackbox({
                connection: { api: 'blackbox.test.host' },
                attributes: '27,28'
            });

            const actual = yield blackbox.userInfo({ custom: 'some value' });

            expect(actual).to.deep.equal({ message: 'blackbox userinfo response' });
            expect(log.warn.called).to.be.false;
        });

        it('should request to blackbox with tvm ticket', function *() {
            nock('http://blackbox.test.host', {
                reqheaders: { 'x-ya-service-ticket': 'someTicket' }
            })
                .get('/blackbox')
                .query({
                    method: 'userinfo',
                    format: 'json',
                    custom: 'some value',
                    attributes: '27,28'
                })
                .reply(200, { message: 'blackbox userinfo response' });

            nockTvm.getTicket({
                blackbox: { ticket: 'someTicket' }
            });

            const blackbox = new Blackbox({
                connection: { api: 'blackbox.test.host' },
                attributes: '27,28',
                tvmName: 'blackbox'
            });

            const actual = yield blackbox.userInfo({ custom: 'some value', forceTvmCheck: 1 });

            expect(actual).to.deep.equal({ message: 'blackbox userinfo response' });
            expect(log.warn.called).to.be.false;
        });

        it('should write warn when request is slowly', function *() {
            nock('http://blackbox.test.host')
                .get('/blackbox')
                .query(true)
                .delay(51)
                .reply(200, {});

            const blackbox = new Blackbox({
                connection: { api: 'blackbox.test.host' },
                attributes: '27,28'
            });

            yield blackbox.userInfo();
            expect(log.warn.calledOnce).to.be.true;
        });
    });

    describe('`getEmails`', () => {
        it('should get emails by uids', function *() {
            const blackboxResponse = {
                users: [
                    {
                        uid: { value: 1234 },
                        'address-list': [
                            { address: 'email1@yandex.ru' }
                        ]
                    },
                    {
                        uid: { value: 5678 },
                        'address-list': [
                            { address: 'email2@yandex.ru' }
                        ]
                    },
                    {
                        uid: { value: 9101 },
                        'address-list': []
                    }
                ]
            };

            nockBlackbox({ uid: '1234,5678,9101', userip: ip.address(), response: blackboxResponse });

            const blackbox = new Blackbox(config.blackbox);
            const actual = yield blackbox.getEmails([1234, 5678, 9101]);

            const expected = {
                1234: 'email1@yandex.ru',
                5678: 'email2@yandex.ru',
                9101: ''
            };

            expect(actual).to.deep.equal(expected);
        });
    });
});
