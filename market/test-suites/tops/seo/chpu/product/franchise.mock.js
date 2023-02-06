/* eslint-disable */

import {flatMap} from 'lodash/fp';

import {createProduct, mergeState, createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers';

const productIds = [
    11918753,
    1726813993,
    1727766112,
    1973551418,
    1884058709,
    1727440637,
    1728482563,
    1820735923,
    1806557460,
    1812947154,
    1733314963,
    1728175559,
    1694278406,
    1705698763,
    42168832,
    14196697,
    1726813930,
    1711387612,
    1809972044,
    417637829,
    1723530897,
    1970967499,
    1733067479,
    330862819,
    1728173156,
    123929601,
    30430160,
    1946957289,
    1729208313,
    122871034,
    1729321017,
    417263074,
];

export default {
    Tarantino: {
        'entity': 'page',
        'id': 48176,
        'rev': 321356,
        'type': 'franchise',
        'name': 'Звездные войны',
        'hasContextParams': true,
        'bindings': [{'entity': 'franchise', 'id': 14192222}, {'entity': 'domain', 'id': 'ru'}],
        'entrypoints': [{
            'suggest': {
                'text': 'Звездные войны',
                'aliases': ['Star wars', 'Звездные войны', 'звёздные войны'],
            },
        }],
        'info': {
            'seo': {
                'title': 'Звездные войны — Каталог товаров с героями Star Wars — Яндекс.Маркет',
                'description': 'Популярные товары с героями саги "Звездные войны" (Star Wars) на Маркете. Цены, характеристики, широкий ассортимент товаров.',
                'image': 'https://avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-9237ef89d392baeef331f94c5ff0b275.png/orig',
            },
        },
        'content': {
            'entity': 'box', 'rows': [{
                'entity': 'box',
                'name': 'Grid12',
                'props': {
                    'type': 'row',
                    'width': 'default',
                    'layout': true,
                    'grid': 1,
                    'backgroundImage': {
                        'entity': 'picture',
                        'width': '1706',
                        'height': '472',
                        'url': '//avatars.mds.yandex.net/get-mpic/1363071/cms_resources-navigation-pages-48176-3df560883529a2ac0e27ce0e0d2984cc.png/optimize',
                        'selectedThumb': '1420x10000',
                        'thumbnails': [{
                            'entity': 'thumbnail',
                            'id': '1420x10000',
                            'containerWidth': '1420',
                            'containerHeight': '10000',
                            'width': '1420',
                            'height': '393',
                            'averageColors': {
                                'borders': {
                                    'top': 'fceaea',
                                    'right': 'fefefe',
                                    'bottom': 'fdfcfc',
                                    'left': 'fefefe',
                                }, 'full': 'fae3e3',
                            },
                            'densities': [{
                                'entity': 'density',
                                'id': '1',
                                'url': '//avatars.mds.yandex.net/get-mpic/1215212/cms_resources-navigation-pages-48176-3df560883529a2ac0e27ce0e0d2984cc_1420x393@x1.png/optimize',
                            }],
                        }],
                    },
                },
                'nodes': [{
                    'entity': 'box',
                    'name': 'Grid12',
                    'props': {'type': 'column', 'layout': false, 'width': 1, 'position': 'default'},
                    'nodes': [{
                        'entity': 'widget',
                        'name': 'SearchResultAlert',
                        'id': 19728812,
                        'loadMode': 'default',
                        'props': {'type': 'franchise', 'color': '#000000'},
                    }, {
                        'entity': 'widget',
                        'name': 'Headline',
                        'id': 9366592,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'CustomBvlInformation',
                                'params': {
                                    'data': {
                                        'entity': 'bvl',
                                        'id': 14192222,
                                        'type': 'franchise',
                                        'bindings': [],
                                    },
                                },
                            }],
                        },
                        'props': {'ratio': [5, 1], 'theme': 'light'},
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
                        'name': 'MediaCarousel',
                        'id': 49776998,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'MediaSet', 'params': {
                                    'data': [{
                                        'entity': 'banner',
                                        'id': 49777002,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '720',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1532570/img-5a58eb59-c3bb-4bab-97be-8b62ea17359d.png/optimize',
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
                                                        'top': 'ac8e72',
                                                        'right': 'e7d5c3',
                                                        'bottom': 'dee5eb',
                                                        'left': '7a5a56',
                                                    }, 'full': 'a79588',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/944743/img-1ce25540-86b1-4c9a-b1d3-065a144a90b8.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/944743/img-e6bc96e4-7e37-4d80-b888-85affa7eaeeb.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog/59749/list?hid=10470548&glfilter=14020987%3A14192222&glfilter=15060326%3A15146281%2C15193937&viewtype=grid',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 49777016,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '720',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1490511/img-10bb3918-bf3a-46a4-ac08-6ee6ecede7c0.png/optimize',
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
                                                        'top': '711141',
                                                        'right': '4b072d',
                                                        'bottom': '630a28',
                                                        'left': 'af172e',
                                                    }, 'full': '701436',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1490511/img-989768d2-a04d-4c01-8453-8526495726b5.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-f7ecae98-7c7c-4eb5-8dc8-30dce8e8df81.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/multisearch?hid=10470548&hid=10683227&hid=10682592&hid=922144&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281&onstock=1',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 49777030,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '720',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/944743/img-6423842d-e4de-450b-9d91-1e01cfe940d1.png/optimize',
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
                                                        'top': '070405',
                                                        'right': '090607',
                                                        'bottom': '232021',
                                                        'left': '1e1b1c',
                                                    }, 'full': '1c1818',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-4daeab2c-72aa-42ec-b81d-318fef2ea81f.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1490511/img-e2b5e4ef-80eb-4ecf-b9f8-574c8088611c.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog/59702/list?glfilter=14020987%3A14192222&glfilter=15060326%3A15146281%2C15193937&glfilter=15086295%3A15193128%2C15280129&viewtype=grid',
                                        'metrika': {},
                                    }],
                                },
                            }],
                        },
                        'props': {
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'autoplay': false,
                            'looped': false,
                            'layouts': [],
                            'display': 'adaptive',
                            'adaptiveLayout': false,
                            'size': 'm',
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
                        'id': 9691608,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'CustomFormulas', 'params': {
                                    'data': [{
                                        'entity': 'formula', 'id': 9691611, 'pictures': [{
                                            'entity': 'picture',
                                            'width': '472',
                                            'height': '628',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '181030',
                                                        'right': '584a62',
                                                        'bottom': '6d4d60',
                                                        'left': '131633',
                                                    }, 'full': '372e49',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '181030',
                                                        'right': '584a62',
                                                        'bottom': '6d4c60',
                                                        'left': '131533',
                                                    }, 'full': '372e49',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '181030',
                                                        'right': '584a62',
                                                        'bottom': '6d4d61',
                                                        'left': '131633',
                                                    }, 'full': '372e49',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-3a881e8bed75c19055fb2ebd4aac20dd_320x320@x1.png/optimize',
                                                }],
                                            }],
                                        }], 'link': '/franchise--temnaia-storona/15294598',
                                    }, {
                                        'entity': 'formula', 'id': 9691633, 'pictures': [{
                                            'entity': 'picture',
                                            'width': '472',
                                            'height': '628',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'ffffff',
                                                        'right': 'c7d0d9',
                                                        'bottom': '4c5b6f',
                                                        'left': 'd4e1ef',
                                                    }, 'full': 'a1aaba',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'ffffff',
                                                        'right': 'c7d0d9',
                                                        'bottom': '4c5b6f',
                                                        'left': 'd4e1ef',
                                                    }, 'full': 'a1aaba',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'ffffff',
                                                        'right': 'c7d0d9',
                                                        'bottom': '4d5b6f',
                                                        'left': 'd4e1ef',
                                                    }, 'full': 'a1aaba',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-6be92b1eb80b68ea5c385456b275fc4b_320x320@x1.png/optimize',
                                                }],
                                            }],
                                        }], 'link': '/franchise--svetlaia-storona/15294599',
                                    }],
                                },
                            }], 'completers': [],
                        },
                        'props': {
                            'title': 'Выбери свою сторону Силы',
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'size': 'm',
                            'ratio': [2, 5],
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
                        'name': 'VideoFrame',
                        'id': 49777060,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'VideoFrame',
                                'params': {
                                    'width': 740,
                                    'height': 320,
                                    'hosting': 'frontend.vh.yandex.ru',
                                    'url': 'https://frontend.vh.yandex.ru/player/2788537994053022062?from=ya-market&autoplay=0',
                                },
                            }],
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
                        'name': 'Rubricator',
                        'id': 9721168,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'CustomNavnodes', 'params': {
                                    'data': [{
                                        'entity': 'navnode',
                                        'fullName': 'Конструкторы',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b32323',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/200316/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b32323',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b32323',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-10b6b29195877b8a475f6314fd2047b7_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/59749/list?hid=10470548&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Игровые наборы и фигурки',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/364668/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1c1c',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1c1c',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/364668/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/372220/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1c1c',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-ad9b95cb4fceb118fb52f5e2993a9474_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/59694/list?hid=10683227&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Машинки и техника',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b01f20',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b01f20',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'b01f20',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-2b31cc28ee9a83c5c343fb3177ba8338_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/59698/list?hid=10682592&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Роботы',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13031',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/364668/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13031',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/364668/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13031',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-5eec174065cb7751fa1dc6a49699327b_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/59702/list?hid=10682618&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Игрушечные оружие и бластеры',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/372220/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c22c29',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c22c29',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/364668/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/331398/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c22c29',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-28fcf6a8b2552c6393d76757e619d22e_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/59695/list?hid=10682597&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Одежда',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': '972032',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/199079/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': '982032',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/372220/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': '972032',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-c77ad217103c35d3dad05c99c1eeb2b3_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/multisearch?hid=7812191&hid=7811879&hid=7811877&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Рюкзаки и ранцы для школы',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/372220/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1616',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/175985/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1616',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/175985/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/200316/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'ad1616',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-13c1f6ae17de28560dbd3977408d8ffb_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/67092/list?hid=13858259&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }, {
                                        'entity': 'navnode',
                                        'fullName': 'Карнавальные костюмы и маски',
                                        'icons': [{
                                            'entity': 'picture',
                                            'width': '648',
                                            'height': '432',
                                            'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f.png/optimize',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '200x200',
                                                'containerWidth': '200',
                                                'containerHeight': '200',
                                                'width': '200',
                                                'height': '200',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13332',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_200x200@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/372220/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_200x200@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '250x250',
                                                'containerWidth': '250',
                                                'containerHeight': '250',
                                                'width': '250',
                                                'height': '250',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13332',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_250x250@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/397397/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_250x250@x2.png/optimize',
                                                }],
                                            }, {
                                                'entity': 'thumbnail',
                                                'id': '320x320',
                                                'containerWidth': '320',
                                                'containerHeight': '320',
                                                'width': '320',
                                                'height': '320',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'f40303',
                                                        'right': 'c30707',
                                                        'bottom': '920b0b',
                                                        'left': 'c30707',
                                                    }, 'full': 'c13332',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/466729/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_320x320@x1.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-mpic/200316/cms_resources-navigation-pages-48176-52e2dbeaa00a31e574cc4fa7cb6c946f_320x320@x2.png/optimize',
                                                }],
                                            }],
                                        }],
                                        'link': '/catalog/55632/list?hid=922144&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'navnodes': [],
                                    }],
                                },
                            }],
                        },
                        'props': {
                            'title': 'Популярные категории',
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'showActionElement': false,
                            'labels': {'more': 'Все категории', 'less': 'Свернуть категории'},
                            'bordered': false,
                            'size': 'l',
                            'rows': 3,
                            'contentAlign': 'auto',
                            'snippetView': 'default',
                        },
                        'metrika': {},
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
                        'name': 'MediaCarousel',
                        'id': 49777074,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'MediaSet', 'params': {
                                    'data': [{
                                        'entity': 'banner',
                                        'id': 49777078,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '472',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1534436/img-9dd2e332-51da-4083-bd60-94911cb69884.png/optimize',
                                            'selectedThumb': '853x236',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x236',
                                                'containerWidth': '853',
                                                'containerHeight': '236',
                                                'width': '853',
                                                'height': '236',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': 'aabebe',
                                                        'right': '7e8a93',
                                                        'bottom': 'a3aea7',
                                                        'left': 'bad4d2',
                                                    }, 'full': '919b99',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1534436/img-8cfb4872-6bed-462a-bcd4-2eb5787cf6bb.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1776516/img-9ef56296-e1d9-4aaa-a4af-f6e7690d5781.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog/63046/list?glfilter=14020987%3A14192222&glfilter=15060326%3A15146281%2C15193937&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 49777092,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '472',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1490511/img-2be44901-ba06-4e59-94e3-204871762154.png/optimize',
                                            'selectedThumb': '853x236',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x236',
                                                'containerWidth': '853',
                                                'containerHeight': '236',
                                                'width': '853',
                                                'height': '236',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '69241f',
                                                        'right': '753423',
                                                        'bottom': '4d1920',
                                                        'left': '8a4d27',
                                                    }, 'full': '79392a',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/944743/img-aa49d9da-bad2-4515-8d11-3a8c3fd0859b.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-15d13385-74e4-4be1-bc44-86cb8b7c6eb9.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/collections/svetovye-mechi',
                                        'metrika': {},
                                    }, {
                                        'entity': 'banner',
                                        'id': 49777106,
                                        'image': {
                                            'entity': 'picture',
                                            'width': '1706',
                                            'height': '472',
                                            'url': '//avatars.mds.yandex.net/get-marketcms/1668019/img-59e63c28-03a4-4c95-94b6-1112eb979c40.png/optimize',
                                            'selectedThumb': '853x236',
                                            'thumbnails': [{
                                                'entity': 'thumbnail',
                                                'id': '853x236',
                                                'containerWidth': '853',
                                                'containerHeight': '236',
                                                'width': '853',
                                                'height': '236',
                                                'averageColors': {
                                                    'borders': {
                                                        'top': '9e9b92',
                                                        'right': 'a6a197',
                                                        'bottom': '8f8075',
                                                        'left': 'b0a29a',
                                                    }, 'full': '9a9286',
                                                },
                                                'densities': [{
                                                    'entity': 'density',
                                                    'id': '1',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1534436/img-312d1a40-a4e4-41a9-ba4c-bf08e1cb2d59.png/optimize',
                                                }, {
                                                    'entity': 'density',
                                                    'id': '2',
                                                    'url': '//avatars.mds.yandex.net/get-marketcms/1776516/img-9a7673e3-2fc9-4520-b5db-acba65ed64da.png/optimize',
                                                }],
                                            }],
                                        },
                                        'link': '/catalog/59699/list?glfilter=14020987%3A14192222&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281',
                                        'metrika': {},
                                    }],
                                },
                            }],
                        },
                        'props': {
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'autoplay': false,
                            'looped': false,
                            'layouts': [],
                            'display': 'adaptive',
                            'adaptiveLayout': false,
                            'size': 'm',
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
                        'id': 9724504,
                        'loadMode': 'default',
                        'resources': {
                            'garsons': [{
                                'id': 'ProductsByIds',
                                'params': {'hyperid': productIds},
                            }], 'completers': [],
                        },
                        'props': {
                            'title': 'Популярные товары',
                            'subtitle': {'type': 'default'},
                            'titleParams': {'size': 'm', 'type': 'default'},
                            'link': {
                                'caption': 'Все товары',
                                'link': '/multisearch?hid=90783&hid=13491296&hid=90748&hid=10790728&hid=10682592&hid=90667&hid=7330337&hid=90712&hid=90701&hid=91597&hid=7330340&hid=7811879&gfilter=14020987%3A14192222&gfilter=15060326%3A15193937%2C15146281&onstock=1',
                                'theme': 'normal',
                                'position': 'title_right',
                            },
                            'paddingTop': 'normal',
                            'paddingBottom': 'normal',
                            'paddingLeft': 'normal',
                            'paddingRight': 'normal',
                            'theme': 'light',
                            'titleStyle': 'default',
                            'compensateSideMargin': false,
                            'size': 'm',
                            'ratio': [2, 5],
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
                    }],
                }],
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
    ),
};

