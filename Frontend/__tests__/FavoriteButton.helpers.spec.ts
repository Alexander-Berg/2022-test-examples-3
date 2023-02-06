import { prepareUrl } from '../FavoriteButton.helpers';

describe('FavoriteButton.helpers', () => {
    describe('prepareUrl', () => {
        it('Возвращает корректный урл, если исходный начинается с 2 слэшей', () => {
            expect(prepareUrl('//avatars.mds.yandex.net/get-turbo/2399246'))
                .toStrictEqual('https://avatars.mds.yandex.net/get-turbo/2399246');
        });

        it('Возвращает корректный урл, если исходный урл с http', () => {
            expect(prepareUrl('http://avatars.mds.yandex.net/get-turbo/2399246'))
                .toStrictEqual('https://avatars.mds.yandex.net/get-turbo/2399246');
        });

        it('Возвращает корректный урл, если исходный урл с https', () => {
            expect(prepareUrl('https://avatars.mds.yandex.net/get-turbo/2399246'))
                .toStrictEqual('https://avatars.mds.yandex.net/get-turbo/2399246');
        });

        it('Возвращает корректный урл, если исходный без протокола', () => {
            expect(prepareUrl('avatars.mds.yandex.net/get-turbo/2399246'))
                .toStrictEqual('https://avatars.mds.yandex.net/get-turbo/2399246');
        });
    });
});
