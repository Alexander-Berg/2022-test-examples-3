require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const dbHelper = require('tests/helpers/clear');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;

const { ProctoringResponses, User } = require('db/postgres');

describe('Attempt needRevision controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(nock.cleanAll);

    const user = { id: 1, uid: 1234567890, email: null };
    const authType = { id: 2, code: 'web' };

    const emailFormData = JSON.stringify({
        question: { slug: 'email' },
        value: 'expert@yandex.ru'
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        yield proctoringResponsesFactory.createWithRelations(
            {
                source: 'proctoring',
                verdict: 'failed',
                isRevisionRequested: false,
                isLast: true
            },
            { trial: { id: 123, passed: 1 }, user, authType }
        );
    });

    it('should return 200 when request for revision was success', function *() {
        yield request
            .post('/v1/attempt/123/needRevision')
            .field('field_1', emailFormData)
            .expect(200)
            .end();

        const actual = yield ProctoringResponses.findAll({
            attributes: ['isRevisionRequested']
        });

        expect(actual.length).to.equal(1);
        expect(actual[0].isRevisionRequested).to.be.true;
    });

    it('should save email', function *() {
        yield request
            .post('/v1/attempt/123/needRevision')
            .field('email', emailFormData)
            .expect(200)
            .end();

        const actual = yield User.findAll({
            attributes: ['id', 'email'],
            raw: true
        });

        expect(actual).to.deep.equal([{ id: 1, email: 'expert@yandex.ru' }]);
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield request
            .post('/v1/attempt/abc/needRevision')
            .expect(400)
            .expect({ message: 'Attempt id is invalid', internalCode: '400_AII', attemptId: 'abc' })
            .end();
    });

    it('should return 400 when email is invalid', function *() {
        yield request
            .post('/v1/attempt/123/needRevision')
            .field('email', 'expert')
            .expect(400)
            .expect({ message: 'User email is invalid', internalCode: '400_UEI' })
            .end();
    });

    it('should return 403 when revision is not available', function *() {
        yield proctoringResponsesFactory.createWithRelations(
            {
                source: 'appeal',
                verdict: 'failed',
                isRevisionRequested: false,
                isLast: true
            },
            { trial: { id: 345 }, user, authType }
        );

        yield request
            .post('/v1/attempt/345/needRevision')
            .field('email', emailFormData)
            .expect(403)
            .expect({
                message: 'Revision is not available',
                internalCode: '403_RNA',
                attemptId: 345
            })
            .end();
    });

    it('should return 404 when attempt is not found', function *() {
        yield request
            .post('/v1/attempt/456/needRevision')
            .field('email', emailFormData)
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF',
                attemptId: 456
            })
            .end();
    });
});
