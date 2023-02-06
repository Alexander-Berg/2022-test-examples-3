const proxyquire = require('proxyquire');

const IssueStub = {};
const CommentStub = {};

const Logger = require('../fixtures/logger');
const logger = new Logger;

const configStub = require('config');
configStub.startrek.sleepAfterTicketCreationBeforeNextUpdate = 1;
configStub['@global'] = true;

const Argentum = require('../fixtures/argentum');
Argentum['@global'] = true;

/**
 * @param {String} type – serp|layout
 */
function getTicketClass(type) {
    return proxyquire(`../../../src/server/models/ticket/ticket_${type}`, {
        /* eslint-disable */
        'stapi/models/issue': function() {
            return IssueStub;
        },
        'stapi/models/comment': function() {
            return CommentStub;
        },
        'config': configStub,
        '../../adapters/argentum/tracker': Argentum,
        /* eslint-enable */
    });
}

/**
 * @param {String} type – serp|layout
 */
function getFixtures(type) {
    return require(`../fixtures/ticket/ticket_${type}`);
}

/**
 * @param {String} type – serp|layout
 */
function testTicket(type) {
    describe(`Ticket mod ${type}`, () => {
        let ticket;
        let sandbox;
        let fixtures;

        beforeEach(() => {
            const Ticket = getTicketClass(type);
            fixtures = getFixtures(type);

            const ISSUE_NUMBER = 123;

            sandbox = sinon.createSandbox();

            ticket = new Ticket(logger);
            ticket._save = sandbox.stub().returns(new Promise((r) => r()));
            ticket._getIssueNumber = sandbox.stub().returns(ISSUE_NUMBER);
            ticket._publish = sandbox.stub().returns(Promise.resolve(IssueStub));
            ticket.getIssue = sandbox.stub().returns(Promise.resolve(IssueStub));
            ticket._linkIssues = sandbox.stub().returns(Promise.resolve(IssueStub));
            ticket.postComment = sandbox.stub();
            ticket._executeIssueTransition = sandbox.stub().returns(new Promise((r) => r()));
            ticket._getMDTemplate = sandbox.stub().returns(() => 'test');

            IssueStub.setQueue = sandbox.stub().returns(IssueStub);
            IssueStub.setSummary = sandbox.stub().returns(IssueStub);
            IssueStub.setField = sandbox.stub().returns(IssueStub);
            IssueStub.setDescription = sandbox.stub().returns(IssueStub);
            IssueStub.getStatus = sandbox.stub().returns({ getKey: () => 'test' });

            CommentStub.setText = sandbox.stub().returns(CommentStub);
            CommentStub.publishTo = sandbox.stub().returns(CommentStub);
        });

        afterEach(() => sandbox.restore());

        describe('create', () => {
            it('создает и сохраняет новый issue', () => {
                sandbox.spy(Argentum.prototype, 'createTicket');

                return ticket.create(fixtures.create.title, 38575, 'gwer')
                    .then(() => {
                        assert.isTrue(Argentum.prototype.createTicket.calledOnce);
                    });
            });

            it('должен обрабатывать ошибки трекера', () => {
                const ERR_MESSAGE = 'ошибка трекера';

                sandbox.stub(Argentum.prototype, 'createTicket').returns(Promise.reject(ERR_MESSAGE));
                sandbox.spy(ticket, '_handleError');

                return ticket.create(fixtures.create.title, 38575, 'gwer')
                    .catch((err) => assert.strictEqual(err, ERR_MESSAGE))
                    .then(() => assert.isTrue(ticket._handleError.calledOnce));
            });
        });

        describe('_getIssueNumber()', () => {
            it('возвращает правильное число', function() {
                const ISSUE_NUMBER = 123;

                assert.equal(ticket._getIssueNumber({
                    getKey: () => 'SBS-123',
                }), ISSUE_NUMBER);
            });
        });

        describe('_getIssueId()', () => {
            it('возвращает правильный ID тикета', function() {
                const ISSUE_NUMBER = 123;
                const ISSUE_ID = 'SIDEBYSIDE-123';

                assert.equal(ticket._getIssueId(ISSUE_NUMBER), ISSUE_ID);
            });
        });

        describe('_generateComment()', () => {
            it('функция-шаблон комментария будет вызвана с правильными параметрами', () => {
                const template = sandbox.stub().returns('text');
                const data = {
                    id: 42,
                    workflowId: 'bccf77dc-3568-11e7-89a6-0025909427cc',
                    pools: {
                        production: {
                            'pool-id': 456,
                        },
                        sandbox: {
                            'pool-id': 89,
                        },
                    },
                    planReportUrl: 'https://sbs.s3.yandex.net/0681d29e3610579d7e3b823f198a4a6e6421cc58eda45500d63c3310aeae4ab3/sbs-101174-plan-report.html',

                };
                const expectedArg = {
                    ...data,
                    login: undefined,
                    prodPoolUrl: 'https://toloka.yandex.ru/requester/pool/456',
                    sandboxPoolUrl: 'https://sandbox.toloka.yandex.com/requester/assignment-preview/pool/89',
                    workflowUrl: 'https://nirvana.yandex-team.ru/flow/bccf77dc-3568-11e7-89a6-0025909427cc/graph',
                    resultsUrl: 'https://localhost/experiment/42/results',
                    hasProdPoolId: true,
                    tolokaSandbox: {
                        login: 'sbs.sandbox',
                        pass: 'LEFT-is-BEttER',
                    },
                    planReportUrl: 'https://sbs.s3.yandex.net/0681d29e3610579d7e3b823f198a4a6e6421cc58eda45500d63c3310aeae4ab3/sbs-101174-plan-report.html',
                };

                ticket._generateComment(template, data);
                assert.calledWith(template, expectedArg);
            });
        });

        describe('Комментарии к этапа/статусам графа', () => {
            it('если для этапа задан шаблон сообщения, то комментарий будет добавлен', () => {
                const ID = 42;

                ticket._generateComment = sandbox.stub().returns('text');
                ticket.postStatusComment(ID, 'results', type, {}, 'all');

                assert.isTrue(ticket.postComment.calledOnce);
            });

            it('если для этапа не задан шаблон сообщения, то комментарий не будет добавлен', () => {
                const ID = 42;

                ticket.postStatusComment(ID, 'unknown_stage', type, {}, 'all');

                assert.isTrue(ticket.postComment.notCalled);
            });

            it('работает уведомление для шаблонного комментария для которого должно в mailNotifyPreset: all', () => {
                ticket.postCommentByTemplate(1, 'workflow-start-error', {}, 'all');

                assert.calledWith(ticket.postComment, 1, 'test', false, false, true);
            });

            it('работает уведомление для шаблонного комментария для которого должно в mailNotifyPreset: silent', () => {
                ticket.postCommentByTemplate(1, 'workflow-start-error', {}, 'silent');

                assert.calledWith(ticket.postComment, 1, 'test', false, false, true);
            });

            it('не работает уведомление для комментария смены статуса для которого не должно в mailNotifyPreset: all', () => {
                ticket._generateComment = sandbox.stub().returns('text');

                ticket.postStatusComment(1, 'owner-approve', type, {}, 'all');

                assert.calledWith(ticket.postComment, 1, 'text', true, false, false);
            });

            it('работает уведомление для комментария смены статуса для которого должно в mailNotifyPreset: all', () => {
                ticket._generateComment = sandbox.stub().returns('text');

                ticket.postStatusComment(1, 'new-graph-launch', type, {}, 'all');

                assert.calledWith(ticket.postComment, 1, 'text', undefined, undefined, true);
            });

            it('не работает уведомление для комментария смены статуса для которого не должно в mailNotifyPreset: silent', () => {
                ticket._generateComment = sandbox.stub().returns('text');

                ticket.postStatusComment(1, 'new-graph-launch', type, {}, 'silent');

                assert.calledWith(ticket.postComment, 1, 'text', undefined, undefined, false);
            });

            it('не работает уведомление при смене статуса в трекере для которого не должно в mailNotifyPreset: all', () => {
                return ticket.transitionTicket(1, 'configure', 'all')
                    .then(() => {
                        assert.calledWith(ticket._executeIssueTransition, IssueStub, 'preparation', null, false);
                    });
            });

            it('работает уведомление при смене статуса в трекере для которого должно', () => {
                return ticket.transitionTicket(1, 'pool-start', 'all')
                    .then(() => {
                        assert.calledWith(ticket._executeIssueTransition, IssueStub, 'experimentOn', null, true);
                    });
            });

            it('не работает уведомление при смене статуса в трекере для которого не должно в mailNotifyPreset: silent', () => {
                return ticket.transitionTicket(1, 'pool-start', 'silent')
                    .then(() => {
                        assert.calledWith(ticket._executeIssueTransition, IssueStub, 'experimentOn', null, false);
                    });
            });
        });
    });
}

testTicket('serp');
testTicket('layout');
