import { IRequestSource } from '~/report-renderer/apphost/types';
import { getBlackboxRequest } from '~/report-renderer/actions/blackbox_pre/request';

// @ts-ignore
const request: IRequestSource = {
    cookies_parsed: {
        Session_id: 'example'
    },
    headers: {},
    ip: 'userip',
    hostname: 'ecom-sins.yandex.ru'
};

describe('getBlackboxRequest()', () => {
    test('Возвращает undefined, если пользователь нет sessionId', () => {
        const httpRequest = getBlackboxRequest({ request: { ...request, cookies_parsed: {} } });

        expect(httpRequest).toBeUndefined();
    });

    test('Возвращает корректный запрос к blackbox, если есть sessionId', () => {
        const httpRequest = getBlackboxRequest({ request });

        const expectedSessionId = request.cookies_parsed.Session_id;

        expect(httpRequest).toEqual({
            method: 0,
            scheme: 2,
            path: `/blackbox?method=sessionid&format=json&get_user_ticket=yes&sessionid=${expectedSessionId}&userip=${request.ip}&host=${request.hostname}`
        });
    });
});
