import { buildUrlWithZone, buildUrlWithUserQuery, buildUrlWithLang, buildUrlWithParams } from './url-utils';

describe('Lib. url-utils.', () => {
    describe('#buildUrlWithZone', () => {
        it('Должен верно подменить зону', () => {
            expect(buildUrlWithZone('zone=%zone%', 'test'))
                .toStrictEqual<string>('zone=test');
        });
        it('Должен верно отработать если зона не задана', () => {
            expect(buildUrlWithZone('zone=%zone%'))
                .toStrictEqual<string>('zone=');
        });
        it('Должен верно отработать если зона задана в path', () => {
            expect(buildUrlWithZone('/%zone%/tariff', 'test'))
                .toStrictEqual<string>('/test/tariff');
        });
    });

    describe('#buildUrlWithUserQuery', () => {
        it('Должен верно подменить параметры', () => {
            expect(buildUrlWithUserQuery('id=%id%&orderid=%orderid%', 'usertest', 'ordertest'))
                .toStrictEqual<string>('id=usertest&orderid=ordertest');
        });
        it('Должен верно отработать если параметры не заданы', () => {
            expect(buildUrlWithUserQuery('id=%id%&orderid=%orderid%'))
                .toStrictEqual<string>('id=&orderid=');
        });
    });

    describe('#buildUrlWithLang', () => {
        it('Должен верно подменить параметры', () => {
            expect(buildUrlWithLang('lang=%lang%', 'test'))
                .toStrictEqual<string>('lang=test');
        });
        it('Должен верно отработать если параметры не заданы', () => {
            expect(buildUrlWithLang('lang=%lang%'))
                .toStrictEqual<string>('lang=');
        });
    });

    describe('#buildUrlWithParams', () => {
        it('Должен верно подменить параметры', () => {
            expect(buildUrlWithParams('id=%id%&orderid=%orderid%&lang=%lang%&zone=%zone%', {
                zoneName: 'zonetest',
                userId: 'idtest',
                orderId: 'ordertest',
                lang: 'langtest',
            }))
                .toStrictEqual<string>('id=idtest&orderid=ordertest&lang=langtest&zone=zonetest');
        });
    });
});
