require('co-mocha');

const fs = require('fs');
let api = require('api');
const { expect } = require('chai');
let request = require('co-supertest').agent(api.callback());
const mockery = require('mockery');
const sinon = require('sinon');
const mockS3 = require('tests/helpers/s3');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const usersFactory = require('tests/factory/usersFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const authTypesFactory = require('tests/factory/authTypesFactory');
const rolesFactory = require('tests/factory/rolesFactory');

const { UserIdentification, User } = require('db/postgres');

describe('User create photo controller', () => {
    const authType = { id: 2, code: 'web' };
    const photo = fs.readFileSync('tests/models/userIdentification/mock-photo').toString();
    const now = 456;
    const userId = 23;

    beforeEach(function *() {
        yield dbHelper.clear();
        yield usersFactory.createWithRelations({
            id: userId,
            uid: 1234567890
        }, { authType });
        yield trialTemplatesFactory.createWithRelations({ id: 1 });
    });

    before(() => {
        nockBlackbox({ response: {
            uid: { value: 1234567890 }
        } });
        mockS3(true);
        sinon.stub(Date, 'now').returns(now);

        api = require('api');
        request = require('co-supertest').agent(api.callback());
    });

    after(() => {
        require('nock').cleanAll();

        mockery.disable();
        mockery.deregisterAll();

        Date.now.restore();
    });

    it('should save photos correctly', function *() {
        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: photo, document: photo })
            .expect(201)
            .end();

        const mockS3Path = `${userId}_${now}`;
        const entities = yield UserIdentification.findAll({
            attributes: ['document', 'face'],
            raw: true
        });

        expect(entities).to.deep.equal([
            {
                document: `documents/${mockS3Path}.jpg`,
                face: `faces/${mockS3Path}.jpg`
            }
        ]);
    });

    it('should save photos when there is no user in database with blackbox uid', function *() {
        yield dbHelper.clear();
        yield authTypesFactory.create(authType);
        yield rolesFactory.create({ id: 1 });
        yield trialTemplatesFactory.createWithRelations({ id: 1 });

        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: photo, document: photo })
            .expect(201)
            .end();

        const users = yield User.findAll({
            where: { uid: 1234567890 }
        });

        expect(users).to.have.length(1);
    });

    it('should return 400 when examIdentity is not exam id or slug', function *() {
        const examIdentity = 'DROP%20DATABASE%20expert;--';

        yield request
            .post(`/v1/identification/${examIdentity}/photo`)
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400, {
                message: 'Exam identity is incorrect',
                internalCode: '400_EII',
                examIdentity: decodeURIComponent(examIdentity)
            })
            .end();
    });

    it('should return 400 when face photo was not set', function *() {
        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ document: photo })
            .expect(400, {
                message: 'Face photo is absent',
                internalCode: '400_PIA'
            })
            .end();
    });

    it('should return 400 when document photo was not set', function *() {
        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: photo })
            .expect(400, {
                message: 'Document photo is absent',
                internalCode: '400_PIA'
            })
            .end();
    });

    it('should return 400 when file format is incorrect', function *() {
        const incorrectMime = fs.readFileSync('tests/web/user/user-createPhoto-test.js')
            .toString();

        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: incorrectMime, document: photo })
            .expect(400, {
                message: 'Photo is incorrect',
                internalCode: '400_PII'
            })
            .end();
    });

    it('should return 401 when user is not authorized', function *() {
        yield request
            .post('/v1/identification/1/photo')
            .send({ face: photo, document: photo })
            .expect(401, {
                internalCode: '401_UNA',
                message: 'User not authorized'
            })
            .end();
    });

    it('should return 403 when user has no access to create attempt', function *() {
        yield trialsFactory.createWithRelations({
            userId: 23,
            trialTemplateId: 1,
            state: 'in_progress'
        }, {});

        yield request
            .post('/v1/identification/1/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: photo, document: photo })
            .expect(403, {
                attemptId: 1000,
                internalCode: '403_AAS',
                message: 'Attempt is already started'
            })
            .end();
    });

    it('should return 404 when exam not found', function *() {
        yield request
            .post('/v1/identification/not-existent-exam/photo')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ face: photo, document: photo })
            .expect(404, {
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });
});
