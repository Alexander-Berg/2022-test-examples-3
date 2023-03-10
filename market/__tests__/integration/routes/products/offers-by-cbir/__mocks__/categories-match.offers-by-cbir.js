const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/categories\/match/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: 225,
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        page: {
            number: 1,
            count: 100,
        },
        id: '1571860682842/a4185728bb1b78eb2b20a35599950500',
        time: '2019-10-23T22:58:02.857+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    categories: [
        {
            id: 7812201,
            name: 'Сумки',
            fullName: 'Сумки',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: 4.805163167430132,
        },
        {
            id: 7811938,
            name: 'Шарфы и платки',
            fullName: 'Женские шарфы и платки',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: 0.8287019306608363,
        },
        {
            id: 7920819,
            name: 'Портфели',
            fullName: 'Портфели',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: 0.24964164644766396,
        },
        {
            id: 7814990,
            name: 'Босоножки',
            fullName: 'Женские босоножки',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.0444818273872796,
        },
        {
            id: 7814991,
            name: 'Ботинки',
            fullName: 'Женские ботинки',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.1271130535472196,
        },
        {
            id: 7811903,
            name: 'Брюки',
            fullName: 'Женские брюки',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.1605593927446,
        },
        {
            id: 7812175,
            name: 'Ремни и пояса',
            fullName: 'Ремни и пояса',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.270783716507959,
        },
        {
            id: 7812200,
            name: 'Кошельки',
            fullName: 'Кошельки',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.438528351336248,
        },
        {
            id: 16155466,
            name: 'Вино',
            fullName: 'Вино',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.4883700198915564,
        },
        {
            id: 7811940,
            name: 'Перчатки и варежки',
            fullName: 'Женские перчатки и варежки',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.5038939518612424,
        },
        {
            id: 7920674,
            name: 'Ботильоны',
            fullName: 'Женские ботильоны',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.50996170150078,
        },
        {
            id: 7814997,
            name: 'Полусапоги',
            fullName: 'Женские полусапоги',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.5747778061868307,
        },
        {
            id: 91076,
            name: 'Сумки и рюкзаки',
            fullName: 'Сумки и рюкзаки для ноутбуков',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.6560857466107355,
        },
        {
            id: 7811901,
            name: 'Платья',
            fullName: 'Женские платья',
            type: 'VISUAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.702266433733058,
        },
        {
            id: 2662954,
            name: 'Чехлы для планшетов',
            fullName: 'Чехлы для планшетов',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.728451295294836,
        },
        {
            id: 91498,
            name: 'Чехлы',
            fullName: 'Чехлы для мобильных телефонов',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.759293996222256,
        },
        {
            id: 15927546,
            name: 'Парфюмерия',
            fullName: 'Парфюмерия',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.8422781243297373,
        },
        {
            id: 90616,
            name: 'Сумки, чехлы для фото- и видеотехники',
            fullName: 'Сумки, кейсы, чехлы для фото- и видеотехники',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -1.8462850210568602,
        },
        {
            id: 16226051,
            name: 'Пульты',
            fullName: 'Пульты для шлагбаумов и ворот',
            type: 'GENERAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.0259278909389535,
        },
        {
            id: 6203657,
            name: 'Вибромассажеры',
            fullName: 'Вибромассажеры',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.1239905851956924,
        },
        {
            id: 90588,
            name: 'Мясорубки',
            fullName: 'Мясорубки электрические',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.1531991819512597,
        },
        {
            id: 14255967,
            name: 'Витамины и минералы',
            fullName: 'Витамины и минералы для спортсменов',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.1630143734074228,
        },
        {
            id: 14910546,
            name: 'Ножницы и гильотины',
            fullName: 'Ножницы и гильотины',
            type: 'GENERAL',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.170475838112534,
        },
        {
            id: 15450276,
            name: 'Видеокамеры',
            fullName: 'Видеокамеры',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.1825506149298675,
        },
        {
            id: 6203656,
            name: 'Массажные кресла',
            fullName: 'Массажные кресла',
            type: 'GURU',
            childCount: 0,
            advertisingModel: 'CPC',
            rank: -2.185109293905966,
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
