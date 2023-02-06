import { verifiedShops } from './verifiedShops';

describe('verifiedShops', () => {
    // Это проверка захардкоженного списка магазинов
    // Они доверенные, но не попали в выгрузку https://st.yandex-team.ru/GOODS-2251
    it('должен содержать доверенные магазины', () => {
        [
            'ozon.ru',
            'www.ozon.ru',
            'sbermegamarket.ru',
            'mvideo.ru',
            'yandex.ru',
            'ozon.onelink.me',
            'eldorado.ru',
            'wildberries.ru',
            'pleer.ru',
            'mvideo.ru',
            'market.yandex.ru',
            'www.wildberries.ru',
        ].forEach(shop => expect(verifiedShops.has(shop)).toEqual(true));
    });
});
