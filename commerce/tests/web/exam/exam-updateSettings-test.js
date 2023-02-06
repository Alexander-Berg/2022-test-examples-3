require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');

const { TrialTemplate } = require('db/postgres');

describe('Exam updateSettings controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        yield trialTemplatesFactory.createWithRelations(
            {
                id: 1,
                timeLimit: 1000,
                title: 'not updated title',
                rules: 'old rules',
                description: 'old description',
                seoDescription: 'old seo',
                ogDescription: 'old og'
            },
            { service: { id: 2 } }
        );
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890 };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success update exam settings', function *() {
        yield createAdmin();
        yield trialTemplatesFactory.createWithRelations(
            {
                id: 2,
                timeLimit: 3000,
                title: 'title',
                rules: 'r',
                description: 'd',
                seoDescription: 's',
                ogDescription: 'o'
            },
            { service: { id: 2 } }
        );

        const res = yield request
            .post('/v1/exam/1/settings')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({
                timeLimit: 2000,
                title: 'new title',
                rules: 'new rules',
                description: 'new description',
                seoDescription: 'new seo',
                ogDescription: 'new og'
            })
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.title).to.equal('new title');
        expect(actual.timeLimit).to.equal(2000);
        expect(actual.rules).to.equal('new rules');
        expect(actual.description).to.equal('new description');
        expect(actual.seoDescription).to.equal('new seo');
        expect(actual.ogDescription).to.equal('new og');

        const actualSaved = yield TrialTemplate.findAll({
            attributes: [
                'title',
                'timeLimit',
                'rules',
                'description',
                'seoDescription',
                'ogDescription'
            ],
            order: ['id']
        });
        const firstActual = actualSaved[0].toJSON();
        const secondActual = actualSaved[1].toJSON();

        expect(firstActual).to.deep.equal({
            title: 'new title',
            timeLimit: 2000,
            rules: 'new rules',
            description: 'new description',
            seoDescription: 'new seo',
            ogDescription: 'new og',
            delays: [],
            language: 'ru'
        });

        expect(secondActual).to.deep.equal({
            title: 'title',
            timeLimit: 3000,
            rules: 'r',
            description: 'd',
            seoDescription: 's',
            ogDescription: 'o',
            delays: [],
            language: 'ru'
        });
    });

    it('should throw 400 when timeLimit is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/1/settings')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ timeLimit: '1h' })
            .expect(400)
            .expect({
                message: 'Time limit is invalid',
                internalCode: '400_TLI',
                timeLimit: '1h'
            })
            .end();
    });

    it('should throw 400 when title is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/1/settings')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ timeLimit: 1000, title: 2 })
            .expect(400)
            .expect({
                message: 'Field is not a string',
                internalCode: '400_FNS',
                title: 2
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/exam/1/settings')
            .send({})
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not admin', function *() {
        yield request
            .post('/v1/exam/1/settings')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({})
            .expect(403)
            .expect({
                message: 'User is not admin',
                internalCode: '403_NAD'
            })
            .end();
    });

    it('should throw 404 when test not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/2/settings')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({})
            .expect(404)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });
});
