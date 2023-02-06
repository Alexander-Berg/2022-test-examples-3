import { isMarketHost } from '../isInMarket';

describe('isInMarket', () => {
    describe('isMarketHost', () => {
        it('возвращает true, если хост === m.market.yandex.ru', async() => {
            expect(isMarketHost('m.market.yandex.ru')).toBeTruthy();
        });

        it('возвращает true, если хост === market.yandex.ru', async() => {
            expect(isMarketHost('market.yandex.ru')).toBeTruthy();
        });

        it('возвращает true, если хост === m.market.yandex.{tld}', async() => {
            expect(isMarketHost('m.market.yandex.ua')).toBeTruthy();
        });

        it('возвращает true, если хост === market.yandex.{tld}', async() => {
            expect(isMarketHost('market.yandex.com.tr')).toBeTruthy();
        });

        it('возвращает true на бетах маркета', () => {
            expect(isMarketHost('ololo.demofslb.market.yandex.ru')).toBeTruthy();
        });

        it('возвращает false, если хост === yandex.ru', async() => {
            expect(isMarketHost('yandex.ru')).toBeFalsy();
        });
    });
});
