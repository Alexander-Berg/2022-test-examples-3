const assert = require('assert');
const request = require('supertest');
const nock = require('nock');
const config = require('yandex-cfg');

const { nockTvmCheckTicket } = require('tests/mocks');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const app = require('app');

describe('Takeout routes', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('POST /takeout', () => {
        it('should respond user data by uid', async() => {
            nockTvmCheckTicket({ dst: config.tvmtool.clientId, src: config.takeout.tvmId });

            const uid = 26304219;
            const unixtime = 123456789;

            await factory.registration.create({
                id: 17,
                answers: [
                    { slug: 'email', label: 'Email', value: 'saaaaaaaaasha@yandex-team.ru' },
                    { slug: 'firstName', label: 'Ваша имя', value: 'Alex' },
                    { slug: 'answer_choices_124928', label: 'JS знаете?', value: 'Естественно' },
                ],
                accountId: {
                    id: 10,
                    email: 'saaaaaaaaasha@yandex-team.ru',
                    firstName: 'Solo',
                    yandexuid: 26304219,
                },
            });

            await request(app.listen())
                .post('/v1/takeout')
                .set('x-ya-service-ticket', '123')
                .send({ uid, unixtime })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const { data, status } = body;

                    const fileName = `${uid}-${unixtime}.json`;
                    const userData = JSON.parse(data[fileName]);

                    assert.strictEqual(status, 'ok');
                    assert.strictEqual(userData.email, 'saaaaaaaaasha@yandex-team.ru');
                    assert.strictEqual(userData.registrations.length, 1);
                });
        });

        it('should respond user data by uid without registrations', async() => {
            nockTvmCheckTicket({ dst: config.tvmtool.clientId, src: config.takeout.tvmId });

            const uid = 26304220;
            const unixtime = 123456789;

            await factory.account.create({
                id: 11,
                email: 'solo@yandex-team.ru',
                firstName: 'Solo',
                yandexuid: 26304220,
            });

            await request(app.listen())
                .post('/v1/takeout')
                .set('x-ya-service-ticket', '123')
                .send({ uid, unixtime })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const { data, status } = body;

                    const fileName = `${uid}-${unixtime}.json`;
                    const userData = JSON.parse(data[fileName]);

                    assert.strictEqual(status, 'ok');
                    assert.strictEqual(userData.email, 'solo@yandex-team.ru');
                    assert.strictEqual(userData.registrations.length, 0);
                });
        });

        it('should respond default response if user isn\'t found', async() => {
            nockTvmCheckTicket({ dst: config.tvmtool.clientId, src: config.takeout.tvmId });

            await request(app.listen())
                .post('/v1/takeout')
                .set('x-ya-service-ticket', '123')
                .send({ uid: 26304219 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({ status: 'no_data' });
        });

        it('should throw error if uid is not passed', async() => {
            nockTvmCheckTicket({ dst: config.tvmtool.clientId, src: config.takeout.tvmId });

            await request(app.listen())
                .post('/v1/takeout')
                .set('x-ya-service-ticket', '123')
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'User uid is invalid',
                });
        });
    });
});
