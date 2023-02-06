import { Server } from 'http';
import { format } from 'url';
import nock from 'nock';
import request from 'supertest';

import config from '@yandex-int/yandex-cfg';

import { HistoryItem } from 'src/shared/types/history';

import configureServer from 'src/server/tests/helpers/server';
import * as nocks from 'src/server/tests/nocks';

describe('GET /api/history', () => {
    let server: Server;

    beforeEach(async () => {
        server = await configureServer(8080);

        nocks.nockWebauth({
            sessionId: 'session-id',
            sessionId2: 'session-id-2',
        });
        nocks.nockBlackbox({
            response: {
                error: 'OK',
                uid: {
                    value: '1120000000050176',
                    lite: false,
                    hosted: false,
                },
                login: 'semenmakhaev',
                regname: 'semenmakhaev',
                display_name: {
                    name: 'semenmakhaev',
                },
                attributes: {
                    '31': 'ru',
                    '33': 'Asia/Yekaterinburg',
                    '34': 'ru',
                    '14': 'semenmakhaev@yandex-team.ru',
                    '1008': 'semenmakhaev',
                },
                status: {
                    value: 'VALID',
                    id: 0,
                },
            },
            ticket: 'some-ticket',
        });
        nocks.nockGeobase();
        nocks.nockLangdetect({});
        nocks.nockUatraits({});
        nocks.nockTvm({
            ticket: 'some-ticket',
        });
    });

    afterEach(() => {
        server.close();

        nock.cleanAll();
    });

    it('should reply with history', async () => {
        const history: HistoryItem[] = [
            {
                createdAt: '2021-04-26T16:42:53.944095',
                content: 'The grand opening will start in 15 minutes',
            },
            {
                createdAt: '2021-04-27T10:20:22.60924',
                content: 'Main events today: the grand opening, plenary',
            },
        ];

        nock(format(config.api))
            .get('/history')
            .matchHeader('x-ya-service-ticket', 'some-ticket')
            .matchHeader('x-uid', '1120000000050176')
            .matchHeader('x-request-id', 'request-id')
            .once()
            .reply(200, history);

        await request(server)
            .get('/api/history')
            .set('Cookie', 'Session_id=session-id; sessionid2=session-id-2')
            .set('x-request-id', 'request-id')
            .expect(200)
            .expect(history);
    });

    it('should throw 500 when user has no access', async () => {
        nock(format(config.api))
            .get('/history')
            .matchHeader('x-ya-service-ticket', 'some-ticket')
            .matchHeader('x-uid', '1120000000050176')
            .matchHeader('x-request-id', 'request-id')
            .once()
            .reply(403, {
                i18nParams: { message: 'Access denied', code: '403_AD' },
            });

        await request(server)
            .get('/api/history')
            .set('Cookie', 'Session_id=session-id; sessionid2=session-id-2')
            .set('x-request-id', 'request-id')
            .expect(500);
    });
});
