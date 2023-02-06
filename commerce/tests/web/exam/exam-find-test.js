require('co-mocha');

const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');

describe('Exam find controller', () => {
    let type;
    let service;

    beforeEach(function *() {
        yield dbHelper.clear();

        type = { id: 3, code: 'type code', title: 'type title' };
        service = { id: 4, code: 'service code', title: 'service title' };
        const trialTemplate = {
            id: 2,
            active: 2,
            title: 'Test title',
            previewCert: 'Test url to preview',
            allowedTriesCount: 5,
            timeLimit: 98965,
            delays: '1M',
            validityPeriod: '3M',
            periodBeforeCertificateReset: '1M',
            delayUntilTrialsReset: '1M',
            language: 0,
            slug: 'testExam',
            isProctoring: false,
            clusterSlug: 'test',
            description: 'DESCRIPTION',
            rules: 'RULES',
            seoDescription: 'SEO',
            ogDescription: 'OG'
        };

        // First sections data
        let section = { id: 4, code: 'first' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 1, quantity: 1 },
            { trialTemplate, section, type, service }
        );
        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 2, quantity: 2 },
            { trialTemplate, section, type, service }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section, type, service }
        );

        // Second sections data
        section = { id: 5, code: 'second' };
        yield trialTemplateToSectionsFactory.createWithRelations(
            { categoryId: 1, quantity: 3 },
            { trialTemplate, section, type, service }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 2 },
            { trialTemplate, section, type, service }
        );
    });

    before(() => {
        nockBlackbox({ response: { uid: { value: 'current user' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    it('should return exam by id', function *() {
        const actual = {
            id: 2,
            active: 2,
            service: { id: 4, code: 'service code', title: 'service title' },
            type: { id: 3, code: 'type code', title: 'type title' },
            title: 'Test title',
            previewCert: 'Test url to preview',
            questionsCount: 6,
            allowedTriesCount: 5,
            sectionsCount: 2,
            timeLimit: 98965,
            allowedFails: 3,
            delays: ['1M'],
            validityPeriod: '3M',
            periodBeforeCertificateReset: '1M',
            delayUntilTrialsReset: '1M',
            language: 'ru',
            slug: 'testExam',
            isProctoring: false,
            clusterSlug: 'test',
            description: 'DESCRIPTION',
            rules: 'RULES',
            seoDescription: 'SEO',
            ogDescription: 'OG'
        };

        yield request
            .get('/v1/exam/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(actual)
            .end();
    });

    it('should return exam by slug', function *() {
        const actual = {
            id: 2,
            active: 2,
            service: { id: 4, code: 'service code', title: 'service title' },
            type: { id: 3, code: 'type code', title: 'type title' },
            title: 'Test title',
            previewCert: 'Test url to preview',
            questionsCount: 6,
            allowedTriesCount: 5,
            sectionsCount: 2,
            timeLimit: 98965,
            allowedFails: 3,
            delays: ['1M'],
            validityPeriod: '3M',
            periodBeforeCertificateReset: '1M',
            delayUntilTrialsReset: '1M',
            language: 'ru',
            slug: 'testExam',
            isProctoring: false,
            clusterSlug: 'test',
            description: 'DESCRIPTION',
            rules: 'RULES',
            seoDescription: 'SEO',
            ogDescription: 'OG'
        };

        yield request
            .get('/v1/exam/testExam')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(actual)
            .end();
    });

    it('should return 404 when exam not found', function *() {
        yield request
            .get('/v1/exam/100500')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                internalCode: '404_TNF',
                message: 'Test not found'
            })
            .end();
    });

    it('should return 400 when `identity` is invalid', function *() {
        yield request
            .get('/v1/exam/!$^')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                internalCode: '400_EII',
                message: 'Exam identity is invalid',
                identity: '!$^'
            })
            .end();
    });
});
