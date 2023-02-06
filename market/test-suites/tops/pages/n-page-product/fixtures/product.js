import {assign} from 'ambar';

const phone = {
    'id': '1831859610',
    'titles': {
        'raw': 'Смартфон Xiaomi Redmi 5 Plus 3/32GB',
        'highlighted': [
            {
                'value': 'Смартфон Xiaomi Redmi 5 Plus 3/32GB',
            },
        ],
    },
    'slug': 'smartfon-xiaomi-redmi-5-plus-3-32gb',
    'categories': [
        {
            'entity': 'category',
            'id': 91491,
            'slug': 'mobilnye-telefony',
            'name': 'Мобильные телефоны',
            'fullName': 'Мобильные телефоны',
            'type': 'guru',
            'isLeaf': true,
        },
    ],
    'navnodes': [
        {
            'entity': 'navnode',
            'id': 54726,
            'name': 'Мобильные телефоны',
            'slug': 'mobilnye-telefony',
            'fullName': 'Мобильные телефоны',
            'isLeaf': true,
            'rootNavnode': {},
        },
    ],
};

const newPhone = assign({}, phone, {
    isNew: true,
    isExclusive: true,
});

const absentPhone = {
    id: '160291',
    slug: 'telefon-nokia-3310',
    offers: {count: 0},
    categories: [
        {
            entity: 'category',
            id: 91491,
            slug: 'mobilnye-telefony',
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    navnodes: [
        {
            entity: 'navnode',
            id: 54726,
            name: 'Мобильные телефоны',
            slug: 'mobilnye-telefony',
            fullName: 'Мобильные телефоны',
            isLeaf: true,
            rootNavnode: {},
        },
    ],
};

// Тип "Групповая"
const notebook = {
    id: '12345',
    type: 'group',
    titles: {
        raw: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
        highlighted: [
            {
                value: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
            },
        ],
    },
    slug: 'noutbuk-lenovo-ideapad-310-15-amd',
    categories: [
        {
            entity: 'category',
            id: 91013,
            slug: 'slug',
            name: 'Ноутбуки',
            fullName: 'Ноутбуки',
            type: 'guru',
            isLeaf: true,
        },
    ],
    opinions: 0,
};

// Тип "Кластер"
const dress = {
    id: '12345',
    type: 'cluster',
    titles: {
        raw: 'Платье Selia',
        highlighted: [
            {
                value: 'Платье Selia',
            },
        ],
    },
    slug: 'plate-selia',
    categories: [
        {
            entity: 'category',
            id: 7811901,
            name: 'Платья',
            fullName: 'Женские платья',
            type: 'visual',
            isLeaf: true,
        },
    ],
};

// 'Тип "Книга"',
const book = {
    id: '12345',
    type: 'book',
    titles: {
        raw: 'Овидий "Искусство любви (подарочное издание)"',
        highlighted: [
            {
                value: 'Овидий "Искусство любви (подарочное издание)"',
            },
        ],
    },
    slug: 'ovidii-iskusstvo-liubvi-podarochnoe-izdanie',
    categories: [
        {
            entity: 'category',
            id: 90865,
            name: 'Античная литература',
            fullName: 'Античная литература',
            type: 'guru',
            isLeaf: true,
        },
    ],
};

export {
    phone,
    newPhone,
    absentPhone,
    notebook,
    dress,
    book,
};
