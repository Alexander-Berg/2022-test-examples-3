'use strict';

const JLXParser = require('../../../../../../src/helper/jlx');

let parser;

describe('JSON-like-XML Parser', () => {
    beforeEach(() => {
        parser = new JLXParser();
    });

    test('should correct parse full response', () => {
        const response = {
            yandexsearch: {
                $: { version: '1.0' },
                request: [
                    {
                        query: ['ДЖИНСЫ MARVIN STRAIGHT 3999 РУБ.'],
                        page: ['0'],
                        sortby: [{ _: 'rlv', $: { order: 'descending', priority: 'no' } }],
                        maxpassages: [''],
                        groupings: [
                            {
                                groupby: [
                                    {
                                        $: {
                                            attr: 'ii',
                                            mode: 'deep',
                                            'groups-on-page': '1',
                                            'docs-in-group': '1',
                                            curcateg: '-1'
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ],
                response: [
                    {
                        $: { date: '20181018T122611' },
                        reqid: ['1539865571324443-99547400616809333409598-sas1-5960-XML'],
                        found: [
                            { _: '11228', $: { priority: 'phrase' } },
                            { _: '11228', $: { priority: 'strict' } },
                            { _: '11228', $: { priority: 'all' } }
                        ],
                        'found-human': ['Нашлось 11 тыс. ответов'],
                        'is-local': ['no'],
                        results: [
                            {
                                grouping: [
                                    {
                                        $: {
                                            attr: 'ii',
                                            mode: 'deep',
                                            'groups-on-page': '1',
                                            'docs-in-group': '1',
                                            curcateg: '-1'
                                        },
                                        found: [
                                            { _: '11183', $: { priority: 'phrase' } },
                                            { _: '11183', $: { priority: 'strict' } },
                                            { _: '11183', $: { priority: 'all' } }
                                        ],
                                        'found-docs': [
                                            { _: '11221', $: { priority: 'phrase' } },
                                            { _: '11221', $: { priority: 'strict' } },
                                            { _: '11221', $: { priority: 'all' } }
                                        ],
                                        'found-docs-human': ['нашёл 11 тыс. ответов'],
                                        page: [{ _: '0', $: { first: '1', last: '1' } }],
                                        group: [
                                            {
                                                categ: [{ $: { attr: 'ii', name: '8747087499842056476' } }],
                                                doccount: ['1'],
                                                relevance: ['105626256'],
                                                doc: [
                                                    {
                                                        $: { id: 'Z1080A03E049F16E1' },
                                                        relevance: ['105626256'],
                                                        url: [
                                                            'https://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg'
                                                        ],
                                                        domain: ['cdn.tom-tailor.com'],
                                                        title: ['Marvin straight denim'],
                                                        modtime: ['20180921T104930'],
                                                        size: ['0'],
                                                        charset: ['utf-8'],
                                                        passages: [
                                                            {
                                                                passage: [
                                                                    {
                                                                        _: '  denim - Men - mid stone wash denim.',
                                                                        hlword: ['Marvin', 'straight']
                                                                    }
                                                                ]
                                                            }
                                                        ],
                                                        properties: [
                                                            {
                                                                ImageSemDesc: [
                                                                    'BJ8W4RCAoD4IADAC6QIznbNmkBbVl49pXyBpGtuLSFiwrnvXO5SDvqX3onl1V4ailwSHNZiKeXjIBUpJy2oaW7Ctmrg8hJNes62auCuDowA='
                                                                ],
                                                                NNFeatures: [
                                                                    'EsgBMyr0FEgACfLwCd3857xIEhf73ejtEA0ANTE33toXCCEqpCOoAA7yO0_JpTbF69O26MYy3fwoERAV7iDb2vf-3wcX0iLLEKZB8vXQt_PVCPXu2j4MKOjw-wPPOj0PrN3rqwn1DWwGBefyS_EJFPP14Qvo9S3H8gjr3unfvRAg4ukS2yjJuVAb7dMQJO7n-_PaIwvsKuX26BcEzwsgBUMK0PgAEMbc-8eqHgEDqBoECHcwFtrxI9A36VLEEJ401VAo6Lnu_MwM8PUayAEV_f_2_wL-A_r6-_YBAf7--gUE_P4EAvn6APQLCPgABf8BBwH9_wQC_AABdwAFAwb8BQj6_QICBAAEBAb9AQP-AAH8-fr6BgID-PgAAvoHF_r__PkB-P8EEAIH_ff8_fwC_v4FEAIGAAUAAP4A9wL6-zoG_AD5EAgAEgAGA_b7_wABDf4B7vrv8QAU_hMCAv77BPEQ7_0AEAP4CP8I9vIPBe4GAgEQ9gYD8QAD_fwA_w0OAAAc_wX9_QoB_evyUQL4_-3xDgD8AA,,'
                                                                ],
                                                                _ImagesDups: [
                                                                    'i\t4726fb1759965c1b2508c78d4854c441\tc\t4512747636609425156\tw\t560\th\t745\ttw\t241\tth\t320\tu\thttps://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg\tuh\thttps://www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/\ts\t126480\tt\tjpg\tsmartcrop\t0x0+99x99\tsmartcropnoaspect\t21x0+64x99\tbq\t255\ttxt_img_sim\t0.7116\tdosid\t71\tdobbox\t29x7+47x76;56x75+14x16;37x80+19x15\ni\t4726fb1759965c1b2508c78d4854c441\tc\t4512747636609425156\tw\t560\th\t745\ttw\t241\tth\t320\tu\thttps://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg\tuh\thttps://www.tom-tailor.de/marvin-straight-jeans-manner-62041830910_1052/\ts\t126480\tt\tjpg\tsmartcrop\t0x0+99x99\tsmartcropnoaspect\t21x0+64x99\tbq\t255\ttxt_img_sim\t0.7181\tdosid\t71\tdobbox\t29x7+47x76;56x75+14x16;37x80+19x15'
                                                                ],
                                                                _ImagesJson: [
                                                                    '{"dc":"white","Passages":[{"qhits":"16","di":0,"lang":2,"title":"Marvin straight denim","text":"\\u0007[Marvin\\u0007] \\u0007[straight\\u0007] denim - Men - mid stone wash denim."}],"Preview":[{"di":0}]}'
                                                                ],
                                                                _Markers: [
                                                                    'SnipDebug=uil=ru;tld=ru;report=xml;exps=imgbuild;rsdups=0;pvdups=2;t=2;screenw=1366;fsframeh=768;frameh=563;framew=926;fsframew=1300;screenh=768;',
                                                                    'documentid=PqCAEOEWnwQAAAAAAAAAAA==',
                                                                    'SnipImageRanker=visquality_boost_ranker_viewportsize_1d5',
                                                                    'SnipTextRanker=page_relevance_ranker',
                                                                    'SnipTextFastRanker=images_stabilizer#corsa_based_ranker'
                                                                ],
                                                                _MetaSearcherHostname: [
                                                                    'sas1-7262.search.yandex.net:9200',
                                                                    'sas1-3327.search.yandex.net:9402',
                                                                    'sas1-5960.search.yandex.net:9080'
                                                                ],
                                                                _MimeType: ['2 0&d=3978362&sh=-1&sg='],
                                                                _SearcherHostname: ['sas1-6074.search.yandex.net:9300'],
                                                                _Shard: ['imgsidx-308-20181014-070347'],
                                                                documentid: ['PqCAEOEWnwQAAAAAAAAAAA=='],
                                                                imagetags: [
                                                                    'marvin=27.7931;denim=20.6395;wash=19.7601;mid=19.4711;stone=18.0788;jeans=11.7169;man=4.25254'
                                                                ]
                                                            }
                                                        ],
                                                        'image-properties': [
                                                            {
                                                                id: ['4726fb1759965c1b2508c78d4854c441'],
                                                                gid: ['PqCAEOEWnwQ'],
                                                                shard: ['0'],
                                                                'thumbnail-width': ['112'],
                                                                'thumbnail-height': ['150'],
                                                                'thumbnail-width-original': ['241'],
                                                                'thumbnail-height-original': ['320'],
                                                                'thumbnail-link': [
                                                                    'http://im0-tub-ru.yandex.net/i?id=4726fb1759965c1b2508c78d4854c441'
                                                                ],
                                                                'original-width': ['560'],
                                                                'original-height': ['745'],
                                                                'html-link': [
                                                                    'www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/'
                                                                ],
                                                                'image-link': [
                                                                    'cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg'
                                                                ],
                                                                'file-size': ['126480'],
                                                                'mime-type': ['jpg'],
                                                                dominated_color: ['white']
                                                            }
                                                        ],
                                                        'mime-type': ['text/html'],
                                                        'highlight-cookie': ['0&d=3978362&sh=-1&sg='],
                                                        'image-duplicates': [''],
                                                        'image-duplicates-preview': [
                                                            {
                                                                'image-properties': [
                                                                    {
                                                                        id: ['4726fb1759965c1b2508c78d4854c441'],
                                                                        gid: [''],
                                                                        shard: ['0'],
                                                                        'thumbnail-width': ['112'],
                                                                        'thumbnail-height': ['150'],
                                                                        'thumbnail-width-original': ['241'],
                                                                        'thumbnail-height-original': ['320'],
                                                                        'thumbnail-link': [
                                                                            'http://im0-tub-ru.yandex.net/i?id=4726fb1759965c1b2508c78d4854c441'
                                                                        ],
                                                                        'original-width': ['560'],
                                                                        'original-height': ['745'],
                                                                        'html-link': [
                                                                            'www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/'
                                                                        ],
                                                                        'image-link': [
                                                                            'cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg'
                                                                        ],
                                                                        'file-size': ['126480'],
                                                                        'mime-type': ['jpg']
                                                                    }
                                                                ]
                                                            }
                                                        ],
                                                        'image-duplicates-resized': ['']
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        };
        const result = {
            yandexsearch: {
                attributes: {
                    version: '1.0'
                },
                request: {
                    query: 'ДЖИНСЫ MARVIN STRAIGHT 3999 РУБ.',
                    page: '0',
                    sortby: {
                        value: 'rlv',
                        attributes: {
                            order: 'descending',
                            priority: 'no'
                        }
                    },
                    maxpassages: '',
                    groupings: {
                        groupby: {
                            attributes: {
                                attr: 'ii',
                                mode: 'deep',
                                'groups-on-page': '1',
                                'docs-in-group': '1',
                                curcateg: '-1'
                            }
                        }
                    }
                },
                response: {
                    attributes: {
                        date: '20181018T122611'
                    },
                    reqid: '1539865571324443-99547400616809333409598-sas1-5960-XML',
                    found: [
                        {
                            value: '11228',
                            attributes: {
                                priority: 'phrase'
                            }
                        },
                        {
                            value: '11228',
                            attributes: {
                                priority: 'strict'
                            }
                        },
                        {
                            value: '11228',
                            attributes: {
                                priority: 'all'
                            }
                        }
                    ],
                    'found-human': 'Нашлось 11 тыс. ответов',
                    'is-local': 'no',
                    results: {
                        grouping: {
                            attributes: {
                                attr: 'ii',
                                mode: 'deep',
                                'groups-on-page': '1',
                                'docs-in-group': '1',
                                curcateg: '-1'
                            },
                            found: [
                                {
                                    value: '11183',
                                    attributes: {
                                        priority: 'phrase'
                                    }
                                },
                                {
                                    value: '11183',
                                    attributes: {
                                        priority: 'strict'
                                    }
                                },
                                {
                                    value: '11183',
                                    attributes: {
                                        priority: 'all'
                                    }
                                }
                            ],
                            'found-docs': [
                                {
                                    value: '11221',
                                    attributes: {
                                        priority: 'phrase'
                                    }
                                },
                                {
                                    value: '11221',
                                    attributes: {
                                        priority: 'strict'
                                    }
                                },
                                {
                                    value: '11221',
                                    attributes: {
                                        priority: 'all'
                                    }
                                }
                            ],
                            'found-docs-human': 'нашёл 11 тыс. ответов',
                            page: {
                                value: '0',
                                attributes: {
                                    first: '1',
                                    last: '1'
                                }
                            },
                            group: {
                                categ: {
                                    attributes: {
                                        attr: 'ii',
                                        name: '8747087499842056476'
                                    }
                                },
                                doccount: '1',
                                relevance: '105626256',
                                doc: {
                                    attributes: {
                                        id: 'Z1080A03E049F16E1'
                                    },
                                    relevance: '105626256',
                                    url: 'https://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg',
                                    domain: 'cdn.tom-tailor.com',
                                    title: 'Marvin straight denim',
                                    modtime: '20180921T104930',
                                    size: '0',
                                    charset: 'utf-8',
                                    passages: {
                                        passage: {
                                            value: '  denim - Men - mid stone wash denim.',
                                            hlword: ['Marvin', 'straight']
                                        }
                                    },
                                    properties: {
                                        ImageSemDesc:
                                            'BJ8W4RCAoD4IADAC6QIznbNmkBbVl49pXyBpGtuLSFiwrnvXO5SDvqX3onl1V4ailwSHNZiKeXjIBUpJy2oaW7Ctmrg8hJNes62auCuDowA=',
                                        NNFeatures:
                                            'EsgBMyr0FEgACfLwCd3857xIEhf73ejtEA0ANTE33toXCCEqpCOoAA7yO0_JpTbF69O26MYy3fwoERAV7iDb2vf-3wcX0iLLEKZB8vXQt_PVCPXu2j4MKOjw-wPPOj0PrN3rqwn1DWwGBefyS_EJFPP14Qvo9S3H8gjr3unfvRAg4ukS2yjJuVAb7dMQJO7n-_PaIwvsKuX26BcEzwsgBUMK0PgAEMbc-8eqHgEDqBoECHcwFtrxI9A36VLEEJ401VAo6Lnu_MwM8PUayAEV_f_2_wL-A_r6-_YBAf7--gUE_P4EAvn6APQLCPgABf8BBwH9_wQC_AABdwAFAwb8BQj6_QICBAAEBAb9AQP-AAH8-fr6BgID-PgAAvoHF_r__PkB-P8EEAIH_ff8_fwC_v4FEAIGAAUAAP4A9wL6-zoG_AD5EAgAEgAGA_b7_wABDf4B7vrv8QAU_hMCAv77BPEQ7_0AEAP4CP8I9vIPBe4GAgEQ9gYD8QAD_fwA_w0OAAAc_wX9_QoB_evyUQL4_-3xDgD8AA,,',
                                        _ImagesDups:
                                            'i\t4726fb1759965c1b2508c78d4854c441\tc\t4512747636609425156\tw\t560\th\t745\ttw\t241\tth\t320\tu\thttps://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg\tuh\thttps://www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/\ts\t126480\tt\tjpg\tsmartcrop\t0x0+99x99\tsmartcropnoaspect\t21x0+64x99\tbq\t255\ttxt_img_sim\t0.7116\tdosid\t71\tdobbox\t29x7+47x76;56x75+14x16;37x80+19x15\ni\t4726fb1759965c1b2508c78d4854c441\tc\t4512747636609425156\tw\t560\th\t745\ttw\t241\tth\t320\tu\thttps://cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg\tuh\thttps://www.tom-tailor.de/marvin-straight-jeans-manner-62041830910_1052/\ts\t126480\tt\tjpg\tsmartcrop\t0x0+99x99\tsmartcropnoaspect\t21x0+64x99\tbq\t255\ttxt_img_sim\t0.7181\tdosid\t71\tdobbox\t29x7+47x76;56x75+14x16;37x80+19x15',
                                        _ImagesJson:
                                            '{"dc":"white","Passages":[{"qhits":"16","di":0,"lang":2,"title":"Marvin straight denim","text":"\\u0007[Marvin\\u0007] \\u0007[straight\\u0007] denim - Men - mid stone wash denim."}],"Preview":[{"di":0}]}',
                                        _Markers: [
                                            'SnipDebug=uil=ru;tld=ru;report=xml;exps=imgbuild;rsdups=0;pvdups=2;t=2;screenw=1366;fsframeh=768;frameh=563;framew=926;fsframew=1300;screenh=768;',
                                            'documentid=PqCAEOEWnwQAAAAAAAAAAA==',
                                            'SnipImageRanker=visquality_boost_ranker_viewportsize_1d5',
                                            'SnipTextRanker=page_relevance_ranker',
                                            'SnipTextFastRanker=images_stabilizer#corsa_based_ranker'
                                        ],
                                        _MetaSearcherHostname: [
                                            'sas1-7262.search.yandex.net:9200',
                                            'sas1-3327.search.yandex.net:9402',
                                            'sas1-5960.search.yandex.net:9080'
                                        ],
                                        _MimeType: '2 0&d=3978362&sh=-1&sg=',
                                        _SearcherHostname: 'sas1-6074.search.yandex.net:9300',
                                        _Shard: 'imgsidx-308-20181014-070347',
                                        documentid: 'PqCAEOEWnwQAAAAAAAAAAA==',
                                        imagetags:
                                            'marvin=27.7931;denim=20.6395;wash=19.7601;mid=19.4711;stone=18.0788;jeans=11.7169;man=4.25254'
                                    },
                                    'image-properties': {
                                        id: '4726fb1759965c1b2508c78d4854c441',
                                        gid: 'PqCAEOEWnwQ',
                                        shard: '0',
                                        'thumbnail-width': '112',
                                        'thumbnail-height': '150',
                                        'thumbnail-width-original': '241',
                                        'thumbnail-height-original': '320',
                                        'thumbnail-link':
                                            'http://im0-tub-ru.yandex.net/i?id=4726fb1759965c1b2508c78d4854c441',
                                        'original-width': '560',
                                        'original-height': '745',
                                        'html-link': 'www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/',
                                        'image-link': 'cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg',
                                        'file-size': '126480',
                                        'mime-type': 'jpg',
                                        dominated_color: 'white'
                                    },
                                    'mime-type': 'text/html',
                                    'highlight-cookie': '0&d=3978362&sh=-1&sg=',
                                    'image-duplicates': '',
                                    'image-duplicates-preview': {
                                        'image-properties': {
                                            id: '4726fb1759965c1b2508c78d4854c441',
                                            gid: '',
                                            shard: '0',
                                            'thumbnail-width': '112',
                                            'thumbnail-height': '150',
                                            'thumbnail-width-original': '241',
                                            'thumbnail-height-original': '320',
                                            'thumbnail-link':
                                                'http://im0-tub-ru.yandex.net/i?id=4726fb1759965c1b2508c78d4854c441',
                                            'original-width': '560',
                                            'original-height': '745',
                                            'html-link':
                                                'www.tom-tailor.eu/marvin-straight-denim-men-62041830910_1052/',
                                            'image-link': 'cdn.tom-tailor.com/img/560_745/62041830910_1052_1.jpg',
                                            'file-size': '126480',
                                            'mime-type': 'jpg'
                                        }
                                    },
                                    'image-duplicates-resized': ''
                                }
                            }
                        }
                    }
                }
            }
        };

        expect(parser.parse(response)).toEqual(result);
    });

    test('should correct parse error response', () => {
        const response = {
            yandexsearch: {
                $: { version: '1.0' },
                request: [
                    {
                        query: [''],
                        page: ['0'],
                        sortby: [{ _: 'rlv', $: { order: 'descending', priority: 'no' } }],
                        maxpassages: [''],
                        groupings: [
                            {
                                groupby: [
                                    {
                                        $: {
                                            attr: 'ii',
                                            mode: 'deep',
                                            'groups-on-page': '1',
                                            'docs-in-group': '1',
                                            curcateg: '-1'
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ],
                response: [
                    {
                        $: { date: '20181018T124639' },
                        error: [{ _: 'Задан пустой поисковый запрос', $: { code: '2' } }],
                        reqid: ['1539866799081876-1412951529657204181417836-sas1-7156-XML']
                    }
                ]
            }
        };
        const result = {
            yandexsearch: {
                attributes: { version: '1.0' },
                request: {
                    query: '',
                    page: '0',
                    sortby: {
                        value: 'rlv',
                        attributes: { order: 'descending', priority: 'no' }
                    },
                    maxpassages: '',
                    groupings: {
                        groupby: {
                            attributes: {
                                attr: 'ii',
                                mode: 'deep',
                                'groups-on-page': '1',
                                'docs-in-group': '1',
                                curcateg: '-1'
                            }
                        }
                    }
                },
                response: {
                    attributes: { date: '20181018T124639' },
                    error: { value: 'Задан пустой поисковый запрос', attributes: { code: '2' } },
                    reqid: '1539866799081876-1412951529657204181417836-sas1-7156-XML'
                }
            }
        };

        expect(parser.parse(response)).toEqual(result);
    });
});
