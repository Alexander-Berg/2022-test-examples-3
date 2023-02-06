// eslint-disable-next-line import/no-extraneous-dependencies
import { createBrowserHistory } from 'history';
import type { ISku } from '@src/typings/sku';
import { stubStore } from '@src/storybook/stubs/stubStore';
import { Platform } from '@src/typings/platform';
import { createPropsBySku } from '../utils/createProps';
import { ECardType } from '../ProductCard.typings';

describe('createProps', () => {
    describe('createPropsBySku', () => {
        let sku: ISku;

        beforeEach(() => {
            sku = {
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
                    count: 3,
                },
                category: {
                    name: 'Мягкие игрушки',
                    fullName: 'Мягкие игрушки',
                    kinds: [],
                },
                hasBestPrice: true,
            };
        });

        it('должен создавать пропсы', () => {
            const props = createPropsBySku(
                sku,
                { type: 'sku', id: 'id', showUid: 'showUid', reqid: 'reqid' },
                stubStore(),
                Platform.Desktop,
                createBrowserHistory().location,
            );

            expect(props).toEqual({
                id: '101343752894',
                type: 'sku',
                title: 'Мягкая игрушка «Груша», 28 см',
                rating: 0,
                price: { type: 'range', min: '641', max: '1062', currency: 'RUR' },
                priceLabel: 'Лучшая цена',
                offersCount: 3,
                images: [{
                    src: '//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/600x600',
                    srcHd: '//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/600x600',
                }],
                url: '/product/977344138/sku/101343752894?retpath=%2Fsearch',
                logNode: {
                    attrs: {
                        id: 'id',
                        showUid: 'showUid',
                        type: 'sku',
                        url: '/product/977344138/sku/101343752894',
                    },
                },
            });
        });

        it('не должен добавлять лейбл к цене, когда меньше трёх офферов', () => {
            sku.offers.count = 2;
            const props = createPropsBySku(
                sku,
                { type: 'sku', id: 'id', showUid: 'showUid', reqid: 'reqid' },
                stubStore(),
                Platform.Desktop,
                createBrowserHistory().location,
            );

            expect(props).toEqual({
                id: '101343752894',
                type: 'sku',
                title: 'Мягкая игрушка «Груша», 28 см',
                rating: 0,
                price: { type: 'range', min: '641', max: '1062', currency: 'RUR' },
                priceLabel: '',
                offersCount: 2,
                images: [{
                    src: '//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/600x600',
                    srcHd: '//avatars.mds.yandex.net/get-mpic/5221004/img_id9008790343602879802.jpeg/600x600',
                }],
                url: '/product/977344138/sku/101343752894?retpath=%2Fsearch',
                logNode: {
                    attrs: {
                        id: 'id',
                        showUid: 'showUid',
                        type: 'sku',
                        url: '/product/977344138/sku/101343752894',
                    },
                },
            });
        });
    });
});
