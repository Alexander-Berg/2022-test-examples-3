const assert = require('assert');
const nock = require('nock');
const config = require('yandex-cfg');
const catchError = require('catch-error-async');

const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Sender = require('lib/sender');
const { nockSenderSendEmail, nockSenderRenderPreview } = require('tests/mocks');

const testDbType = DbType.internal;

describe('Sender service', () => {
    beforeEach(cleanDb);
    afterEach(() => nock.cleanAll());

    describe('_getUrl', () => {
        it('should replace variables in pathname', () => {
            const url = Sender._getUrl(Sender.endpoints.SEND_EMAIL, { campaignSlug: '123' });
            const { host, accountSlug } = config.sender;
            const pathname = Sender.endpoints.SEND_EMAIL
                .replace('{accountSlug}', accountSlug)
                .replace('{campaignSlug}', 123);

            assert.strictEqual(url, `${host}${pathname}`);
        });
    });

    describe('sendEmail', () => {
        it('should send email', async() => {
            const nockInstance = nockSenderSendEmail({ to: 'luck@starwars.com' });

            await Sender.sendEmail({
                campaignSlug: Sender.mailIds[testDbType].registrationConfirmed,
                to: 'luck@starwars.com',
                dbType: testDbType,
            });

            assert.ok(nockInstance.isDone());
        });

        it('should send email from mail template', async() => {
            const nockInstance = nockSenderSendEmail({ to: 'luck@starwars.com' });

            await factory.mailTemplate.create({ id: 1, externalSlug: 'WA8CHB23-SFU1' });

            await Sender.sendEmail({
                campaignSlug: 'WA8CHB23-SFU1',
                to: 'luck@starwars.com',
                dbType: testDbType,
            });

            assert.ok(nockInstance.isDone());
        });

        it('should not throw error if param silent is true', async() => {
            const params = {
                campaignSlug: Sender.mailIds[testDbType].registrationConfirmed,
                to: 'not-valid-email',
                silent: true,
                dbType: testDbType,
            };

            await Sender.sendEmail(params);
        });

        it('should throw error if email is not valid', async() => {
            const params = {
                campaignSlug: Sender.mailIds[testDbType].registrationConfirmed,
                to: 'not-valid-email',
                silent: false,
                dbType: testDbType,
            };
            const error = await catchError(Sender.sendEmail.bind(Sender), params);

            assert.strictEqual(error.message, 'Email is invalid');
            assert.strictEqual(error.status, 400);
            assert.deepStrictEqual(error.options, {
                internalCode: '400_EII',
                value: params.to,
            });
        });

        it('should throw error if mail id is not valid', async() => {
            const params = {
                campaignSlug: 'is-not-valid-mail',
                to: 'luck@starwars.com',
                silent: false,
                dbType: testDbType,
            };
            const error = await catchError(Sender.sendEmail.bind(Sender), params);

            assert.strictEqual(error.message, 'Value is not allowed');
            assert.strictEqual(error.status, 400);
            assert.deepStrictEqual(error.options, {
                internalCode: '400_VNA',
                value: params.campaignSlug,
                expected: [
                    config.sender.mailIds.registrationCreateError,
                    config.sender.mailIds.error,
                    ...Object.values(config.sender.mailIds[testDbType])],
            });
        });
    });

    describe('renderEmailPreview', () => {
        it('should render preview email', async() => {
            const nockInstance = nockSenderRenderPreview({
                title: 'Вы приглашены на <<eventName>>',
                text: '<html><body><<firstName>>! Добро пожаловать на мероприятие</body></html>',
            });

            const actual = await Sender.renderEmailPreview({
                campaignId: 11596,
                letterId: 13285,
            });

            const expected = {
                title: 'Вы приглашены на {{eventName}}',
                text: '<html><body>{{firstName}}! Добро пожаловать на мероприятие</body></html>',
            };

            assert.ok(nockInstance.isDone());
            assert.deepStrictEqual(actual, expected);
        });

        it('should render preview email with decode html entities', async() => {
            const nockInstance = nockSenderRenderPreview({
                title: 'Вы приглашены на <<eventName>>',
                text: '<html><body>Привет, <a href="/preview?target=%3C%3CfeedbackUrl%3E%3E"></a></body></html>',
            });

            const actual = await Sender.renderEmailPreview({
                campaignId: 11596,
                letterId: 13285,
            });

            const expected = {
                title: 'Вы приглашены на {{eventName}}',
                text: '<html><body>Привет, <a href="/preview?target={{feedbackUrl}}"></a></body></html>',
            };

            assert.ok(nockInstance.isDone());
            assert.deepStrictEqual(actual, expected);
        });

        it('should render preview email with variables', async() => {
            const nockInstance = nockSenderRenderPreview({
                text: '{{firstName}}! Добро пожаловать на мероприятие {{eventName}}',
            });

            const actual = await Sender.renderEmailPreview({
                campaignId: 11596,
                letterId: 13285,
                params: { firstName: 'Solo' },
            });

            const expected = {
                text: 'Solo! Добро пожаловать на мероприятие {{eventName}}',
                title: '',
            };

            assert.ok(nockInstance.isDone());
            assert.deepStrictEqual(actual, expected);
        });
    });
});
