/* eslint-disable */

import {flatMap} from 'lodash/fp';

import {createProduct, mergeState, createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers';

const productIds = [
    382523005,
    382680005,
    382678003,
    368058328,
    352644020,
    348355318,
    278966132,
    201855059,
    197651860,
    194606344,
    194610419,
    192802371,
    183263107,
    175233429,
    144750053,
    144765065,
    114392836,
    85842095,
    84440409,
    72515081,
    70797771,
    61072677,
    43055391,
    43055392,
    42168929,
    39919682,
    33166037,
    1971826634,
    1968987605,
    1968987802,
    1968987836,
    1950474529,
    1921876915,
    1901397008,
    1881106514,
    1767039336,
    1731400948,
    1731401024,
    1731400950,
    1730308792,
];

export default {
    Tarantino: {
        'entity': 'page',
        'id': 43105,
        'rev': 314268,
        'type': 'brand',
        'name': 'Samsung',
        'hasContextParams': true,
        'bindings': [{'entity': 'vendor', 'id': '153061'}, {'entity': 'domain', 'id': 'ru'}],
        'info': {
            'seo': {
                'title': 'Samsung (Самсунг) — Каталог товаров — Яндекс.Маркет',
                'description': 'Популярные товары бренда Samsung на Маркете. Каталог товаров производителя: цены, характеристики, обзоры, обсуждения, отзывы и оценки покупателей.',
            },
        },
        'content': {
            'entity': 'box', 'rows': [{
                'entity': 'row', 'gridId': '8-columns', 'columns': [{
                    'entity': 'column',
                    'gridId': '8-columns',
                    'width': 8,
                    'widgets': [{
                        'entity': 'widget',
                        'id': 'SearchResultAlert',
                        'uid': 19991282,
                        'loadMode': 'default',
                    }, {
                        'entity': 'widget',
                        'id': 'BrandHeadline',
                        'uid': 7290244,
                        'loadMode': 'default',
                        'pageId': 43105,
                        'resources': {
                            'brand': {
                                'garsons': [{
                                    'id': 'CustomBrand',
                                    'params': {
                                        'data': {
                                            'entity': 'vendor',
                                            'id': 153061,
                                            'name': 'Samsung',
                                            'description': 'Компания SAMSUNG образована в 1969 году. Сейчас в корпорации Samsung Electronics насчитывается 21 производственное дочернее предприятие, 29 торговых подразделений и 24 зарубежных представительства в 46 странах мира. Продукция компании охватывает практически весь спектр потребительских товаров.',
                                            'logo': {
                                                'entity': 'picture',
                                                'url': 'https://avatars.mds.yandex.net/get-marketcms/1357599/img-231368ae-c15e-4e63-b588-0b27aa5f9716.png/optimize',
                                            },
                                            'website': 'http://www.samsung.com/ru/home',
                                            'offersCount': 90174,
                                            'categoriesCount': 129,
                                            'hasArticle': false,
                                        },
                                    },
                                }],
                            },
                        },
                        'view': {'warnings': {'noOffers': {'text': 'К сожалению, продукции бренда  сейчас нет в продаже.'}}},
                        'metrika': {
                            'aboutClick': 'brandpage_premium_button_about',
                            'allBrandsClick': 'brandpage_premium_brand_all',
                            'onWebsiteClick': 'brandpage_premium_popup_about_link',
                            'onAboutClick': 'brandpage_premium_popup_about_show',
                        },
                    }, {
                        'entity': 'widget',
                        'id': 'MediaSet',
                        'uid': 37097421,
                        'loadMode': 'default',
                        'pageId': 43105,
                        'resources': {
                            'garsons': [{
                                'id': 'MediaSet', 'params': {
                                    'data': [{
                                        'entity': 'banner',
                                        'id': 37138419,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '853',
                                            'height': '360',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-45da6a47-966c-4345-8d6c-0eec139d7f2f.jpeg/optimize',
                                            'selectedThumb': '853x360',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x360',
                                                'containerWidth': '853',
                                                'containerHeight': '360',
                                                'width': '853',
                                                'height': '360',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '000000',
                                                        'right': '000000',
                                                        'bottom': '101315',
                                                        'left': '000000',
                                                    }, 'full': '101313',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-fc27d2b5-3c13-4f31-b16e-93544324e3c0.jpeg/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog--mobilnye-telefony/54726/list?hid=91491&glfilter=7893318%3A153061&glfilter=12616441%3A16066051',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 41412636,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '853',
                                            'height': '360',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-a88e7e3a-a2ad-4d0d-a847-c507924e2121.jpeg/optimize',
                                            'selectedThumb': '853x360',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x360',
                                                'containerWidth': '853',
                                                'containerHeight': '360',
                                                'width': '853',
                                                'height': '360',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f0f0f0',
                                                        'right': 'f5f5f5',
                                                        'bottom': 'efeff0',
                                                        'left': 'f5f5f5',
                                                    }, 'full': 'e4e6e4',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1779479/img-5c916935-567d-4ab7-a9f8-b126d7a20da9.jpeg/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog--mobilnye-telefony/54726/list?hid=91491&glfilter=7893318%3A153061&glfilter=12616441%3A16066051&glfilter=12782797%3A13732224',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 41412648,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '853',
                                            'height': '360',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1357599/img-1bcfb725-e90c-42da-b360-70b45ceeaf5d.jpeg/optimize',
                                            'selectedThumb': '853x360',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x360',
                                                'containerWidth': '853',
                                                'containerHeight': '360',
                                                'width': '853',
                                                'height': '360',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '000000',
                                                        'right': '1a1a19',
                                                        'bottom': '534a47',
                                                        'left': '101011',
                                                    }, 'full': '332e2d',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1532570/img-009ebdd7-91b8-4995-b2f5-6f49be9cdfcb.jpeg/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog--televizory/59601/list?hid=90639&glfilter=7893318%3A153061&glfilter=15063566%3A1&how=ddate',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 37097441,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '853',
                                            'height': '360',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-2c9315bf-5840-4e1d-b5a5-dd0ffd73362d.png/optimize',
                                            'selectedThumb': '853x360',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x360',
                                                'containerWidth': '853',
                                                'containerHeight': '360',
                                                'width': '853',
                                                'height': '360',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '414747',
                                                        'right': '0e161c',
                                                        'bottom': '31495d',
                                                        'left': '192530',
                                                    }, 'full': '293440',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/944743/img-ad1539f8-5eb2-4bb1-b5e2-356d60dd48af.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog--vertikalnye-pylesosy/83800/list?text=powerstick%20pro%20samsung&hid=16302537&rt=9&was_redir=1&srnum=21&rs=eJwzcjFy4LLj4uV41s0jwCzBoLpaW2E_kHv4MKsAE5CbwfJpHxcfx9sFN9gFGIF8de_kPUD-yebH7AIMEsyqhnwn7QIYAV0oEY8%2C',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 37097449,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '853',
                                            'height': '360',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1523779/img-ad1c5d30-8b51-4518-829c-6b1ccb8a9934.png/optimize',
                                            'selectedThumb': '853x360',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x360',
                                                'containerWidth': '853',
                                                'containerHeight': '360',
                                                'width': '853',
                                                'height': '360',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'a59d95',
                                                        'right': '9c9390',
                                                        'bottom': '483936',
                                                        'left': '6f6b60',
                                                    }, 'full': '847a78',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-ce24d490-3aea-4df2-9035-a8368d93a596.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog/59601/list?hid=90639&glfilter=7893318%3A153061&glfilter=15063566%3A1&how=ddate',
                                        'metrika': {},
                                    }],
                                },
                            }],
                        },
                        'view': {
                            'layouts': [{
                                'entity': 'layout',
                                'id': '853x360',
                                'placesCount': 1,
                                'rows': [{
                                    'entity': 'row',
                                    'columns': [{
                                        'entity': 'column',
                                        'place': {'width': 853, 'height': 360, 'position': 1},
                                    }],
                                }],
                            }, {
                                'entity': 'layout',
                                'id': '853x360',
                                'placesCount': 1,
                                'rows': [{
                                    'entity': 'row',
                                    'columns': [{
                                        'entity': 'column',
                                        'place': {'width': 853, 'height': 360, 'position': 1},
                                    }],
                                }],
                            }, {
                                'entity': 'layout',
                                'id': '853x360',
                                'placesCount': 1,
                                'rows': [{
                                    'entity': 'row',
                                    'columns': [{
                                        'entity': 'column',
                                        'place': {'width': 853, 'height': 360, 'position': 1},
                                    }],
                                }],
                            }, {
                                'entity': 'layout',
                                'id': '853x360',
                                'placesCount': 1,
                                'rows': [{
                                    'entity': 'row',
                                    'columns': [{
                                        'entity': 'column',
                                        'place': {'width': 853, 'height': 360, 'position': 1},
                                    }],
                                }],
                            }, {
                                'entity': 'layout',
                                'id': '853x360',
                                'placesCount': 1,
                                'rows': [{
                                    'entity': 'row',
                                    'columns': [{
                                        'entity': 'column',
                                        'place': {'width': 853, 'height': 360, 'position': 1},
                                    }],
                                }],
                            }], 'pager': {'place': 'onSides', 'autoPlay': true}, 'size': 'l',
                        },
                        'metrika': {},
                    }, {
                        'entity': 'widget',
                        'id': 'BubbleNavigation',
                        'uid': 7237193,
                        'loadMode': 'default',
                        'pageId': 43105,
                        'resources': {
                            'garsons': [{'id': 'BrandNavnodes', 'count': 200, 'params': {'vendor_id': '153061'}}, {
                                'id': 'CustomNavnodes', 'params': {
                                    'data': [{
                                        'entity': 'navnode',
                                        'fullName': 'Смартфоны',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '1000',
                                            'height': '1000',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-b43ee5cf-9e25-484e-a4e1-0f9973c37d11.jpeg/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '38x38',
                                                'containerWidth': '38',
                                                'containerHeight': '38',
                                                'width': '38',
                                                'height': '38',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'fefefe',
                                                        'right': 'fefefe',
                                                        'bottom': 'ffffff',
                                                        'left': 'fefefd',
                                                    }, 'full': 'a8aab0',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1533751/img-caa5ba7d-c8de-443c-b441-f1e04413c9e7.jpeg/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1533751/img-b9e1ece9-5332-454e-993a-25ecc0f91d15.jpeg/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '50x50',
                                                'containerWidth': '50',
                                                'containerHeight': '50',
                                                'width': '50',
                                                'height': '50',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'ffffff',
                                                        'right': 'fefefe',
                                                        'bottom': 'ffffff',
                                                        'left': 'fefefe',
                                                    }, 'full': 'a8a9b1',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/475644/img-587b4a00-086d-43aa-8804-df816d030019.jpeg/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1523779/img-c9876a87-b36d-4b1a-8318-a0b7003b5ce1.jpeg/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '125x125',
                                                'containerWidth': '125',
                                                'containerHeight': '125',
                                                'width': '125',
                                                'height': '125',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'ffffff',
                                                        'right': 'fefefe',
                                                        'bottom': 'ffffff',
                                                        'left': 'fefefe',
                                                    }, 'full': 'a8aab1',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-c83c8ddd-83a6-4a2f-a123-4e020a54a4b6.jpeg/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1533751/img-08d62c6f-4cc3-4877-9ac8-f20e7433942b.jpeg/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/54726/list?glfilter=7893318%3A153061',
                                        'navnodes': [{
                                            'entity': 'navnode',
                                            'fullName': 'Планшеты',
                                            'icons': [{
                                                'entity': 'picture',
                                                'width': '4500',
                                                'height': '3102',
                                                'url': '//avatars.mds.yandex.net/get-marketcms/879900/img-19614fe5-b5dc-454c-824d-1ea1fb6d470e.jpeg/optimize',
                                                'thumbnails': [],
                                            }],
                                            'link': '/catalog/54545/list?hid=6427100&glfilter=7893318%3A153061',
                                            'navnodes': [{
                                                'entity': 'navnode',
                                                'fullName': 'Носимые устройства',
                                                'icons': [{
                                                    'entity': 'picture',
                                                    'width': '3000',
                                                    'height': '2000',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1357599/img-cea184bd-d72f-4f8c-89dc-4da17e147305.jpeg/optimize',
                                                    'thumbnails': [],
                                                }],
                                                'link': '/multisearch?hid=10498025&hid=10972670&hid=14369750&gfilter=7893318%3A153061',
                                                'navnodes': [],
                                            }],
                                        }],
                                    }],
                                },
                            }], 'bubble': {'garsons': [], 'completers': []}, 'popup': {'garsons': [], 'completers': []},
                        },
                        'view': {
                            'toggleButton': {
                                'more': {'text': 'Все категории'},
                                'less': {'text': 'Свернуть категории'},
                            },
                            'bubbles': {'hidden': false},
                            'header': {'title': {}},
                            'snippets': {
                                'bordered': false,
                                'columns': {'count': '7'},
                                'rows': {'count': '1'},
                                'cells': {'type': 'brand'},
                            },
                            'modal': {'search': {'visible': false}},
                            'modalButton': {'style': {'theme': 'secondary', 'size': 'medium'}},
                        },
                        'metrika': {
                            'track': 'brandpage_premium_catxt',
                            'onVisible': 'brandpage_premium_bubbles_show',
                            'onClick': 'brandpage_premium_brand_bbl',
                            'allCategoriesClick': 'brandpage_premium_categories_to_all',
                        },
                    }],
                }],
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'default', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default'},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'ScrollBox',
                        'id': 43832688,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'ProductsByIds',
                                'params': {'hyperid': productIds},
                            }], 'completers': [{'id': 'offers', 'params': {}}],
                        },
                        'props': {
                            'title': 'Смартфоны Samsung',
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'extended',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'size': 'm',
                            'controlsSize': 'm',
                            'align': 'top',
                            'horizontalAlign': 'left',
                            'minCountToShow': 0,
                            'deduplicate': false,
                            'priceLinkToOffers': false,
                            'clickableTitle': false,
                            'withSeparator': false,
                            'adaptiveLayout': false,
                        },
                    }, {
                        'entity': 'widget',
                        'name': 'VideoFrame',
                        'id': 45559078,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'VideoFrame',
                                'params': {
                                    'width': 720,
                                    'height': 315,
                                    'hosting': 'frontend.vh.yandex.ru',
                                    'url': 'https://frontend.vh.yandex.ru/player/17956158786275859604?from=ya-market&autoplay=0',
                                    'title': 'Встречайте смартфон будущего – новый Galaxy S10 Иммерсивный экран, интеллектуальная камера, беспроводная зарядка Powershare',
                                },
                            }],
                        },
                    }],
                }],
            }, {
                'entity': 'row',
                'gridId': '8-columns',
                'columns': [{'entity': 'column', 'gridId': '8-columns', 'width': 8, 'widgets': []}],
            }, {
                'entity': 'row',
                'gridId': '8-columns',
                'columns': [{'entity': 'column', 'gridId': '8-columns', 'width': 8, 'widgets': []}],
            }],
        },
    },
    report: mergeState(
        flatMap(
            id => [
                createProduct({slug: 'product'}, id),
                createEntityPicture(
                    {
                        original: {
                            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
                        },
                        thumbnails: [{
                            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
                        }],
                    },
                    'product',
                    id,
                    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/1'
                )
            ],
            productIds
        )
    )
};

