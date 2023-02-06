require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');

const trialsFactory = require('tests/factory/trialsFactory');

describe('Attempt get proctoring metrics', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    it('should return proctoring metrics', function *() {
        const user = { id: 234, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };

        yield trialsFactory.createWithRelations({
            id: 323,
            proctoringMetrics: {
                critical: [
                    { metric: 'c4', duration: 3000 }
                ]
            }
        }, { user, authType });

        const res = yield request
            .get('/v1/attempt/323/proctoringMetrics')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;
        const expected = {
            critical: [
                { metric: 'c4', duration: 3000 }
            ]
        };

        expect(actual).to.deep.equal(expected);
    });

    it('should return empty proctoring metrics', function *() {
        const user = { id: 234, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };

        yield trialsFactory.createWithRelations({
            id: 323
        }, { user, authType });

        const res = yield request
            .get('/v1/attempt/323/proctoringMetrics')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;
        const expected = {};

        expect(actual).to.deep.equal(expected);
    });

    it('should throw 400 when attempt id is invalid', function *() {
        yield request
            .get('/v1/attempt/invalid/proctoringMetrics')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'invalid'
            })
            .end();
    });

    it('should throw 404 when attempt is not found', function *() {
        yield request
            .get('/v1/attempt/323/proctoringMetrics')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 403 when user has no access to attempt', function *() {
        const user = { id: 234, uid: 1234567890 };
        const otherUser = { id: 235, uid: 11111111 };
        const authType = { id: 2, code: 'web' };

        yield trialsFactory.createWithRelations({
            id: 323
        }, { user, authType });
        yield trialsFactory.createWithRelations({
            id: 324,
            proctoringMetrics: {
                critical: [
                    { metric: 'c1', duration: 9000 }
                ]
            }
        }, { user: otherUser, authType });

        yield request
            .get('/v1/attempt/324/proctoringMetrics')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'Illegal user for attempt',
                internalCode: '403_IUA'
            })
            .end();
    });
});
