import React from 'react';
import type { RequestInfo } from '@tenorok/storybook-addon-mock';
import withMock from '@tenorok/storybook-addon-mock';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Reviews } from '..';

const mockData: RequestInfo[] = [
    {
        url: '/ugcpub/digest?(.*)',
        method: 'GET',
        status: 200,
        delay: 500,
        response: request => {
            const {
                searchParams: params = {},
            } = request;

            if (params.objectId === '/site/fake') {
                return 'failed to build digest';
            }

            if (params.offset === '2') {
                return {
                    myReview: {
                        author: {
                            id: '/user/21323681',
                            name: 'Vasily P.',
                            pic: '15298/enc-e9df27cc5ed30aabeb47e528760a0aafb258976f2c3ad874aa76c9afbae08952',
                            login: 'TTenor',
                            pkPath: '/user/cyzv1da402kqe3ucwex6ryva2w',
                            professionLevelNum: 1,
                            verified: false,
                        },
                    },
                    view: {
                        views: [
                            {
                                id: 'site_1jg3r_zYmo8nlYhjR2l3vp9D7mrAPRlfS',
                                type: '/ugc/review',
                                time: 1611171481000,
                                author: {
                                    name: 'Vasily Pupkin',
                                    pic: '26057/DY6919wTAHwtivZlzbrmu0mvSQ-1',
                                    signPrivacy: 'NAME',
                                    pkPath: '/user/4pyrcyjha2dy64bfgy5g9zv9jr',
                                    publicId: '',
                                    verified: false,
                                },
                                text: 'Достоинства: Выгодная цена и быстрое перемещение товара\n\nНедостатки: Сложно найти магазин в ТЦ Проспект, т.к. нет навигации. Оплата наличными или перевод.\n\nКомментарий: Давно присматривал умную колонку, ждал выгодную цену т.к. периодически были скидки. Не ожидал что попаду на такую цену в данном магазине. Разница была в 1300руб. от ближайших конкурентов. На кассе также порадовали плиткой шоколадки и мелкой безделушкой. Колонку уже тестим, все отлично! Спасибо',
                                rating: {
                                    val: 5,
                                    max: 5,
                                },
                                object: {
                                    id: '/shops_market/enVybWFya2V0LnJ1',
                                    type: 'Site',
                                },
                                meta: {
                                    moderated: true,
                                    blocked: false,
                                },
                                reactions: {
                                    likesCount: 0,
                                    dislikesCount: 0,
                                    myReaction: 'NONE',
                                },
                                aspects: [
                                    {
                                        id: 1,
                                        name: 'комментарий:',
                                        fragment: [
                                            {
                                                keywordsPosition: 156,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                    {
                                        id: 1,
                                        name: 'достоинства:',
                                        fragment: [
                                            {
                                                keywordsPosition: 0,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                ],
                                isExternal: true,
                                source: 'Яндекс.Маркет',
                                sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                            },
                            {
                                id: 'site_29dkwJ3JlCEegiAT_twzbi7Zb4RZhoPwP',
                                type: '/ugc/review',
                                time: 1607186632000,
                                author: {
                                    name: 'Vasily Pupkin',
                                    pic: '43473/LGOfvvKuDrdGlsTssju5TaPhm2Y-1',
                                    signPrivacy: 'NAME',
                                    pkPath: '/user/vkp3mhmqtuxn02tvabvrukujb0',
                                    publicId: '',
                                    verified: false,
                                },
                                text: 'Достоинства: На все мои вопросы менеджер очень доходчиво и грамотно дал объяснения, даже помог разобраться в некоторых нюансах использования приобретённого мной смартфона Xiaomi Redmi Note 9 Pro 6/128gb.Одним словом-молодец.Не спросил у него имени , но это тот кто обслужил заказ № 81039. Вышел с новой покупкой очень довольный обслуживанием.\n\nНедостатки: Претензий по обслуживанию нет.',
                                rating: {
                                    val: 5,
                                    max: 5,
                                },
                                object: {
                                    id: '/shops_market/enVybWFya2V0LnJ1',
                                    type: 'Site',
                                },
                                meta: {
                                    moderated: true,
                                    blocked: false,
                                },
                                reactions: {
                                    likesCount: 0,
                                    dislikesCount: 0,
                                    myReaction: 'NONE',
                                },
                                aspects: [
                                    {
                                        id: 1,
                                        name: 'достоинства:',
                                        fragment: [
                                            {
                                                keywordsPosition: 0,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                ],
                                isExternal: true,
                                source: 'Яндекс.Маркет',
                                sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                            },
                            {
                                id: 'more_reviews',
                                type: '/ugc/button',
                                title: 'Ещё отзывы',
                            },
                        ],
                    },
                    csrfToken: 'Eh4/Z3UUPHnkGiKvtKQ2orADvIqGA8CCRiZH4Dkc0AcaDONm/zeSVfmcEQ1q6yIHCM306rKWMCoQ4FKsaCzN5F2S+Tg1lf9Npw==',
                    cmntApiKey: 'null',
                    pager: {
                        rating: {},
                        totalCount: 79,
                        realCount: 82,
                        rating10: {},
                        reviewCount: 79,
                        feedbackCount: 0,
                    },
                };
            }

            if (params.offset === '0' && params.ranking === 'by_time') {
                return {
                    myReview: {
                        author: {
                            id: '/user/21323681',
                            name: 'Vasily P.',
                            pic: '15298/enc-e9df27cc5ed30aabeb47e528760a0aafb258976f2c3ad874aa76c9afbae08952',
                            login: 'TTenor',
                            pkPath: '/user/cyzv1da402kqe3ucwex6ryva2w',
                            professionLevelNum: 1,
                            verified: false,
                        },
                    },
                    view: {
                        views: [
                            {
                                id: 'ratingOverall',
                                type: '/ugc/rating',
                                rating: {},
                            },
                            {
                                id: 'site_2L0EYVvmubD54687c0I1300tQxJnzNU',
                                type: '/ugc/review',
                                time: 1653728506000,
                                author: {
                                    name: 'Вася.Пупкин',
                                    pic: '',
                                    signPrivacy: 'NAME',
                                    pkPath: '/user/3zu58y7pgrtbc84jbbc8pm9a54',
                                    publicId: '',
                                    verified: false,
                                },
                                text: 'Достоинства: - Качественный товары\n- Компетентные менеджеры\n- Низкие цены\n- Большой ассортимент\n- Выполнение заказов в срок\n\nНедостатки: Нет\n\nКомментарий: Зурмаркет Казань - самый лучший магазин бытовой техники! Только здесь нашли нужную для нас встраиваемую вытяжку Elica европейской сборки. Лучшее качество по лучшей цене в лучшем магазине!!! Рекомендую!!!',
                                rating: {
                                    val: 5,
                                    max: 5,
                                },
                                object: {
                                    id: '/shops_market/enVybWFya2V0LnJ1',
                                    type: 'Site',
                                },
                                meta: {
                                    moderated: true,
                                    blocked: false,
                                },
                                reactions: {
                                    likesCount: 0,
                                    dislikesCount: 0,
                                    myReaction: 'NONE',
                                },
                                aspects: [
                                    {
                                        id: 1,
                                        name: 'достоинства:',
                                        fragment: [
                                            {
                                                keywordsPosition: 0,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                    {
                                        id: 1,
                                        name: 'комментарий:',
                                        fragment: [
                                            {
                                                keywordsPosition: 142,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                ],
                                isExternal: true,
                                source: 'Яндекс.Маркет',
                                sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                            },
                            {
                                id: 'site_269HZYoUUjym36ZQ-8dLZH3mxkbqmHYP',
                                type: '/ugc/review',
                                time: 1635331048000,
                                author: {
                                    name: 'Vasily P.',
                                    pic: '',
                                    signPrivacy: 'NAME',
                                    pkPath: '/user/27kbufxtfun0hkbeknwepjw7d4',
                                    publicId: '',
                                    verified: false,
                                },
                                text: 'Достоинства: Быстро и недорого\n\nНедостатки: Нет\n\nКомментарий: Привезли на следующий день и это хорошо.',
                                rating: {
                                    val: 5,
                                    max: 5,
                                },
                                object: {
                                    id: '/shops_market/enVybWFya2V0LnJ1',
                                    type: 'Site',
                                },
                                meta: {
                                    moderated: true,
                                    blocked: false,
                                },
                                reactions: {
                                    likesCount: 0,
                                    dislikesCount: 0,
                                    myReaction: 'NONE',
                                },
                                aspects: [
                                    {
                                        id: 1,
                                        name: 'комментарий:',
                                        fragment: [
                                            {
                                                keywordsPosition: 49,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                    {
                                        id: 1,
                                        name: 'достоинства:',
                                        fragment: [
                                            {
                                                keywordsPosition: 0,
                                                keywordsSize: 12,
                                                phraseSentiment: 'NEUTRAL',
                                                quotePosition: 0,
                                                quoteSize: 0,
                                                isSentenceStart: true,
                                                isSentenceEnd: true,
                                            },
                                        ],
                                        type: 1,
                                        rank: 0,
                                    },
                                ],
                                isExternal: true,
                                source: 'Яндекс.Маркет',
                                sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                            },
                            {
                                id: 'more_reviews',
                                type: '/ugc/button',
                                title: 'Ещё отзывы',
                            },
                        ],
                    },
                    csrfToken: 'Eh73jXCWDaauqQ+zdhLbfD/Xd38mrfYyyCkIDekZHFEaDOWepX817zW2msW73iIHCISD7rKWMCoQHITLjRgxsxyd263oNxHAFw==',
                    cmntApiKey: 'null',
                    pager: {
                        rating: {},
                        totalCount: 79,
                        realCount: 82,
                        rating10: {},
                        reviewCount: 79,
                        feedbackCount: 0,
                    },
                };
            }

            return {
                myReview: {
                    author: {
                        id: '/user/21323681',
                        name: 'Vasily P.',
                        pic: '15298/enc-e9df27cc5ed30aabeb47e528760a0aafb258976f2c3ad874aa76c9afbae08952',
                        login: 'TTenor',
                        pkPath: '/user/cyzv1da402kqe3ucwex6ryva2w',
                        professionLevelNum: 1,
                        verified: false,
                    },
                },
                view: {
                    views: [
                        {
                            id: 'ratingOverall',
                            type: '/ugc/rating',
                            rating: {},
                        },
                        {
                            id: 'site_-vM6TQWTVbzJ-wVzTB3MADi-1efJ0sr',
                            type: '/ugc/review',
                            time: 1415433240000,
                            author: {
                                name: 'Vasya Pupkin',
                                pic: '30955/enc-a5c3b9797c1ffe528237887814d81fee46ecda4956fe9787a1b2ace26ec1d24d',
                                signPrivacy: 'NAME',
                                pkPath: '/user/d.salyahutdinova',
                                publicId: '',
                                verified: false,
                            },
                            text: 'Достоинства: Низкие цены. Удобное расположение пункта выдачи. Грамотный персонал. Быстрое оформление и выдача заказа. СМС оповещение.\n\nНедостатки: нет оплаты банковской картой\n\nКомментарий: Искала недорогой телефон. Нашла Lenovo S820 по такой же стоимости что и в китайских магазинах (только тут за такую цену еще и гарантия год прилагается). Заказала в 9 вечера одного дня, в 10 утра следующего телефон был уже в моих руках.',
                            rating: {
                                val: 5,
                                max: 5,
                            },
                            object: {
                                id: '/shops_market/enVybWFya2V0LnJ1',
                                type: 'Site',
                            },
                            meta: {
                                moderated: true,
                                blocked: false,
                            },
                            reactions: {
                                likesCount: 0,
                                dislikesCount: 0,
                                myReaction: 'NONE',
                            },
                            aspects: [
                                {
                                    id: 1,
                                    name: 'комментарий:',
                                    fragment: [
                                        {
                                            keywordsPosition: 177,
                                            keywordsSize: 12,
                                            phraseSentiment: 'NEUTRAL',
                                            quotePosition: 0,
                                            quoteSize: 0,
                                            isSentenceStart: true,
                                            isSentenceEnd: true,
                                        },
                                    ],
                                    type: 1,
                                    rank: 0,
                                },
                                {
                                    id: 1,
                                    name: 'достоинства:',
                                    fragment: [
                                        {
                                            keywordsPosition: 0,
                                            keywordsSize: 12,
                                            phraseSentiment: 'NEUTRAL',
                                            quotePosition: 0,
                                            quoteSize: 0,
                                            isSentenceStart: true,
                                            isSentenceEnd: true,
                                        },
                                    ],
                                    type: 1,
                                    rank: 0,
                                },
                            ],
                            isExternal: true,
                            source: 'Яндекс.Маркет',
                            sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                        },
                        {
                            id: 'site_23jBQ8jlp2Jz0z44E_nZJup7dp_WvBy',
                            type: '/ugc/review',
                            time: 1590579423000,
                            author: {
                                name: 'Вася Пупкин',
                                pic: '51169/zaeBiEyReVjDTVvMkaTkGUCGWY-1',
                                signPrivacy: 'NAME',
                                pkPath: '/user/59z341n94p0rnmmb9pwfaxe8v4',
                                publicId: '',
                                verified: false,
                            },
                            text: 'Достоинства: Оперативность и цена\n\nНедостатки: Совсем не жизнерадостный и не очень вежливый курьер.\n\nКомментарий: Относительно большой ассортимент на сайте. Быстрая доставка. Бонусы дают, как другой гипермаркет электроники на букву "М" и ценник не самый высокий. Единственное, что "не очень" оплата только наличными',
                            rating: {
                                val: 4,
                                max: 5,
                            },
                            object: {
                                id: '/shops_market/enVybWFya2V0LnJ1',
                                type: 'Site',
                            },
                            meta: {
                                moderated: true,
                                blocked: false,
                            },
                            reactions: {
                                likesCount: 0,
                                dislikesCount: 0,
                                myReaction: 'NONE',
                            },
                            aspects: [
                                {
                                    id: 1,
                                    name: 'достоинства:',
                                    fragment: [
                                        {
                                            keywordsPosition: 0,
                                            keywordsSize: 12,
                                            phraseSentiment: 'NEUTRAL',
                                            quotePosition: 0,
                                            quoteSize: 0,
                                            isSentenceStart: true,
                                            isSentenceEnd: true,
                                        },
                                    ],
                                    type: 1,
                                    rank: 0,
                                },
                                {
                                    id: 1,
                                    name: 'комментарий:',
                                    fragment: [
                                        {
                                            keywordsPosition: 101,
                                            keywordsSize: 12,
                                            phraseSentiment: 'NEUTRAL',
                                            quotePosition: 0,
                                            quoteSize: 0,
                                            isSentenceStart: true,
                                            isSentenceEnd: true,
                                        },
                                    ],
                                    type: 1,
                                    rank: 0,
                                },
                            ],
                            isExternal: true,
                            source: 'Яндекс.Маркет',
                            sourceUrl: 'https://market.yandex.ru/shop/88724/reviews',
                        },
                        {
                            id: 'more_reviews',
                            type: '/ugc/button',
                            title: 'Ещё отзывы',
                        },
                    ],
                },
                csrfToken: 'Eh56Yy68fTCs8Z5K2sJrny+5jRhdrN9lWN8VPCoXKOoaDP1V68ln0J7n9oaOsCIHCLLMi5KWMCoQ5GhfmmvSDMdzsOiXOH9rqQ==',
                cmntApiKey: 'null',
                pager: {
                    rating: {},
                    totalCount: 79,
                    realCount: 79,
                    rating10: {},
                    reviewCount: 79,
                    feedbackCount: 0,
                },
            };
        },
    },
];

createPlatformStories('Tests/Reviews', Reviews, stories => {
    stories
        .addDecorator(withMock(mockData))
        .addDecorator(withStubReduxProvider())
        .add('plain', Reviews => {
            return (
                <div style={{ width: 300 }}>
                    <Reviews
                        objectId="/site/bHVzdHJvZi5ydQ=="
                        otype="Site"
                        limit={2}
                    />
                </div>
            );
        })
        .add('error', Reviews => {
            return (
                <div style={{ width: 300 }}>
                    <Reviews
                        objectId="/site/fake"
                        otype="Site"
                        limit={2}
                    />
                </div>
            );
        });
});
