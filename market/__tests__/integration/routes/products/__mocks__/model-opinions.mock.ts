module.exports = {
    host: 'http://pers-static.tst.vs.market.yandex.net:34522/',
    route: /api\/opinion\/model\/[0-9]+/,
    response: {
        data: {
            opinions: Array(10).fill({
                anonymous: 0,
                averageGrade: 5,
                comment: 'Отличный товар, пользуемся всей семьей!',
                contra: 'Нету!1',
                cpa: false,
                created: 1559837933000,
                entity: 'opinion',
                factors: [
                    {
                        count: 1,
                        factorId: 1,
                        title: '',
                        value: '',
                    },
                ],
                fixId: 123,
                grade: {
                    1: null,
                    2: null,
                    3: null,
                    main: 2,
                },
                id: 12346,
                photos: [],
                pro: 'Абсолютли все!',
                product: {
                    entity: 'product',
                    id: 54321,
                },
                provider: {
                    name: null,
                    type: null,
                },
                recommend: false,
                region: {
                    id: 234,
                    entity: 'region',
                },
                type: 0,
                usage: 2,
                user: {
                    uid: 986,
                    entity: 'user',
                },
                votes: {
                    agree: 50,
                    reject: 1,
                    total: 51,
                },
            }),
        },
    },
};
