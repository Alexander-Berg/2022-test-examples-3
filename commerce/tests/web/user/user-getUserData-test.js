const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const dbHelper = require('tests/helpers/clear');

const trialsFactory = require('tests/factory/trialsFactory');
const proctoringVideosFactory = require('tests/factory/proctoringVideosFactory');
const userIdentificationsFactory = require('tests/factory/userIdentificationsFactory');
const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const nockTvm = require('tests/helpers/nockTvm');

describe('Takeout get user data controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        nockTvm.checkTicket({ src: 1234 });
    });

    afterEach(nock.cleanAll);

    it('should return correct user data', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        const user = {
            login: 'fox',
            firstname: 'Orange',
            lastname: 'Firefox',
            uid: 1029384756
        };
        const started = new Date(2019, 5, 9);
        const finished = new Date(2019, 5, 10);
        const trialData = {
            started,
            finished,
            passed: 0,
            nullified: 0
        };
        const trialTemplate = { title: 'Сертификация по&nbsp;тейкауту' };

        yield trialsFactory.createWithRelations(trialData, { user, trialTemplate });

        const expected = {
            login: 'fox',
            firstname: 'Orange',
            lastname: 'Firefox',
            attempts: [
                {
                    started,
                    finished,
                    passed: 0,
                    nullified: 0,
                    exam: 'Сертификация по тейкауту'
                }
            ]
        };

        yield request
            .post('/v1/user/takeout')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .type('application/x-www-form-urlencoded')
            .send({ uid: 1029384756, unixtime: 123 })
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                status: 'ok',
                data: {
                    'user.json': JSON.stringify(expected)
                }
            })
            .end();
    });

    it('should return correct `file_links`', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        const trialTemplate = { id: 3 };
        const user = { id: 12, uid: 12345 };

        yield proctoringVideosFactory.createWithRelations(
            { name: 'video1.webm', startTime: 123 },
            { trial: { id: 1 }, user, trialTemplate }
        );
        yield proctoringVideosFactory.createWithRelations(
            { name: 'video2.webm', startTime: 456 },
            { trial: { id: 1 }, user, trialTemplate }
        );
        yield userIdentificationsFactory.createWithRelations(
            { id: 1, face: 'faces/face.jpg', document: 'documents/doc.jpg' },
            { user, trialTemplate }
        );

        const expectedLinks = [
            'https://test-host.ru/v1/user/takeout/videos/video1.webm',
            'https://test-host.ru/v1/user/takeout/videos/video2.webm',
            'https://test-host.ru/v1/user/takeout/faces/face.jpg',
            'https://test-host.ru/v1/user/takeout/documents/doc.jpg'
        ];

        yield request
            .post('/v1/user/takeout')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .type('application/x-www-form-urlencoded')
            .send({ uid: 12345 })
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(({ body: { file_links } }) => { // eslint-disable-line camelcase
                expect(file_links).to.deep.equal(expectedLinks);
            })
            .end();
    });

    it('should return `no_data` when user does not exist', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        yield request
            .post('/v1/user/takeout')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .type('application/x-www-form-urlencoded')
            .send({ uid: 123456789 })
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                status: 'no_data'
            })
            .end();
    });

    it('should return 400 when uid is invalid', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        yield request
            .post('/v1/user/takeout')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .type('application/x-www-form-urlencoded')
            .send({ uid: 'not_a_number' })
            .expect(400)
            .expect({
                message: 'Uid is invalid',
                internalCode: '400_UII',
                uid: 'not_a_number'
            })
            .end();
    });

    it('should return 403 when client is not takeout', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'testTvmClient' });

        yield request
            .post('/v1/user/takeout')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .expect(403)
            .expect({
                message: 'Client has no access',
                internalCode: '403_CNA',
                tvmClient: 'testTvmClient'
            })
            .end();
    });
});
