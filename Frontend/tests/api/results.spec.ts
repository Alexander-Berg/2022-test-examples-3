import { Server } from 'http';
import nock from 'nock';
import request from 'supertest';
import sinon, { SinonStub } from 'sinon';

import configureServer from 'src/server/tests/helpers/server';
import * as nocks from 'src/server/tests/nocks';
import { Verdict } from 'src/shared/types/verdict';

describe('GET /api/results', () => {
    let server: Server;

    const currentTime = 1592686679317;

    beforeEach(async () => {
        server = await configureServer(8080);

        nocks.nockGeobase();
        nocks.nockLangdetect({});
        nocks.nockUatraits({});
        nocks.nockTvm({
            ticket: 'some-ticket',
        });

        sinon.stub(Date, 'now').returns(currentTime);
    });

    afterEach(() => {
        server.close();

        nock.cleanAll();

        (Date.now as SinonStub).restore();
    });

    it('should return correct results', async () => {
        nocks.nockParticipants({
            contestId: '8458',
            query: {
                login: 'semenmakhaev',
            },
            response: [{ id: 1111, login: 'semenmakhaev', name: 'Джессика Джонсон' }],
        });

        nocks.nockRuns({
            contestId: '8458',
            participantId: 1111,
            response: {
                contestId: 8458,
                firstSubmissionTime: '2018-08-16T15:40:33.511Z',
                startedAt: null,
                login: 'user',
                name: 'user',
                runs: [
                    {
                        submissionTime: 1111,
                        verdict: Verdict.OK,
                        problemAlias: 'A',
                        compiler: 'js-bliz',
                        finalScore: '100.0',
                        checkerLog: [],
                        source: 'console.log(`Hello world!`);',
                        timeFromStart: 5801459,
                    },
                ],
            },
        });

        nocks.nockContest({
            contestId: '8458',
            response: {
                startTime: '1970-01-01T00:00:00.000Z',
                name: 'Test Interview',
                duration: 10800,
            },
        });

        await request(server)
            .get('/api/results')
            .query({
                login: 'semenmakhaev',
                contest: 'trial',
            })
            .set('Cookie', 'Session_id=1111')
            .expect(200)
            .expect('content-type', 'text/html; charset=utf-8')
            .expect((response) => response.text.startsWith('<div'));
    });

    it('should return participants', async () => {
        nocks.nockParticipants({
            contestId: '8458',
            query: {
                login: 'semenma',
            },
            response: [{ id: 1111, login: 'semenmakhaev', name: 'Джессика Джонсон' }],
        });

        await request(server)
            .get('/api/participants')
            .query({
                login: 'semenma',
                contest: 'trial',
            })
            .set('Cookie', 'Session_id=1111')
            .expect(200)
            .expect([{ id: 1111, login: 'semenmakhaev', name: 'Джессика Джонсон' }]);
    });

    it('should redirect to Yandex Passport when user is not authorized', async () => {
        await request(server)
            .get('/api/results')
            .query({
                login: 'semenmakhaev',
                contest: 'trial',
            })
            .set('Cookie', '') // unauthorized
            .expect(302)
            .expect(
                'Location',
                'https://passport.yandex-team.ru/passport?mode=auth&retpath=https%3A%2F%2F127.0.0.1%2Fapi%2Fresults%25253Flogin%3Dsemenmakhaev%26contest%3Dtrial',
            );
    });
});
