require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const _ = require('lodash');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

describe('Exam findByCluster controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        const trialTemplate = {
            active: 1,
            delays: '',
            language: 0,
            isProctoring: false,
            clusterSlug: 'testCluster'
        };

        const firstTemplateData = _.extend({}, trialTemplate, { id: 2, slug: 'test' });
        const secondTemplateData = _.extend({}, trialTemplate, {
            id: 3,
            slug: 'test-pro',
            isProctoring: true
        });

        yield trialTemplatesFactory.createWithRelations(firstTemplateData);
        yield trialTemplatesFactory.createWithRelations(secondTemplateData);
    });

    before(() => {
        nockBlackbox({ response: { uid: { value: 'current user' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    it('should return exams by cluster', function *() {
        const expected = [
            {
                id: 2,
                active: 1,
                delays: [],
                language: 'ru',
                slug: 'test',
                isProctoring: false,
                clusterSlug: 'testCluster'
            },
            {
                id: 3,
                active: 1,
                delays: [],
                language: 'ru',
                slug: 'test-pro',
                isProctoring: true,
                clusterSlug: 'testCluster'
            }
        ];

        yield request
            .get('/v1/exam/cluster/testCluster')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(expected)
            .end();
    });

    it('should return 400 when `clusterSlug` is invalid', function *() {
        yield request
            .get('/v1/exam/cluster/!$^')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                internalCode: '400_CSI',
                message: 'Cluster slug is invalid',
                clusterSlug: '!$^'
            })
            .end();
    });

    it('should return 404 when exams by cluster not found', function *() {
        yield request
            .get('/v1/exam/cluster/100500')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                internalCode: '404_ECN',
                message: 'Exams by cluster not found',
                clusterSlug: '100500'
            })
            .end();
    });
});
