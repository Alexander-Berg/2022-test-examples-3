import querystring, { ParsedUrlQuery } from 'querystring';
import FrontendSignUrl from '.';

const keysJsonFile = `${__dirname}/__fixtures/clickdaemon_keys.json`;
const redirFrom = 'some.host.yandex.net:10100;/search?text=hello&lr=2;web;;123';
const etext = '1194.pO4iQbLNihjfduMkxV7NK1IQICUoQqlWfbU4cE50uZA.7d47735b89ade520d8b0f73789a8463217aaf5ec';

const signurl = new FrontendSignUrl(keysJsonFile);

describe('#signUrl', () => {
    it('should return query string with bunch of parameters', () => {
        const redirPrefix = {
            prefix_const:
                'dtype=iweb/reg=213/u=12345678901234/ruip=192.168.1.1/ids=28668,28158,28101,25466/slots=a,b;c,d/ver=123/uuid=0123456789abcdef0123456789abcdef/reqid=1234567890-TST1/',
            prefix: 'path=/*',
            referer: 'https://hamster.yandex.ru/search/?text=cats&lr=2',
            uuid: '0123456789abcdef0123456789abcdef',
        };

        const signedUrl = signurl.callSignUrl(
            redirPrefix,
            'http://ya.ru',
            'WEB_0',
            'uid:123',
            null,
            null,
            'Ya.ru',
            redirFrom,
            etext,
        );

        let parsed: ParsedUrlQuery = querystring.parse('');
        expect(() => {
            parsed = querystring.parse(signedUrl);
        }).not.toThrow();
        expect(parsed.from).toBe(redirFrom);
        expect(parsed.text).toBe('Ya.ru');
        expect(parsed.uuid).toBe('0123456789abcdef0123456789abcdef');
        expect(typeof parsed.state).toBe('string');
        expect(typeof parsed.data).toBe('string');
        expect(parsed.b64e).toBe('2');
        expect(typeof parsed.sign).toBe('string');
        expect(parsed.keyno).toBe('WEB_0');
        expect(typeof parsed.cst).toBe('string');
    });

    it('should return query string with empty uuid if passed prefix object does not contains any', () => {
        const redirPrefix = {
            prefix_const:
                'dtype=iweb/reg=213/u=12345678901234/ruip=192.168.1.1/ids=28668,28158,28101,25466/slots=a,b;c,d/ver=123/reqid=1234567890-TST1/',
            prefix: 'path=/*',
            referer: 'https://hamster.yandex.ru/search/?text=cats&lr=2',
            uuid: '',
        };

        const signedUrl = signurl.callSignUrl(
            redirPrefix,
            'http://ya.ru',
            'WEB_0',
            null,
            null,
            null,
            'Ya.ru',
            redirFrom,
            etext,
        );

        const parsed = querystring.parse(signedUrl);
        expect(parsed.uuid).toBe('');
    });

    it('should properly work if prefix is a string', () => {
        const signedUrl = signurl.callSignUrl(
            '*',
            'http://www.youtube.com/watch?v=tLqezfYIHS0',
            'WEB_0',
            '3318355581451330984',
            null,
            null,
            'bmw',
            'hamster.yandex.ru;video/search;web;;',
            etext,
        );
        const parsed = querystring.parse(signedUrl);

        expect(parsed.state).toEqual('EIW2pfxuI9g,');
        expect(parsed.uuid).toEqual('');
        expect(parsed.ref).not.toBeDefined();
        expect(parsed.cst).not.toBeDefined();
    });

    it('should properly work withour etext', () => {
        const signedUrl = signurl.callSignUrl(
            '*',
            'http://www.youtube.com/watch?v=tLqezfYIHS0',
            'WEB_0',
            '3318355581451330984',
            null,
            null,
            'bmw',
            'hamster.yandex.ru;video/search;web;;',
        );
        const parsed = querystring.parse(signedUrl);

        expect(parsed.state).toEqual('EIW2pfxuI9g,');
        expect(parsed.uuid).toEqual('');
        expect(parsed.ref).not.toBeDefined();
        expect(parsed.cst).not.toBeDefined();
    });

    describe('should return encrypted text for jsRedir=2', () => {
        const callSignUrl = (etext?: string | null): string =>
            signurl.callSignUrl(
                '*',
                'http://www.youtube.com/watch?v=tLqezfYIHS0',
                'WEB_0',
                '3318355581451330984',
                2,
                2,
                'It+works+%21+/ололо',
                'hamster.yandex.ru;video/search;web;;',
                etext,
            );

        it('with proper etext', () => {
            const signedUrl = callSignUrl(etext);
            const parsed = querystring.parse(signedUrl);

            expect(parsed.text).toEqual('');
            expect(parsed.etext).toEqual(etext);
        });

        it('with undefined etext', () => {
            const signedUrl = callSignUrl(undefined);
            const parsed = querystring.parse(signedUrl);

            expect(parsed.text).toEqual('It works ! /ололо');
            expect(parsed.etext).toEqual('');
        });

        it('with null etext', () => {
            const signedUrl = callSignUrl(null);
            const parsed = querystring.parse(signedUrl);

            expect(parsed.text).toEqual('It works ! /ололо');
            expect(parsed.etext).toEqual('');
        });
    });

    it('should handle string keyno', () => {
        const signedUrl = signurl.callSignUrl(
            '*',
            'http://www.youtube.com/watch?v=tLqezfYIHS0',
            'WEB_0',
            '3318355581451330984',
            2,
            2,
            'It+works+%21+/ололо',
            'hamster.yandex.ru;video/search;web;;',
            etext,
        );
        const parsed = querystring.parse(signedUrl);

        expect(parsed.text).toEqual('');
        expect(parsed.etext).toEqual(etext);
    });
});
