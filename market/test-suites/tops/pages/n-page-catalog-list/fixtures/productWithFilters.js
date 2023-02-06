import {
    mergeState,
    createProduct,
    createFilter,
    createFilterValue,
    createEntityPicture,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const filterMock = {
    'id': '13887626',
    'type': 'enum',
    'name': 'Цвет',
    'xslname': 'color_glob',
    'subType': 'color',
    'kind': 2,
    'position': 2,
    'noffers': 351360,
    'valuesCount': 16,
    'values': [
        {
            'initialFound': 141936,
            'group': 'черный',
            'found': 141936,
            'value': 'черный',
            'code': '#000000',
            'priceMin': {
                'currency': 'RUR',
                'value': '7',
            },
            'id': '13899071',
            'checked': true,
        },
    ],
    'valuesGroups': [
        {
            'type': 'top',
            'valuesIds': [
                '13899071',
            ],
        },
    ],
};

const productMock = {
    'showUid': '15561779571200391866316001',
    'entity': 'product',
    'vendor': {
        'entity': 'vendor',
        'id': 153043,
        'name': 'Apple',
        'slug': 'apple',
        'website': 'http://www.apple.com/ru',
        'logo': {
            'entity': 'picture',
            'url': '//avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
            'thumbnails': [],
        },
        'filter': '7893318:153043',
    },
    'titles': {
        'raw': 'Смартфон Apple iPhone SE 32GB',
        'highlighted': [
            {
                'value': 'Смартфон Apple iPhone SE 32GB',
            },
        ],
    },
    'slug': 'smartfon-apple-iphone-se-32gb',
    'description': 'Описание',
    'categories': [
        {
            'entity': 'category',
            'id': 91491,
            'nid': 54726,
            'name': 'Мобильные телефоны',
            'slug': 'mobilnye-telefony',
            'fullName': 'Мобильные телефоны',
            'type': 'guru',
            'cpaType': 'cpc_and_cpa',
            'isLeaf': true,
            'kinds': [],
        },
    ],
    'cpc': 'GhHB1NzuxulAPvtUTHYnfZ-qZTZBRktz',
    'urls': {
        'direct': '//market.yandex.ru/product/1721921261?hid=91491&nid=54726',
    },
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
    'pictures': [
        {
            'entity': 'picture',
            'original':
                {
                    'containerWidth': 289,
                    'containerHeight': 701,
                    'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/orig',
                    'width': 289,
                    'height': 701,
                },
            'thumbnails':
                [
                    {
                        'containerWidth': 50,
                        'containerHeight': 50,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/1hq',
                        'width': 50,
                        'height': 50,
                    },
                    {
                        'containerWidth': 100,
                        'containerHeight': 100,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/2hq',
                        'width': 100,
                        'height': 100,
                    },
                    {
                        'containerWidth': 75,
                        'containerHeight': 75,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/3hq',
                        'width': 75,
                        'height': 75,
                    },
                    {
                        'containerWidth': 150,
                        'containerHeight': 150,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/4hq',
                        'width': 150,
                        'height': 150,
                    },
                    {
                        'containerWidth': 200,
                        'containerHeight': 200,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/5hq',
                        'width': 200,
                        'height': 200,
                    },
                    {
                        'containerWidth': 250,
                        'containerHeight': 250,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/6hq',
                        'width': 250,
                        'height': 250,
                    },
                    {
                        'containerWidth': 120,
                        'containerHeight': 120,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/7hq',
                        'width': 120,
                        'height': 120,
                    },
                    {
                        'containerWidth': 240,
                        'containerHeight': 240,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/8hq',
                        'width': 240,
                        'height': 240,
                    },
                    {
                        'containerWidth': 500,
                        'containerHeight': 500,
                        'url': '//avatars.mds.yandex.net/get-mpic/331398/img_id2458354758628174106/9hq',
                        'width': 500,
                        'height': 500,
                    },
                ],
            'filtersMatching':
                {
                    '14871214':
                        [
                            '14897638',
                        ],
                    '13887626':
                        [
                            '13898623',
                        ],
                },
        },
    ],
    'filters': [filterMock],
    'meta': {},
    'type': 'model',
    'id': 1721921261,
    'offers': {
        'count': 817,
        'cutPriceCount': 0,
    },
    'isNew': false,
    'prices': {
        'min': '14990',
        'max': '22950',
        'currency': 'RUR',
        'avg': '16990',
    },
    'opinions': 8,
    'rating': 4,
    'ratingCount': 38,
    'reviews': 4,
    'retailersCount': 446,
    'promo': {
        'whitePromoCount': 8,
    },
};

const product = createProduct(productMock, productMock.id);
const colorFilter = createFilter(filterMock, '13887626');

const pictures = productMock.pictures
    .map(pic => createEntityPicture(pic, 'product', productMock.id, pic.original.url));

const filterValues = filterMock.values
    .map(filter => ({
        collections: {
            filterValue: createFilterValue(filter, '13887626', filter.id).collections.filterValue,
        },
    }));

const state = mergeState([
    ...pictures,
    product,
    colorFilter,
    ...filterValues,
]);

const route = {
    nid: '54726',
    slug: 'mobilnye-telefony',
    glfilter: '13887626:13899071',
};

export default {
    state,
    route,
};

