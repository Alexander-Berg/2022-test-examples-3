import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const intents = [
    {
        defaultOrder: 5,
        ownCount: 62,
        relevance: -1.28305,
        category: {
            name: 'Сумки, чехлы для фото- и видеотехники',
            slug: 'sumki-chekhly-dlia-foto-i-videotekhniki',
            uniqName: 'Сумки, кейсы, чехлы для фото- и видеотехники',
            hid: 90616,
            nid: 56147,
            isLeaf: true,
            view: 'list',
        },
    },
    {
        defaultOrder: 1,
        ownCount: 52,
        relevance: -0.515724,
        category: {
            name: 'Чехлы',
            slug: 'chekhly',
            uniqName: 'Чехлы для мобильных телефонов',
            hid: 91498,
            nid: 56036,
            isLeaf: true,
            view: 'list',
        },
    },
    {
        defaultOrder: 12,
        ownCount: 0,
        relevance: -1.84236,
        category: {
            name: 'Аксессуары',
            slug: 'aksessuary',
            uniqName: 'Аксессуары для фототехники',
            hid: 90611,
            nid: 56189,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 15,
        ownCount: 0,
        relevance: -2.10363,
        category: {
            name: 'Фото и видеокамеры',
            slug: 'foto-i-videokamery',
            uniqName: 'Фото и видеокамеры',
            hid: 90607,
            nid: 54778,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 3,
        ownCount: 0,
        relevance: -1.05505,
        category: {
            name: 'Электроника',
            slug: 'elektronika',
            uniqName: 'Электроника',
            hid: 198119,
            nid: 54440,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 10,
        ownCount: 0,
        relevance: -1.81772,
        category: {
            name: 'Компьютерная техника',
            slug: 'kompiuternaia-tekhnika',
            uniqName: 'Компьютерная техника',
            hid: 91009,
            nid: 54425,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 16,
        ownCount: 0,
        relevance: -2.30628,
        category: {
            name: 'Товары для дома',
            slug: 'tovary-dlia-doma',
            uniqName: 'Товары для дома',
            hid: 90666,
            nid: 54422,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 13,
        ownCount: 0,
        relevance: -1.99062,
        category: {
            name: 'Авто',
            slug: 'avto',
            uniqName: 'Товары для авто- и мототехники',
            hid: 90402,
            nid: 54418,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 14,
        ownCount: 0,
        relevance: -1.99613,
        category: {
            name: 'Аксессуары и оборудование',
            slug: 'aksessuary-i-oborudovanie',
            uniqName: 'Аксессуары и оборудование для автомобиля',
            hid: 90461,
            nid: 54454,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 11,
        ownCount: 0,
        relevance: -1.83398,
        category: {
            name: 'Обустройство салона',
            slug: 'obustroistvo-salona',
            uniqName: 'Аксессуары для обустройства салона автомобиля',
            hid: 12327158,
            nid: 60882,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 0,
        ownCount: 4,
        relevance: -0.39552,
        category: {
            name: 'Чехлы и накидки на сиденья',
            slug: 'chekhly-i-nakidki-na-sidenia',
            uniqName: 'Чехлы и накидки на сиденья',
            hid: 90465,
            nid: 54832,
            isLeaf: true,
            view: 'list',
        },
    },
    {
        defaultOrder: 18,
        ownCount: 0,
        relevance: -2.36493,
        category: {
            name: 'Текстиль',
            slug: 'tekstil',
            uniqName: 'Текстиль для дома',
            hid: 90667,
            nid: 54504,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 4,
        ownCount: 6,
        relevance: -1.14936,
        category: {
            name: 'Чехлы для мебели',
            slug: 'chekhly-dlia-mebeli',
            uniqName: 'Чехлы для мебели',
            hid: 11911278,
            nid: 60759,
            isLeaf: true,
            view: 'grid',
        },
    },
    {
        defaultOrder: 19,
        ownCount: 0,
        relevance: -2.56465,
        category: {
            name: 'Хозяйственные товары',
            slug: 'khoziaistvennye-tovary',
            uniqName: 'Хозяйственные товары',
            hid: 10607801,
            nid: 58621,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 20,
        ownCount: 0,
        relevance: -2.7144,
        category: {
            name: 'Хранение вещей',
            slug: 'khranenie-veshchei',
            uniqName: 'Хранение вещей',
            hid: 12805274,
            nid: 62776,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 9,
        ownCount: 1,
        relevance: -1.56838,
        category: {
            name: 'Чехлы для одежды',
            slug: 'chekhly-dlia-odezhdy',
            uniqName: 'Чехлы для хранения одежды',
            hid: 12807782,
            nid: 62790,
            isLeaf: true,
            view: 'grid',
        },
    },
    {
        defaultOrder: 6,
        ownCount: 0,
        relevance: -1.45871,
        category: {
            name: 'Аксессуары',
            slug: 'aksessuary',
            uniqName: 'Аксессуары для компьютерной техники',
            hid: 91070,
            nid: 54530,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 2,
        ownCount: 31,
        relevance: -0.949332,
        category: {
            name: 'Чехлы для планшетов',
            slug: 'chekhly-dlia-planshetov',
            uniqName: 'Чехлы для планшетов',
            hid: 2662954,
            nid: 55299,
            isLeaf: true,
            view: 'grid',
        },
    },
    {
        defaultOrder: 17,
        ownCount: 0,
        relevance: -2.35762,
        category: {
            name: 'Аксессуары и запчасти для ноутбуков',
            slug: 'aksessuary-i-zapchasti-dlia-noutbukov',
            uniqName: 'Аксессуары и запчасти для ноутбуков',
            hid: 12324140,
            nid: 60864,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 21,
        ownCount: 1,
        relevance: -2.77177,
        category: {
            name: 'Сумки и рюкзаки',
            slug: 'sumki-i-riukzaki',
            uniqName: 'Сумки и рюкзаки для ноутбуков',
            hid: 91076,
            nid: 55300,
            isLeaf: true,
            view: 'list',
        },
    },
    {
        defaultOrder: 8,
        ownCount: 0,
        relevance: -1.52965,
        category: {
            name: 'Телефоны',
            slug: 'telefony',
            uniqName: 'Телефоны и аксессуары к ним',
            hid: 91461,
            nid: 54437,
            isLeaf: false,
            view: 'list',
        },
    },
    {
        defaultOrder: 7,
        ownCount: 0,
        relevance: -1.50253,
        category: {
            name: 'Аксессуары для телефонов',
            slug: 'aksessuary-dlia-telefonov',
            uniqName: 'Аксессуары для мобильных телефонов',
            hid: 91497,
            nid: 54719,
            isLeaf: false,
            view: 'list',
        },
    },
];

const productSlug = 'smartfon-samsung-galaxy-s8';
export const titles = {
    raw: 'Смартфон Samsung Galaxy S8',
    highlighted: [
        {
            value: 'Смартфон',
            highlight: true,
        },
        {
            value: ' ',
        },
        {
            value: 'Samsung',
            highlight: true,
        },
        {
            value: ' Galaxy S8',
        },
    ],
};

const categories = [
    {
        cpaType: 'cpc_and_cpa',
        entity: 'category',
        fullName: 'Чехлы для мобильных телефонов',
        id: 91498,
        isLeaf: true,
        kinds: [],
        name: 'Чехлы',
        nid: 56036,
        slug: 'chekhly',
        type: 'guru',
    },
];

const products = new Array(10).fill(0).map(() =>
    createProduct({
        slug: productSlug,
        titles,
        categories,
    })
);

const state = mergeState([
    ...products,
    {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
                total: 2,
            },
            intents,
        },
    },
]);

export default {
    state,
};
