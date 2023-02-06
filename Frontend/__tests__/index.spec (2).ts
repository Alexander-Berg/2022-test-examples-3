import { IProduct, ISku, IOffer } from '@yandex-turbo/applications/beru.ru/interfaces';
import { getOffer, getAddToCartLink } from '..';

function getMockEntites() {
    const offers = {
        items: [{ entity: 'offer' }],
    };
    const product = <IProduct>{
        offers: { ...offers },
    };
    const sku = <ISku> {
        offers: { ...offers },
    };

    return {
        offers,
        product,
        sku,
    };
}

describe('Общие ф-ии для работы с сущностями', () => {
    describe('ф-ия getOffer', () => {
        it('должна возвращать офер из продукта или cку', () => {
            const entities = getMockEntites();

            expect(getOffer(entities.product)).toEqual(entities.offers.items[0]);
            expect(getOffer(entities.sku)).toEqual(entities.offers.items[0]);
        });

        it('должна возвращать undefined, если офера нет в сущности', () => {
            const entities = getMockEntites();

            delete entities.product.offers.items;
            delete entities.sku.offers.items;

            expect(getOffer(entities.product)).toBeUndefined();
            expect(getOffer(entities.sku)).toBeUndefined();
        });
    });

    describe('ф-ия getAddToCartLink', () => {
        it('в окружении отличном от production должна возвращать не encrypted ссылку', () => {
            process.env.NODE_ENV = 'testing';

            const url = getAddToCartLink(<IOffer>{
                urls: {
                    directTurboBundle: '//beru.ru/test/1',
                },
            });

            expect(url).toEqual('//beru.ru/test/1');
        });

        it('в production окружении возвращает encrypted ссылку c хостом market-click2', () => {
            process.env.NODE_ENV = 'production';

            const url = getAddToCartLink(<IOffer>{
                urls: {
                    encryptedTurboBundle: '/test/1',
                },
            });

            expect(url).toEqual('https://market-click2.yandex.ru/test/1');
        });

        it('в production окружении возвращает undefined если CPA ссылки нет', () => {
            process.env.NODE_ENV = 'production';

            const url = getAddToCartLink(<IOffer>{
                urls: {},
            });

            expect(url).toBeUndefined();
        });
    });
});
