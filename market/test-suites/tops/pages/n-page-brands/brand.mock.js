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
        'id': 105933,
        'rev': 1255547,
        'type': 'brand_desktop',
        'name': 'Дефолтная страница - Десктоп',
        'bindings': [{'entity': 'domain', 'id': 'ru'}, {'entity': 'domain', 'id': 'by'}, {
            'entity': 'domain',
            'id': 'kz'
        }, {'entity': 'domain', 'id': 'ua'}],
        'info': {
            'seo': {
                'title': 'AEG — Каталог товаров — Яндекс.Маркет',
                'description': 'Популярные товары бренда AEG на Маркете. Каталог товаров производителя: цены, характеристики, обзоры, обсуждения, отзывы и оценки покупателей.'
            }
        },
        'content': {
            'entity': 'box',
            'rows': [{
                'entity': 'row',
                'gridId': 'no',
                'columns': [{
                    'entity': 'column',
                    'gridId': 'no',
                    'width': 'screen',
                    'widgets': [{
                        'entity': 'widget',
                        'id': 'Header',
                        'loadMode': 'default',
                        'props': {'type': 'default'}
                    }, {'entity': 'widget', 'id': 'TopMenu', 'loadMode': 'default'}]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'SearchResultAlert',
                        'id': 83123115,
                        'loadMode': 'default',
                        'props': {'type': 'brand'}
                    }, {
                        'resources': {
                            'garsons': [{
                                'id': 'CustomBrand',
                                'params': {
                                    'data': {
                                        'entity': 'vendor',
                                        'id': 152789,
                                        'name': 'AEG',
                                        'description': 'АЕG - Allgemeine Electrisitat Gesellschaft - основана в 1887 году. В 1908 году компанией была выпущена первая стиральная машина. В 1910 году в Берлине прошла испытание первая электрическая плита. В 1913 году был выпущен пылесос Dandy. На сегодняшний день AEG является знаменитой торговой маркой в Европе по производству встраиваемой и малой бытовой техники.',
                                        'logo': {
                                            'entity': 'picture',
                                            'url': '//avatars.mds.yandex.net/get-mpic/200316/img_id1788363779728079992/orig'
                                        },
                                        'website': 'http://www.aeg.ru',
                                        'offersCount': 1721,
                                        'categoriesCount': 107,
                                        'hasArticle': false
                                    }
                                }
                            }]
                        },
                        'props': {
                            'noOffersText': 'К сожалению, продукции бренда AEG сейчас нет в продаже.',
                            'textColor': '#2B2B2B'
                        },
                        'id': 83123116,
                        'name': 'BrandHeadline',
                        'entity': 'widget'
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'default', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'Rubricator',
                        'id': 83122517,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'BrandNavnodes',
                                'count': 200,
                                'params': {'vendor_id': '152789'}
                            }]
                        },
                        'props': {
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'condensed',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'showActionElement': false,
                            'labels': {'more': 'Все категории', 'less': 'Свернуть категории'},
                            'bordered': false,
                            'size': 'm',
                            'rows': 2,
                            'contentAlign': 'center',
                            'snippetView': 'default',
                            'isAdult': false
                        },
                        'metrika': {'onVisible': 'brandpage_premium_video_kak-zagrozhatj-posudomoyku_show'}
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'props': {
                            'headerText': 'Товары бренда AEG',
                            'withCategories': false,
                            'titleTheme': 'title',
                            'useDoWareMd5': false,
                            'promoHubId': 'promoHubId',
                            'usePromoCollections': true,
                            'showTrustElement': false,
                            'defaultPromoTypes': [],
                            'showHeader': true,
                            'pageQty': 10,
                            'theme': 'default',
                            'showMoreButton': true,
                            'showTabs': false,
                            'showTitle': true
                        },
                        'resources': {
                            'garsons': [{
                                'count': 40,
                                'params': {'vendor_id': 152789},
                                'id': 'BrandPopularProducts'
                            }]
                        },
                        'id': 82817135,
                        'name': 'Recommendations',
                        'entity': 'widget'
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'ScrollBox',
                        'id': 83120544,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{'id': 'Discounts', 'count': 40, 'params': {'vendor_id': '152789'}}],
                            'completers': []
                        },
                        'props': {
                            'title': 'Скидки на популярные товары бренда',
                            'subtitle': {'type': 'discount', 'text': 'ДИСКОНТ'},
                            'titleParams': {'size': 'm', 'type': 'default', 'align': 'center', 'tag': 'h2'},
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'size': 'm',
                            'ratio': [1, 4],
                            'withGutters': true,
                            'showControls': true,
                            'controlsSize': 'm',
                            'align': 'top',
                            'horizontalAlign': 'center',
                            'snippets': {
                                'discountBadgeType': 'default',
                                'flexible': true,
                                'withTitle': true,
                                'withPrice': true,
                                'useDefaultOffer': true,
                                'priceFirst': true,
                                'type': 'vertical',
                                'shade': false,
                                'withFirstVertical': false,
                                'withCategory': false,
                                'withoutSnippetPreview': true,
                                'promoMechanics': {
                                    'discount': true,
                                    'dealsBadge': true
                                },
                                'withReasonsToBuy': false,
                                'withCartButton': false
                            },
                            'minCountToShow': 0,
                            'deduplicate': false,
                            'priceLinkToOffers': false,
                            'clickableTitle': false,
                            'withSeparator': false,
                            'adaptiveLayout': false,
                            'isAdult': false
                        }
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'props': {'recommendedShopUrl': ''},
                        'id': 83123864,
                        'name': 'RecommendedShopsInforming',
                        'entity': 'widget'
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'props': {
                            'headerText': 'Смотрите также',
                            'autoplay': false,
                            'showHeader': true,
                            'title': 'Свойства виджета'
                        },
                        'resources': {'garsons': [{'count': 12, 'params': {'id': '152789'}, 'id': 'SimilarBrands'}]},
                        'id': 83123209,
                        'name': 'LogoCarousel',
                        'entity': 'widget'
                    }]
                }]
            }, {
                'entity': 'box',
                'name': 'Grid12',
                'props': {'type': 'row', 'width': 'maya', 'layout': true, 'grid': 1},
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default', 'sticky': false},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'WysiwygText',
                        'id': 83150199,
                        'loadMode': 'default',
                        'props': {'theme': 'mbo-page-text'},
                        'resources': {
                            'garsons': [{
                                'id': 'WysiwygText',
                                'params': {'text': '<p> </p>\n\n<p>Это страница бренда AEG</p>\n\n<p>Здесь собраны все товары бренда, которые можно выбрать на Маркете и заказать в интернет-магазинах с доставкой в Москву</p>\n\n<p>Коротко о бренде:</p>\n\n<ul>\n\t<li>Страна: -</li>\n\t<li>Год основания: 1887</li>\n\t<li>Товары бренда AEG продают 136 магазинов на Маркете — всего 526 товаров в 107 категориях.</li>\n</ul>'}
                            }]
                        }
                    }]
                }]
            }, {
                'entity': 'row',
                'gridId': 'no',
                'columns': [{
                    'entity': 'column',
                    'gridId': 'no',
                    'width': 'screen',
                    'widgets': [{'entity': 'widget', 'id': 'Footer', 'loadMode': 'default', 'props': {}}]
                }]
            }]
        },
        '_debug': {
            'page_id': 105933,
            'branch_id': 5,
            'branch_revision_id': 142355,
            'page_name': 'Дефолтная страница - Десктоп',
            'revision_id': 1255547
        }
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

