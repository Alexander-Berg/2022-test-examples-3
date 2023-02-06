import uriHelper from '../../../components/helpers/uri';
import _ from 'lodash';

describe('uriHelper', () => {
    describe('Метод `makeRelative`', () => {
        it('должен вернуть относительный урл /preview/13.jpg по абсолютному http://yandex.ru/preview/13.jpg', () => {
            expect(uriHelper.makeRelative('http://yandex.ru/preview/13.jpg')).toBe('/preview/13.jpg');
        });

        it('должен вернуть относительный урл /preview/13.jpg по абсолютному https://yandex.ru/preview/13.jpg', () => {
            expect(uriHelper.makeRelative('https://yandex.ru/preview/13.jpg')).toBe('/preview/13.jpg');
        });

        it('должен вернуть относительный урл /preview/13.jpg по абсолютному //yandex.ru/preview/13.jpg', () => {
            expect(uriHelper.makeRelative('//yandex.ru/preview/13.jpg')).toBe('/preview/13.jpg');
        });
    });

    describe('Метод `getHost`', () => {
        it('должен вернуть foo.com по урлу http://foo.com/a', () => {
            expect(uriHelper.getHost('http://foo.com/a')).toBe('foo.com');
        });

        it('должен вернуть foo.com по урлу https://foo.com:8080/a', () => {
            expect(uriHelper.getHost('https://foo.com:8080/a')).toBe('foo.com');
        });

        it('должен вернуть null по невалидному урлу foo.com', () => {
            expect(uriHelper.getHost('foo.com')).toBe(null);
        });

        it('должен вернуть null по невалидному урлу foo', () => {
            expect(uriHelper.getHost('foo')).toBe(null);
        });
    });

    describe('Метод `cutProto`', () => {
        const tests = {
            'http://disk.yandex.ru/disk/folder/path/to/parent/file.jpg': '//disk.yandex.ru/disk/folder/path/to/parent/file.jpg',
            'https://disk.yandex.ru/folder/path/to/parent/file.jpg///': '//disk.yandex.ru/folder/path/to/parent/file.jpg///',
            'htt://disk.yandex.ru/disk/folder/path/to/parent////file.jpg': 'htt://disk.yandex.ru/disk/folder/path/to/parent////file.jpg'
        };

        _.each(tests, (output, input) => {
            it('должен вернуть урл без протокола ' + output + ' по полному урлу ' + input, () => {
                const result = uriHelper.cutProto(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('Метод `addParams`', () => {
        const tests = {
            'http://disk.yandex.ru/foo?foo=1&bar=1': { url: 'http://disk.yandex.ru/foo', params: { foo: 1, bar: 1 } },
            'http://disk.yandex.ru/foo?baz=1&foo=1': { url: 'http://disk.yandex.ru/foo?baz=1', params: { foo: 1 } },
            'http://disk.yandex.ru/foo?foo=1#hello': { url: 'http://disk.yandex.ru/foo#hello', params: { foo: 1 } }
        };

        _.each(tests, (input, url) => {
            it('должен построить урл ' + url, () => {
                const result = uriHelper.addParams(input.url, input.params);
                expect(result).toBe(url);
            });
        });
    });

    describe('#getTld', () => {
        [
            ['http://disk.yandex.ru/foo?foo=1&bar=1', 'ru'],
            ['http://disk.yandex.com/foo?foo=1&bar=1', 'com'],
            ['http://disk.yandex.com.tr/foo?foo=1&bar=1', 'tr'],
            ['http://disk.yandex.ua', 'ua'],
            ['http://disk.yandex.az', 'az'],
            ['http://disk.yandex.net', 'net'],
            ['http://disk.yandex.kz', 'kz'],
            ['http://disk.yandex.tr', 'tr'],
            ['http://disk.yandex.com.am', 'am'],
            ['http://disk.yandex.com.ge', 'ge'],
            ['http://disk.yandex.co.il', 'il'],
            ['http://disk.yandex.fr', 'fr'],
            ['http://disk.yandex.kg', 'kg'],
            ['http://disk.yandex.lt', 'lt'],
            ['http://disk.yandex.lv', 'lv'],
            ['http://disk.yandex.tj', 'tj'],
            ['http://disk.yandex.ee', 'ee'],
            ['http://disk.yandex.by', 'by']
        ].forEach((test) => {
            it(test[0] + ' -> ' + test[1], () => {
                expect(uriHelper.getTld(test[0])).toBe(test[1]);
            });
        });
    });

    describe('#replaceTld', () => {
        const tests = [
            ['ru', 'https://disk.yandex.ru'],
            ['com', 'https://disk.yandex.com'],
            ['com.tr', 'https://disk.yandex.com.tr'],
            ['ua', 'https://disk.yandex.ua'],
            ['az', 'https://disk.yandex.az'],
            ['com.am', 'https://disk.yandex.com.am'],
            ['com.ge', 'https://disk.yandex.com.ge'],
            ['co.il', 'https://disk.yandex.co.il'],
            ['kg', 'https://disk.yandex.kg'],
            ['lt', 'https://disk.yandex.lt'],
            ['lv', 'https://disk.yandex.lv'],
            ['md', 'https://disk.yandex.md'],
            ['tj', 'https://disk.yandex.tj'],
            ['tm', 'https://disk.yandex.tm'],
            ['uz', 'https://disk.yandex.uz'],
            ['fr', 'https://disk.yandex.fr'],
            ['ee', 'https://disk.yandex.ee'],
            ['kz', 'https://disk.yandex.kz'],
            ['by', 'https://disk.yandex.by']
        ];
        tests.forEach((first) => {
            describe(first[1], () => {
                tests.forEach((second) => {
                    it(first[1] + ' -> ' + second[1], () => {
                        expect(uriHelper.replaceTld(first[1], second[0])).toBe(second[1]);
                    });
                });
            });
        });
    });

    describe('метод `replaceTldByLang`', () => {
        describe('не должен ничего делать со ссылкой если tld "доступен"', () => {
            it('ru', () => {
                const link = 'https://yandex.ru/support';
                expect(uriHelper.replaceTldByLang(link, '', ['ru'])).toBe(link);
            });

            it('com', () => {
                const link = 'https://yandex.com/support';
                expect(uriHelper.replaceTldByLang(link, '', ['com'])).toBe(link);
            });

            it('fr', () => {
                const link = 'https://yandex.com.fr/support';
                expect(uriHelper.replaceTldByLang(link, '', ['fr'])).toBe(link);
            });

            it('com.tr', () => {
                const link = 'https://yandex.com.tr/support';
                expect(uriHelper.replaceTldByLang(link, '', ['tr'])).toBe(link);
            });
        });

        describe('не должен ничего делать со ссылкой если не передали список доступных tld', () => {
            it('ru', () => {
                const link = 'https://yandex.ru/support';
                expect(uriHelper.replaceTldByLang(link, '', [])).toBe(link);
            });

            it('com.tr', () => {
                const link = 'https://yandex.com.tr/support';
                expect(uriHelper.replaceTldByLang(link, '', [])).toBe(link);
            });
        });

        describe('меняем ссылку на com если язык tr или en и com-домен доступен', () => {
            it('язык en, домен fr', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.fr/support', 'en', ['com'])
                ).toBe('https://yandex.com/support');
            });

            it('язык tr, домен com.tr', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.com.tr/support', 'tr', ['com'])
                ).toBe('https://yandex.com/support');
            });
        });

        describe('меняем ссылку на ru', () => {
            it('язык en, домен com недоступен', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.com/support', 'en', ['ru'])
                ).toBe('https://yandex.ru/support');
            });

            it('язык tr, домены com и com.tr недоступны', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.com.tr/support', 'tr', ['ru'])
                ).toBe('https://yandex.ru/support');
            });

            it('язык uk, домен ua недоступен', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.ua/support', 'uk', ['ru'])
                ).toBe('https://yandex.ru/support');
            });

            it('язык ru, домен kz недоступен', () => {
                expect(
                    uriHelper.replaceTldByLang('https://yandex.kz/support', 'ru', ['ru'])
                ).toBe('https://yandex.ru/support');
            });
        });
    });

    describe('isDisk', () => {
        [
            'https://disk.yandex.ru',
            'https://disk.yandex.fr',
            'http://disk.yandex.com',
            'http://disk.yandex.co.il',
            'https://disk.dst.yandex.com',
            'http://disk.dst.yandex.com',
            'http://disk.dst.yandex.com.ge',
            'http://disk.dst.yandex.com.tr',
        ].forEach((url) => {
            it(`isDisk should return true for ${url}`, () => {
                expect(uriHelper.isDisk(url)).toBe(true);
            });
        });

        [
            'https://disks.yandex.ru',
            'https://disk.evil.yandex.fr',
            'https://disk.yandex.evil.bastard.com',
            'https://disk.yandex.co.uk',
            'https://disk.yandex.be',
            'https://disk.yandex.comege',
            'https://disk.yandex.zlo.com',
        ].forEach((url) => {
            it(`isDisk should return false for ${url}`, () => {
                expect(uriHelper.isDisk(url)).toBe(false);
            });
        });
    });

    describe('getSearch', () => {
        [
            ['https://yandex.ru?search=disk', '?search=disk'],
            ['https://disk.yandex.ru/client/remember/000000157245650809203090000001572357867550?from=gnc', '?from=gnc'],
        ].forEach(([url, search]) => {
            it(`getSearch should return ${search} from ${url}`, () => {
                expect(uriHelper.getSearch(url)).toEqual(search);
            });
        });
    });
});
