require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nock = require('nock');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const proctoringVideosFactory = require('tests/factory/proctoringVideosFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');

describe('Admin get videos controller', () => {
    before(() => {
        nockBlackbox({
            response: { uid: { value: '1234567890' } }
        });
    });

    after(nock.cleanAll);

    beforeEach(dbHelper.clear);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success return links to videos and pdf', function *() {
        yield createAdmin();

        const trial = { id: 234, openId: 'testOpenId', pdf: 'some-name.pdf' };

        yield proctoringVideosFactory.createWithRelations({ name: '11.webm', startTime: 11 }, { trial });
        yield proctoringVideosFactory.createWithRelations({ name: '22.webm', startTime: 22 }, { trial });

        yield certificatesFactory.createWithRelations({ id: 17 }, { trial });
        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                source: 'appeal',
                isLast: true,
                time: new Date(1, 1, 1),
                additionalInfo: null
            },
            { trial }
        );

        const tolokersVerdicts = {
            start: 0,
            end: 25,
            answers: [
                { noVioAudioProblems: false, vioCheating: true },
                { vioWalkAwayScreen: false, vioDiffUser: false }
            ]
        };

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                source: 'toloka',
                isLast: false,
                time: new Date(2, 2, 2),
                additionalInfo: tolokersVerdicts
            },
            { trial }
        );

        yield request
            .get('/v1/admin/videos/234')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                videos: [
                    'https://yastatic.net/s3/expert/testing/videos/11.webm',
                    'https://yastatic.net/s3/expert/testing/videos/22.webm'
                ],
                pdfLink: 'https://yastatic.net/s3/expert/testing/pdf/some-name.pdf',
                appealData: { certId: 17, verdict: 'failed' },
                additionalInfo: [{ source: 'toloka', additionalInfo: tolokersVerdicts }]
            })
            .end();
    });

    it('should throw 400 when attempt id is invalid', function *() {
        yield request
            .get('/v1/admin/videos/abc')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/videos/1')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no access to revise video', function *() {
        yield request
            .get('/v1/admin/videos/1')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no access to revise video',
                internalCode: '403_NRV'
            })
            .end();
    });

    it('should throw 404 when attempt not found', function *() {
        yield createAdmin();

        yield request
            .get('/v1/admin/videos/1')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });
});
