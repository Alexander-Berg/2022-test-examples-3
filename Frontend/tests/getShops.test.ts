import {
    BEST_PRICE_LABEL,
    BEST_PRICE_DISCLAIMER,
} from '@src/constants/sku';
import { ECardType } from '@src/components/ProductCard/ProductCard.typings';
import type { IGetShopsOptions } from '../utils/getShops';
import { getShops } from '../utils/getShops';

describe('getShops', () => {
    let options: IGetShopsOptions;
    let res: ReturnType<typeof getShops>;

    beforeEach(() => {
        options = {
            sku: {
                type: ECardType.sku,
                id: '101343752894',
                pictures: ['//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/orig'],
                description: 'Мягкая игрушка — презент для ребёнка и взрослого.',
                formattedDescription: {
                    shortHtml: 'Мягкая игрушка — презент для ребёнка и взрослого.',
                    fullHtml: 'Мягкая игрушка — презент для ребёнка и взрослого.',
                },
                specs: [
                    'тип: мягкая игрушка',
                ],
                title: 'Мягкая игрушка «Груша», 28 см',
                productId: '977344138',
                parameters: [],
                price: {
                    type: 'range',
                    min: '641',
                    max: '1062',
                    currency: 'RUR',
                },
                offers: {
                    count: 4,
                },
                category: {
                    name: 'Мягкие игрушки',
                    fullName: 'Мягкие игрушки',
                    kinds: [],
                },
            },
            offers: [
                {
                    showUid: '16437130827494690960700001',
                    reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                    offer: {
                        type: ECardType.offer,
                        id: '10hbKOuc7QQKatyN2rjLEg',
                        title: 'Мягкая игрушка «Груша», 28 см',
                        pictures: ['//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/orig'],
                        description: 'Мягкая игрушка — презент для ребёнка и взрослого.',
                        price: {
                            type: 'exact',
                            currency: 'RUR',
                            current: '641',
                            old: '1068',
                        },
                        shop: {
                            title: 'Яндекс.Маркет',
                            id: '431782',
                            hostname: 'pokupki.market.yandex.ru',
                        },
                        urls: {
                            direct: 'https://pokupki.market.yandex.ru/product/101343752894',
                            encrypted: 'https://market-click2.yandex.ru/redir/',
                        },
                        isMarket: true,
                        parameters: [
                            [
                                'Высота',
                                '28см',
                            ],
                        ],
                        category: {
                            name: 'Мягкие игрушки',
                            fullName: 'Мягкие игрушки',
                            kinds: [],
                        },
                        usedGoods: false,
                    },
                },
                {
                    showUid: '16437130827494690960700002',
                    reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                    offer: {
                        type: ECardType.offer,
                        id: 'L9zjRmpNqFHIRznWuo2MSA',
                        title: 'Мягкая игрушка "Груша", 28 см',
                        pictures: ['//avatars.mds.yandex.net/get-marketpic/1710220/market_ClmEwMXXFsCcMB75tEj1FQ/orig'],
                        description: 'Артикул: 1379-151. Размеры: 10 x 18 x 28 см., Вес: 154 гр., Размер упаковки: 18 x 15 x 28 см., Вид: Еда, Высота, см: 28',
                        price: {
                            type: 'exact',
                            currency: 'RUR',
                            current: '662',
                        },
                        shop: {
                            title: 'Дом Подарка',
                            id: '37040',
                            hostname: 'dom-podarka.ru',
                        },
                        urls: {
                            direct: 'https://dom-podarka.ru/catalog/detskie-tovari/igrushki/myagkie-igrushki/myagkie-igrushki-po-razmeram/ot-20-do-50-sm/1379-151-myagkaya-igrushka-grusha-28-sm/',
                            encrypted: 'https://market-click2.yandex.ru/redir/',
                        },
                        isMarket: false,
                        parameters: [
                            [
                                'Высота',
                                '28см',
                            ],
                        ],
                        category: {
                            name: 'Мягкие игрушки',
                            fullName: 'Мягкие игрушки',
                            kinds: [],
                        },
                        usedGoods: false,
                    },
                },
                {
                    showUid: '16437130827494690960700003',
                    reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                    offer: {
                        type: ECardType.offer,
                        id: 'UhhHUjWGinlM1IKvVk6UDQ',
                        title: 'Мягкая игрушка «Груша», 28 см',
                        pictures: ['//avatars.mds.yandex.net/get-marketpic/1711025/picdcf757f92c3949428452fad262c4fe12/orig'],
                        description: 'Мягкая игрушка — презент для ребёнка и взрослого.',
                        price: {
                            type: 'exact',
                            currency: 'RUR',
                            current: '1062',
                        },
                        shop: {
                            title: 'RusExpress',
                            id: '971099',
                            hostname: 'rusexpress.ru',
                        },
                        urls: {
                            direct: 'https://rusexpress.ru/products/myagkaya-igrushka-grusha-28-cm',
                            encrypted: 'https://market-click2.yandex.ru/redir/',
                        },
                        isMarket: false,
                        parameters: [
                            [
                                'Высота',
                                '28см',
                            ],
                        ],
                        category: {
                            name: 'Мягкие игрушки',
                            fullName: 'Мягкие игрушки',
                            kinds: [],
                        },
                        usedGoods: false,
                    },
                },
            ],
        };

        res = [
            {
                id: '10hbKOuc7QQKatyN2rjLEg',
                reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                showUid: '16437130827494690960700001',
                priceLabel: '',
                priceLabelDisclaimer: '',
            },
            {
                id: 'L9zjRmpNqFHIRznWuo2MSA',
                reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                showUid: '16437130827494690960700002',
                priceLabel: '',
                priceLabelDisclaimer: '',
            },
            {
                id: 'UhhHUjWGinlM1IKvVk6UDQ',
                reqid: '1643713082615751-3218566028314846509-gfvquh5jy6kgryxj-BAL-573',
                showUid: '16437130827494690960700003',
                priceLabel: '',
                priceLabelDisclaimer: '',
            },
        ];
    });

    it('должен формировать базовый список магазинов', () => {
        const shops = getShops(options);
        expect(shops).toEqual(res);
    });

    describe('Лейбл хорошей цены', () => {
        beforeEach(() => {
            // Имитация данных от бекенда.
            options.sku.hasBestPrice = true;
        });

        it('должен добавлять лейбл хорошей цены только первому офферу', () => {
            res[0].priceLabel = BEST_PRICE_LABEL;
            res[0].priceLabelDisclaimer = BEST_PRICE_DISCLAIMER;

            const shops = getShops(options);
            expect(shops).toEqual(res);
        });

        it('должен добавлять лейбл хорошей цены первым двум офферам', () => {
            // Ставим второму офферу такую же цену, как у первого
            // во входящих данных и в ожидаемом результате.
            options.offers[1].offer.price.current = options.offers[0].offer.price.current;
            // Ожидаем лейбл на первых двух офферах.
            res[0].priceLabel = res[1].priceLabel = BEST_PRICE_LABEL;
            // Ожидаем дисклеймер только у первого оффера.
            res[0].priceLabelDisclaimer = BEST_PRICE_DISCLAIMER;

            const shops = getShops(options);
            expect(shops).toEqual(res);
        });

        it('не должен добавлять лейбл хорошей цены, когда меньше трёх офферов', () => {
            // Есть минимальные условия показа лейбла хорошей цены и на фронтенде на всякий случай нужно проверять,
            // что офферов как минимум три и только в таком случае показывать лейбл.
            // Это нужно, поскольку в теории на бекенде может произойти рассинхрон
            // проставления метки наличия хорошей цены в SKU и списка его офферов.

            // Оставляем только первые два оффера.
            options.offers.splice(2);
            res.splice(2);

            const shops = getShops(options);
            expect(shops).toEqual(res);
        });
    });
});
