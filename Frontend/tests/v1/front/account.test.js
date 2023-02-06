const assert = require('assert');
const request = require('supertest');
const nock = require('nock');
const uuid = require('uuid/v1');

const { nockSenderSendEmail } = require('tests/mocks');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const app = require('app');
const Sender = require('lib/sender');
const db = require('db');
const { DbType } = require('db/constants');

describe('Account routes', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('GET /front/confirmation/:token', () => {
        it('should confirm account email', async() => {
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            const token = '087755b0-1afc-11e9-914d-338cdb03ced3';

            await factory.registration.create({
                id: 17,
                answers: [
                    {
                        slug: 'email',
                        label: 'Email',
                        value: 'saaaaaaaaasha@yandex-team.ru',
                    },
                    {
                        slug: 'firstName',
                        label: 'Ваша имя',
                        value: 'Alex',
                    },
                ],
                accountId: { id: 10, email: 'saaaaaaaaasha@yandex-team.ru', firstName: 'Solo' },
                confirmationEmailCode: token,
            });

            await request(app.listen())
                .get(`/v1/front/confirmation/${token}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({});

            const account = await db.account.findById(10);
            const registration = await db.registration.findById(17);

            assert.strictEqual(account.firstName, 'Alex'); // Обновилось имя из регистрации
            assert.strictEqual(account.isEmailConfirmed, true); // Email подтвержен
            assert.strictEqual(registration.confirmationEmailCode, null);
        });

        it('should confirm account email with replaced variables from ', async() => {
            nockSenderSendEmail({
                to: 'saaaaaaaaasha@yandex-team.ru',
                expectedVariables: {
                    firstName: 'Alex',
                    eventName: 'Code Fest',
                    waitingPeriod: 'через пару дней',
                },
            });

            const token = '087755b0-1afc-11e9-914d-338cdb03ced3';

            await factory.eventMailTemplatePreset.create({
                id: 10,
                eventId: { id: 6 },
                mailTemplateId: { id: 15, externalSlug: Sender.mailIds[DbType.internal].registrationConfirmed },
                variables: { eventName: 'Code Fest', waitingPeriod: 'через пару дней' },
            });
            await factory.registration.create({
                id: 17,
                answers: [
                    {
                        slug: 'email',
                        label: 'Email',
                        value: 'saaaaaaaaasha@yandex-team.ru',
                    },
                    {
                        slug: 'firstName',
                        label: 'Ваша имя',
                        value: 'Alex',
                    },
                ],
                eventId: 6,
                accountId: { id: 10, email: 'saaaaaaaaasha@yandex-team.ru', firstName: 'Solo' },
                confirmationEmailCode: token,
            });

            await request(app.listen())
                .get(`/v1/front/confirmation/${token}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({});

            const account = await db.account.findById(10);
            const registration = await db.registration.findById(17);

            assert.strictEqual(account.firstName, 'Alex'); // Обновилось имя из регистрации
            assert.strictEqual(account.isEmailConfirmed, true); // Email подтвержен
            assert.strictEqual(registration.confirmationEmailCode, null);
        });

        it('should throw error if token is invalid', async() => {
            const token = 'invalid';

            await request(app.listen())
                .get(`/v1/front/confirmation/${token}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_UIF',
                    message: 'token has invalid uuid format',
                    value: 'invalid',
                });
        });

        it('should throw error if registration is not found by token', async() => {
            const token = '087755b0-1afc-11e9-914d-338cdb03ced3';

            await request(app.listen())
                .get(`/v1/front/confirmation/${token}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Registration not found',
                    scope: 'oneWithEvent',
                    dbType: DbType.internal,
                    where: { confirmationEmailCode: token },
                });
        });
    });

    describe('GET /front/unsubscribe/:token', () => {
        it('should unsubscribe account from news subscription', async() => {
            const unsubscribeCode = uuid();

            await factory.subscription.create({ id: 7, unsubscribeCode });

            await request(app.listen())
                .get(`/v1/front/unsubscribe/${unsubscribeCode}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(200);

            const subscriptions = await db.subscription.findAll();

            assert.strictEqual(subscriptions.length, 1);
            assert.strictEqual(subscriptions[0].isActive, false);
        });

        it('should throw error if uuid is not valid', async() => {
            const unsubscribeCode = 'invalid';

            await request(app.listen())
                .get(`/v1/front/unsubscribe/${unsubscribeCode}`)
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_UIF',
                    message: 'token has invalid uuid format',
                    value: 'invalid',
                });
        });
    });
});
