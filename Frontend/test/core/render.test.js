const { canMakeRedirect, isArgPositive, getRedirectUrl, doNeedRedirect } = require('../../core/render');

describe('render', () => {
    describe('doNeedRedirect', () => {
        let data;

        beforeEach(() => {
            data = {
                doc: {},
                env: {
                    expFlags: {},
                },
                reqdata: {
                    device_detect: {},
                },
            };
        });

        it('должен вернуть false, когда отсутсвует doc', () => {
            delete data.doc;

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть false, когда в doc есть флаг "fallbackDisabled=true"', () => {
            data.doc.fallbackDisabled = true;

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть false, когда есть флаг "fallback-disabled"', () => {
            data.env.expFlags = { 'fallback-disabled': 1 };

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть false, когда устройство поддерживается', () => {
            data.reqdata.device_detect.OSFamily = 'windows';
            data.reqdata.device_detect.BrowserName = 'chrome';

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть true, когда устройство не поддерживается', () => {
            data.reqdata.device_detect.BrowserName = 'iemobile';

            expect(doNeedRedirect(data)).toBe(true);
        });

        it('должен вернуть fasle, когда устройство не поддерживается, но есть флаг "fallback-disabled"', () => {
            data.reqdata.device_detect.BrowserName = 'iemobile';
            data.env.expFlags = { 'fallback-disabled': 1 };

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть fasle, когда устройство не поддерживается, но есть флаг "fallbackDisabled=true" в doc ', () => {
            data.reqdata.device_detect.BrowserName = 'iemobile';
            data.doc.fallbackDisabled = true;

            expect(doNeedRedirect(data)).toBe(false);
        });

        it('должен вернуть false, если выставлено свойство "noredirect"', () => {
            data.reqdata.special_prefs = { noredirect: true };

            expect(doNeedRedirect(data)).toBe(false);
        });
    });

    describe('canMakeRedirect', () => {
        let data;

        beforeEach(() => {
            data = {
                doc: {
                    url: 'https://www.kommersant.ru/doc/3750682',
                },
                reqdata: {
                    url: 'https://www.yandex.ru/turbo?text=https%3A%2F%2Fwww.kommersant.ru%2Fdoc%2F3750682',
                },
            };
        });

        it('должен вернуть true, когда редирект возможен', () => {
            expect(canMakeRedirect(data)).toBe(true);
        });

        it('должен вернуть false, когда нет оригинального урла', () => {
            data.doc.url = undefined;
            expect(!canMakeRedirect(data)).toBe(true);
        });

        it('должен вернуть false, когда оригинальный урл такой же, как и текущий', () => {
            data.doc.url = data.reqdata.url;
            expect(!canMakeRedirect(data)).toBe(true);
        });
    });

    describe('isArgPositive', () => {
        let data;

        beforeEach(() => data = [['key1', 'value1'], ['key2', 'value2']]);

        it('Возвращает false, если параметра нет', () => {
            expect(isArgPositive(data, 'key3')).toBe(false);
        });

        it('Возвращает true, если параметр только один', () => {
            expect(isArgPositive(data, 'key2')).toBe(true);
        });

        it('Возвращает false, если есть отрицательное значение', () => {
            data.push(['key2', '0']);
            expect(isArgPositive(data, 'key2')).toBe(false);
        });

        it('Возвращает true, если есть несколько положительных значений', () => {
            data.push(['key2', '1']);
            expect(isArgPositive(data, 'key2')).toBe(true);
        });
    });

    describe('getRedirectUrl', () => {
        it('Возвращает url без доп. параметров', () => {
            expect(getRedirectUrl({ url: 'http://example.com' })).toEqual('http://example.com');
        });

        it('Добавляет no_turbo для turboOnly сервисов', () => {
            expect(getRedirectUrl({ url: 'http://example.com', turboOnly: true })).toEqual('http://example.com?no_turbo=1');
        });

        it('Добавляет no_turbo для turboOnly сервисов в url с query', () => {
            expect(getRedirectUrl({ url: 'http://example.com?foo=bar', turboOnly: true }))
                .toEqual('http://example.com?foo=bar&no_turbo=1');
        });
    });
});
