module.exports = {
    host: 'http://pers-static.tst.vs.market.yandex.net:34522/',
    route: /api\/opinion\/model\/[0-9]+/,
    response: {
        data: {
            opinions: [
                {
                    anonymous: 0,
                    averageGrade: 1,
                    comment: 'плахой тавар, бабка па падъезду не оценила',
                    contra: 'Очинь, очинь плоха выглядет',
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
                        main: -2,
                    },
                    id: 12345,
                    photos: [],
                    pro: 'хатя бы ни тяжелый',
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
                    usage: 1,
                    user: {
                        uid: 987,
                        entity: 'user',
                    },
                    votes: {
                        agree: 10,
                        reject: 100,
                        total: 110,
                    },
                },
                {
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
                },
                {
                    anonymous: 1,
                    averageGrade: 3,
                    comment: 'Ну такой средненький, под пивко пойдет',
                    contra: 'Ну какие-то есть',
                    cpa: true,
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
                        main: 0,
                    },
                    id: 12347,
                    photos: [],
                    pro: 'Ну что-то есть',
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
                    usage: 0,
                    user: {
                        uid: 985,
                        entity: 'user',
                    },
                    votes: {
                        agree: 30,
                        reject: 30,
                        total: 60,
                    },
                },
            ],
        },
    },
};
