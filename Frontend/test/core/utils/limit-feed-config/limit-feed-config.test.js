const { getFeedCfgConstructor } = require('../../../../core/utils/limit-feed-config/limit-feed-config');

describe('Ограничение ленты', () => {
    describe('Дзен', () => {
        const data = {
            reqdata: {
                headers: {
                    'zen-features': '{}',
                },
            },
        };

        const expFlags = {};

        const makeFeedCfg = getFeedCfgConstructor(data, expFlags);

        it('Возвращает корректные параметры для полностраничной ленты', () => {
            expect(makeFeedCfg('full')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: 1,
                maxCount: 1,
                shouldLimit: true,
            });
        });

        it('Возвращает корректные параметры для полностраничной ленты, если есть флаг', () => {
            expect(getFeedCfgConstructor(data, {
                'limit-feed': JSON.stringify({
                    from: 'zen',
                    targets: [
                        ['full', 2],
                        ['cards', 3],
                    ],
                }),
            })('full')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: 1,
                maxCount: 2,
                shouldLimit: true,
            });
        });

        it('Возвращает корректные параметры для карточной ленты', () => {
            expect(makeFeedCfg('cards')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: 1,
                maxCount: 5,
                shouldLimit: true,
            });
        });

        it('Возвращает корректные параметры для карточной ленты, если есть флаг', () => {
            expect(getFeedCfgConstructor(data, {
                'limit-feed': JSON.stringify({
                    from: 'zen',
                    targets: [
                        ['full', 2],
                        ['cards', 3],
                    ],
                }),
            })('cards')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: 1,
                maxCount: 3,
                shouldLimit: true,
            });
        });

        it('Возвращает null, если запрос не из Дзена', () => {
            expect(getFeedCfgConstructor({
                reqdata: {
                    headers: {},
                },
            }, expFlags)).toBeNull();
        });
    });

    describe('Новости', () => {
        const data = {
            cgidata: {
                args: {
                    utm_source: ['yxnews'],
                },
            },
            reqdata: {
                headers: {},
            },
        };

        const expFlags = {
            'limit-feed': JSON.stringify({
                from: 'news',
                targets: [
                    ['full', 2],
                    ['cards', 3],
                ],
            }),
        };

        const makeFeedCfg = getFeedCfgConstructor(data, expFlags);

        it('Возвращает корректные параметры для полностраничной ленты', () => {
            expect(makeFeedCfg('full')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: undefined,
                maxCount: 2,
                shouldLimit: true,
            });
        });

        it('Возвращает корректные параметры для карточной ленты', () => {
            expect(makeFeedCfg('cards')).toStrictEqual({
                isDisabled: false,
                loadMoreCount: undefined,
                maxCount: 3,
                shouldLimit: true,
            });
        });

        it('Возвращает null, если запрос не из Новостей', () => {
            expect(getFeedCfgConstructor({
                reqdata: {
                    headers: {},
                },
            }, expFlags)).toBeNull();
        });
    });
});
