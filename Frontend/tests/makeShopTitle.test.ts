import { makeShopTitle } from '../index';

describe('makeShopTitle', () => {
    describe('домен магазина из knownShops', () => {
        it('должен игнорировать название магазина', () => {
            const url = 'https://ozon.ru';
            expect(makeShopTitle(url, 'iCases')).toEqual('OZON');
        });

        it('должен игнорировать пустое название магазина', () => {
            const url = 'https://ozon.ru';
            expect(makeShopTitle(url, '')).toEqual('OZON');
        });

        it('должен игнорировать отсутствие названия магазина', () => {
            const url = 'https://ozon.ru';
            expect(makeShopTitle(url)).toEqual('OZON');
        });
    });

    describe('домен магазина отсутствует в knownShops', () => {
        describe('shopName не указан', () => {
            it('должен вернуть url, если не смог извлечь домен', () => {
                const url = 'абвгд_qwe_123';
                expect(makeShopTitle(url)).toEqual('абвгд_qwe_123');
            });

            it('должен вернуть домен', () => {
                const url = 'https://spb.aliexpress.ru/search?q=iphone';
                expect(makeShopTitle(url)).toEqual('spb.aliexpress.ru');
            });
        });

        describe('shopName передан', () => {
            it('должен вернуть shopName независимо от url', () => {
                const url = 'https://ozonus.ru';
                expect(makeShopTitle(url, 'Озонус')).toEqual('Озонус');
            });

            it('должен вернуть shopName в punycode', () => {
                const url = 'https://msk.rdshop.ru/shop/samogonovarenie/ingredienty/skorlupa-kedrovogo-oreha-100-g';
                const result = makeShopTitle(url, 'xn--80aalwclyias7g0b.xn--p1ai');

                expect(result).toEqual('русскаядымка.рф');
            });

            it('должен вырезать www. в shopName, если оно совпадает с hostname', () => {
                const url = 'https://www.dns.ru';
                expect(makeShopTitle(url, 'www.dns.ru')).toEqual('dns.ru');
            });

            it('должен вырезать https:// в shopName', () => {
                const url = 'https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-x-aifon-10-60a36b340dfd6561532b8863';
                expect(makeShopTitle(url, 'https://youla.ru')).toEqual('youla.ru');
            });
        });
    });
});
