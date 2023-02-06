'use strict';

const {
    formatBanner,
    getAvailableBanners,
} = require('../../src/middlewares/banners');

describe('Banners utility functions', () => {
    describe('getAvailableBanners', () => {
        const testCases = [
            {
                name: 'should find banner with all filters',
                input: {
                    testBanner: {
                        title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'test-banner',
                        startDate: '2019-06-25',
                        priority: -1,
                        enabled: true,
                    },
                },
                expected: {
                    testBanner: {
                        title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'test-banner',
                        startDate: new Date('2019-06-25T00:00:00.000Z'),
                        priority: -1,
                        enabled: true,
                    },
                },
            },
            {
                name: 'should find enabled banner and filter disabled',
                input: {
                    testBanner: {
                        title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'test-banner',
                        startDate: '2019-06-25',
                        priority: -1,
                        enabled: false,
                    },
                    love: {
                        title: 'Любооовь',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'love-banner',
                        startDate: '2019-06-25',
                        priority: 100,
                        enabled: true,
                    },
                },
                expected: {
                    love: {
                        title: 'Любооовь',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'love-banner',
                        startDate: new Date('2019-06-25T00:00:00.000Z'),
                        priority: 100,
                        enabled: true,
                    },
                },
            },
            {
                name: 'should filter all outdated banners',
                input: {
                    testBanner: {
                        title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'test-banner',
                        startDate: '2019-06-25',
                        endDate: '2019-07-25',
                        priority: -1,
                        enabled: false,
                    },
                    love: {
                        title: 'Любооовь',
                        url: '{url}',
                        image: '/banners/test.png',
                        target: 'love-banner',
                        endDate: '2017-07-25',
                        priority: 100,
                        enabled: true,
                    },
                },
                expected: {},
            },
        ];

        testCases.forEach(({ name, input, expected }) => {
            test(name, () => {
                expect(getAvailableBanners(input)).toEqual(expected);
            });
        });
    });

    describe('formatBanner', () => {
        const testCases = [
            {
                name: 'should format banner properly',
                input: {
                    title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                    url: '{url}',
                    image: '/banners/test.png',
                    target: 'test-banner',
                    startDate: '2019-06-25',
                    priority: -1,
                    enabled: true,
                },
                expected: expect.objectContaining({
                    title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                    url: 'https://sovetnik.market.yandex.ru/redir?url=%7Burl%7D&v=&target=test-banner&click_type=unknown',
                    image: expect.any(String),
                }),
            },
            {
                name: 'should format banner with default target',
                input: {
                    title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                    url: '{url}',
                    image: '/banners/test.png',
                    startDate: '2019-06-25',
                    priority: -1,
                    enabled: true,
                },
                expected: expect.objectContaining({
                    title: 'Что подарить на 14 февраля? 10 небанальных идей для пар',
                    url: 'https://sovetnik.market.yandex.ru/redir?url=%7Burl%7D&v=&target=promo&click_type=market',
                    image: expect.any(String),
                }),
            },
        ];

        testCases.forEach(({ name, input, expected }) => {
            test(name, () => {
                expect(formatBanner(input, {})).toEqual(expected);
            });
        });
    });
});
