import {productWithSpecs} from '@self/platform/spec/hermione/fixtures/product';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

const createFormula = id => ({
    [id]: {
        'id': id,
        'entity': 'formula',
        'index': 0,
        'name': 'Скрабы и пилинги',
        'link': {
            'url': '/catalog/29034936/list?rs=eJwz0vBS4RJLzC4odLV0ys4uD0_NS0kxd00PL3IJFOIyNDE1M7C0MDE0S2AEAAI-C18%2C&tcl=1',
            'urlEndpoint': 'categories',
            'params': {
                'rs': 'eJwz0vBS4RJLzC4odLV0ys4uD0_NS0kxd00PL3IJFOIyNDE1M7C0MDE0S2AEAAI-C18,',
                'tcl': 1,
                'nid': 29034936,
                'models': '1456098416',
            },
        },
        'picture-source': 'model',
        'pictures': [
            {
                'entity': 'picture',
                'original': {
                    'width': 1731,
                    'height': 1331,
                    'namespace': 'mpic',
                    'groupId': 5257935,
                    'key': 'img_id7942799414048743262.jpeg',
                },
            },
        ],
    },
});

const createFormulaResultState = count => ({
    data: {
        search: {
            'results': new Array(count).fill(0).map((_, i) => i + 1).map(id => ({id, schema: 'formula'})),
            'title': 'Косметика в подарок или для себя',
            'dj-place': 'model_card_thematics_block',
            'dj-meta-place': 'model_card_thematics_block',
            'link': {'url': '/catalog/29034930/list?rs=eJwd0MtyokAUBuCampUuXeQFZrZUnW7obnopgRDwQlCRy4bISBQxqKByeeF5jdDsv_r_8x_8_7f9d_yyy683g2t5Xvtpsd8z4-CXujsZI4VQ4KqCqFA1fiaRefK20n05094Xb2myWtdCqUgmiADYf8Yv7G6zedCdnPTirWrXagotyOrJiLJeATBVoA6gWp7KbRRCc7hbl_ABvOsRJhQTDByJPu8jcworlN36iPRLUoZqE7fDVSpGjKgwREXn8hzRKayC2F5c35r4S-4W08kIyRwhADSgV8t46qfIA9fftPQyT4vue-NORipSZCYzqgi0D5P5NonyW1Ag9m_j7HxZa_okSjhXCCZcIFjkj1W1rxxZx0vdkkC615qoowRhpAAVyHnfQXzEplRsSouk_rEwAtbPEzdTGdAwzyyrb_v4anaVFNdnZZqQ9jYb3skoRVRBTERN1yY8SPrMsrZStnwef7XprO_jhHLM-48KlCGsJVmmGh916DnrdVLFh2s_jymMyJwi_PnrBzTrhPs%2C&tl=1', 'urlEndpoint': 'categories', 'params': {'rs': 'eJwd0MtyokAUBuCampUuXeQFZrZUnW7obnopgRDwQlCRy4bISBQxqKByeeF5jdDsv_r_8x_8_7f9d_yyy683g2t5Xvtpsd8z4-CXujsZI4VQ4KqCqFA1fiaRefK20n05094Xb2myWtdCqUgmiADYf8Yv7G6zedCdnPTirWrXagotyOrJiLJeATBVoA6gWp7KbRRCc7hbl_ABvOsRJhQTDByJPu8jcworlN36iPRLUoZqE7fDVSpGjKgwREXn8hzRKayC2F5c35r4S-4W08kIyRwhADSgV8t46qfIA9fftPQyT4vue-NORipSZCYzqgi0D5P5NonyW1Ag9m_j7HxZa_okSjhXCCZcIFjkj1W1rxxZx0vdkkC615qoowRhpAAVyHnfQXzEplRsSouk_rEwAtbPEzdTGdAwzyyrb_v4anaVFNdnZZqQ9jYb3skoRVRBTERN1yY8SPrMsrZStnwef7XprO_jhHLM-48KlCGsJVmmGh916DnrdVLFh2s_jymMyJwi_PnrBzTrhPs,', 'nid': 29034930, 'tl': 1, 'models': '1456098416,1481351500,678130078,256252091,1482175808,139110018,814373764,659945259,165121406,580863011,1476616417,956929130,747539612', 'thematic_id': 1008}},
            'recomParams': {
                'type-of-results': 'thematic_model_picture',
            },
            'total': 3,
            'totalOffers': 1,
            'totalOffersBeforeFilters': 1,
            'totalModels': 0,
        },
    },
    collections: {
        'formula': {
            ...new Array(count).fill(0).map((_, i) => i + 1).reduce((accum, id) => ({...accum, ...createFormula(id)}), {}),
        },
    },
});

const formulaCatalogerMock = {
    'entity': 'navnode',
    'category': {},
    'fullName': 'Подборки',
    'hasPromo': false,
    'id': 22488650,
    'isLeaf': true,
    'link': {
        'params': {
            'nid': [
                '22488650',
            ],
        },
        'target': 'catalog',
    },
    'name': 'Подборки',
    'navnodes': [
        {
            'childrenType': 'guru',
            'entity': 'navnode',
            'fullName': 'Косметика в подарок или для себя',
            'hasPromo': false,
            'id': 29034930,
            'isLeaf': false,
            'link': {
                'params': {
                    'nid': [
                        '29034930',
                    ],
                },
                'target': 'catalog',
            },
            'name': 'Косметика в подарок или для себя',
            'rootNavnode': {
                'entity': 'navnode',
                'id': 22488650,
            },
            'slug': 'kosmetika-v-podarok-ili-dlia-sebia',
            'type': 'virtual',
        },
        {
            'childrenType': 'guru',
            'entity': 'navnode',
            'fullName': 'Скрабы и пилинги',
            'hasPromo': false,
            'id': 29034936,
            'isLeaf': true,
            'link': {
                'params': {
                    'nid': [
                        '29034936',
                    ],
                },
                'target': 'catalogleaf',
            },
            'name': 'Скрабы и пилинги',
            'rootNavnode': {
                'entity': 'navnode',
                'id': 22488650,
            },
            'slug': 'skraby-i-pilingi',
            'type': 'category',
        },
    ],
    'rootNavnode': {
        'entity': 'navnode',
        'id': 22488650,
    },
    'slug': 'podborki',
    'type': 'virtual',
};

const productStateWithFormula = mergeReportState([productWithSpecs, createFormulaResultState(6)]);

export {
    productStateWithFormula,
    formulaCatalogerMock,
};
