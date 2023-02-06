/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/categories\/[0-9]+\/search/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        page: {
            number: 1,
            count: 30,
            total: 4,
            totalItems: 93,
        },
        processingOptions: {
            adult: false,
            checkSpelled: true,
            highlightedText: '',
        },
        id: '1571780017395/e974cbaab0b51dbec4159a8d86950500',
        time: '2019-10-23T00:33:37.813+03:00',
        marketUrl: 'https://market.yandex.ru?pp=490&clid=2210590&distr_type=4',
    },
    items: [
        {
            __type: 'model',
            id: 193771144,
            name: 'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'магнитный держатель для автомобиля, место крепления: воздуховод, приборная панель, способ крепления: клеящаяся платформа, зажим, подходит для смартфонов, зарядное устройство',
            photo: {
                width: 607,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
                criteria: [
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 607,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 695,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1345185/img_id3466469756009512095.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 495,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id8390411745591861177.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 659,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1428687/img_id4488591916146764157.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 657,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1428687/img_id3527967112832167422.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 461,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id2823550852337499778.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 646,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1428687/img_id8139611686872795933.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 654,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1345185/img_id8007933552262932639.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 267,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id7728364416864803508.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 486,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id4881698568577618536.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 477,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id3944118209300951378.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 697,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1060343/img_id7910098885928830234.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 564,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1364191/img_id9016020088548699913.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '2199',
                min: '1234',
                avg: '1800',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 38,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 3,
                        percent: 100,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-s-besprovodnoi-zariadkoi-baseus-big-ears-car-mount-wireless-charger/193771144?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-s-besprovodnoi-zariadkoi-baseus-big-ears-car-mount-wireless-charger/193771144/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 28,
            opinionCount: 3,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZGluLpe8pAj4Lgj9oj5qPlb4WLzNY_N9m0N_yJtwi2GOA',
                wareMd5: 'tEPKhMo74gjOZzEhC0D-ag',
                skuType: 'market',
                name:
                    'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger черный (WXER-01)',
                description:
                    'Автомобильный держатель с быстрой беспроводной зарядкой Baseus Big Ears предлагает премиальное качество и дизайн. Магнитное крепление надежно удерживает смартфон, беспроводная технология QI обеспечивает быструю зарядку мобильного устройства, а возможность крепления на торпеду или в дефлектор обеспечивает непревзойденный уровень комфорта при использовании. Держатель совместим со всеми мобильными устройствами, поддерживающими стандарт QI (на некоторые модели необходим дополнительный ресивер).',
                price: {
                    value: '1234',
                },
                cpa: false,
                directUrl:
                    'https://lirider.ru/products/avtomobilnyi-derzhatel-dlya-smartfona-baseus-big-ears-magnetic-car-mount-s-besprovodnoi-zaryadkoi-iq',
                shop: {
                    organizations: [],
                    id: 386543,
                    outlets: [],
                },
                model: {
                    id: 193771144,
                },
                onStock: true,
                photo: {
                    width: 761,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/orig',
                },
                delivery: {
                    price: {
                        value: '285',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 285 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '285',
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 15,
                            },
                            brief: 'завтра при заказе до 15:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: true,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.007667689119,
                        NUMBER_OFFERS: 36,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.895498',
                        },
                    ],
                },
                photos: [
                    {
                        width: 761,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/orig',
                    },
                    {
                        width: 952,
                        height: 900,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_pTwP04iq0So_nOkvvBabbw/orig',
                    },
                    {
                        width: 877,
                        height: 900,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_WylPjGSAp6bcXfx_HqQV5g/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 200,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/200x200',
                    },
                    {
                        width: 190,
                        height: 179,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/167181/market_pTwP04iq0So_nOkvvBabbw/190x250',
                    },
                    {
                        width: 155,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/210846/market_WylPjGSAp6bcXfx_HqQV5g/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '1.20842',
                    },
                ],
            },
            showUid: '15717800175384431183316001',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-s-besprovodnoi-zariadkoi-baseus-big-ears-car-mount-wireless-charger/193771144/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 385807038,
            name: 'Держатель с беспроводной зарядкой Baseus Heukji Wireless Charger Gravity Car Mount',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: приборная панель, способ крепления: присоска, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 648,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id3500404070704761834.png/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 648,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id3500404070704761834.png/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 528,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id6417022709098916.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 628,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1669769/img_id3687808161269096427.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 693,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1620389/img_id4350279040802116134.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 660,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id7995901918914624525.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 685,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1522540/img_id130171452096452137.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 528,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1602935/img_id7241150254113141601.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 648,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id1581668819274873898.png/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 541,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1626700/img_id3245977300755127872.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 619,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1614201/img_id8495029382864863599.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 683,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1680954/img_id6290553669321367561.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 657,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id3741467233063140938.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 682,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id7251064264976836141.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 528,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id5260679137569551470.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 648,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1605421/img_id8750912626201188263.png/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 546,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8387192911356494157.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 625,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1522540/img_id5323230571534642110.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 684,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1605421/img_id4216718459689437248.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 658,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id8420937479123961005.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 683,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id5581202748474030708.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 522,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1602935/img_id6913892248052227686.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1890',
                min: '1139',
                avg: '1160',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 8,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 1,
                        percent: 100,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-heukji-wireless-charger-gravity-car-mount/385807038?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-heukji-wireless-charger-gravity-car-mount/385807038/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 15,
            opinionCount: 1,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZGCX26x8ZqWP3TrN--9ftK25YvUFibrfHLOVpKc5gQcmg',
                wareMd5: 'l0H2TvNw7XSpbTtw8i7WVw',
                sku: '100584261853',
                skuType: 'market',
                name: 'Держатель с беспроводной зарядкой Baseus Heukji Wireless Charger Gravity Car Mount черный',
                modelAwareTitle:
                    'Держатель с беспроводной зарядкой Baseus Heukji Wireless Charger Gravity Car Mount черный',
                description:
                    'Автодержатель с беспроводной зарядкой Baseus Wireless Charger Gravity Car Mount – незаменимый аксессуар для автомобиля. Крепится на торпеду при помощи присоски, а после снятия не оставляет никаких следов. Держатель имеет мягкие подкладки в местах соприкосновения с телефоном, поэтому не повреждает устройство. Аксессуар оснащён LED-индикатором. В комплекте — кабель microUSB.',
                price: {
                    value: '1490',
                },
                promocode: true,
                cpa: false,
                directUrl:
                    'https://beru.ru/product/100584261853?offerid=l0H2TvNw7XSpbTtw8i7WVw&utm_source=market&utm_medium=cpc&utm_term=641769.17110&utm_content=91502&clid=910',
                shop: {
                    organizations: [],
                    id: 431782,
                    outlets: [],
                },
                model: {
                    id: 385807038,
                },
                photo: {
                    width: 648,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id3500404070704761834.png/orig',
                },
                delivery: {
                    price: {
                        value: '249',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 249 руб., возможен самовывоз',
                    inStock: false,
                    global: false,
                    post: true,
                    postStats: {
                        minDays: 5,
                        maxDays: 7,
                        minDate: '2019-10-28',
                        maxDate: '2019-10-30',
                        minPrice: {
                            value: '149',
                        },
                        maxPrice: {
                            value: '149',
                        },
                    },
                    options: [
                        {
                            service: {
                                id: 179,
                            },
                            conditions: {
                                price: {
                                    value: '249',
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 18,
                            },
                            brief: 'завтра при заказе до 18:00',
                        },
                    ],
                    deliveryPartnerTypes: ['YANDEX_MARKET'],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: true,
                paymentOptions: {
                    canPayByCard: false,
                },
                promo: {
                    type: 'bundle',
                    description: 'autogenerated bundle #105321',
                    url:
                        'beru.ru/bundle/100584261853?schema=type,objId,count&data=offer,01e-0CSykLlPSxfFb5vlsg,1&data=offer,Oz5WHF45xr1woTogZ-1yxA,1',
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.06314510107,
                        NUMBER_OFFERS: 27,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.467269',
                        },
                    ],
                },
                spasibo: {
                    receive: {
                        points: 14,
                        percent: 1,
                    },
                },
                photos: [
                    {
                        width: 648,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id3500404070704761834.png/orig',
                    },
                    {
                        width: 528,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id6417022709098916.jpeg/orig',
                    },
                    {
                        width: 701,
                        height: 628,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1669769/img_id3687808161269096427.jpeg/orig',
                    },
                    {
                        width: 701,
                        height: 693,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1620389/img_id4350279040802116134.jpeg/orig',
                    },
                    {
                        width: 701,
                        height: 660,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id7995901918914624525.jpeg/orig',
                    },
                    {
                        width: 685,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1522540/img_id130171452096452137.jpeg/orig',
                    },
                    {
                        width: 528,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1602935/img_id7241150254113141601.jpeg/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 184,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id3500404070704761834.png/200x200',
                    },
                    {
                        width: 188,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id6417022709098916.jpeg/190x250',
                    },
                    {
                        width: 190,
                        height: 170,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1669769/img_id3687808161269096427.jpeg/190x250',
                    },
                    {
                        width: 190,
                        height: 187,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1620389/img_id4350279040802116134.jpeg/190x250',
                    },
                    {
                        width: 190,
                        height: 178,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id7995901918914624525.jpeg/190x250',
                    },
                    {
                        width: 156,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1522540/img_id130171452096452137.jpeg/120x160',
                    },
                    {
                        width: 188,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1602935/img_id7241150254113141601.jpeg/190x250',
                    },
                ],
                manufactCountries: [
                    {
                        id: 134,
                        name: 'Китай',
                        type: 'COUNTRY',
                        childCount: 33,
                        country: {
                            id: 134,
                            name: 'Китай',
                            type: 'COUNTRY',
                            childCount: 33,
                            nameAccusative: 'Китай',
                            nameGenitive: 'Китая',
                        },
                        nameAccusative: 'Китай',
                        nameGenitive: 'Китая',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.906906',
                    },
                ],
            },
            showUid: '15717800175384431183316002',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-heukji-wireless-charger-gravity-car-mount/385807038/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 242788342,
            name: 'Держатель с беспроводной зарядкой Baseus Smart Vehicle Bracket Wireless Charger (WXZN-01)',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: воздуховод, способ крепления: зажим, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 633,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id374055427722199225.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 633,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id374055427722199225.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 680,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1353698/img_id2808054363486881667.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 544,
                    height: 500,
                    url: 'https://avatars.mds.yandex.net/get-mpic/933699/img_id2618681093094667286.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 522,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1353698/img_id2268634539250442848.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 619,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1042102/img_id2377917567949470552.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 687,
                    url: 'https://avatars.mds.yandex.net/get-mpic/931379/img_id6816074629958004249.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 407,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1045304/img_id1225370747768420857.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 407,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id1863886057166379716.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 402,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1353698/img_id9153339857986016063.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '2026',
                min: '1120',
                avg: '1799',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 27,
                distribution: [
                    {
                        value: 1,
                        count: 1,
                        percent: 17,
                    },
                    {
                        value: 2,
                        count: 3,
                        percent: 50,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 2,
                        percent: 33,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-01/242788342?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-01/242788342/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 26,
            opinionCount: 6,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZFdn8ebb7UqKhablbidEXHaHsIo4ZG6aV2iZP5U3vEdXQ',
                wareMd5: '7pskMDsFSfejEEDGaT1HzQ',
                skuType: 'market',
                name:
                    'Автомобильный держатель смартфона с беспроводной зарядкой Baseus Smart Vehicle Bracket Wireless Charger черный (WXZN-01)',
                description:
                    'Baseus Smart Vehicle Bracket - автомобильный держатель для смартфонов, устанавливаемый в дефлектор. Он не только надежно фиксирует смартфон, но и позволяет быстро его зарядить за счет поддержки технологии беспроводной зарядки QI. Этот аксессуар совместим с мобильными аппаратами, имеющие встроенную функцию беспроводной зарядки. Для ряда телефонов Baseus Smart Vehicle Bracket имеет возможность ускоренной зарядки, обеспечивающей на 40% большую скорость заряда.',
                price: {
                    value: '1120',
                    discount: '10',
                    base: '1239',
                },
                cpa: false,
                directUrl:
                    'https://lirider.ru/products/avtomobilnyi-derzhatel-dlya-telefona-v-deflektor-s-besprovodnoi-bystroi-zaryadkoi-baseus-smart-vehicle-bracket-wxzn-01',
                shop: {
                    organizations: [],
                    id: 386543,
                    outlets: [],
                },
                model: {
                    id: 242788342,
                },
                onStock: true,
                photo: {
                    width: 640,
                    height: 640,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1574355/market_azNYK4ZswhWl9zz4T7XeIA/orig',
                },
                delivery: {
                    price: {
                        value: '285',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 285 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '285',
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 15,
                            },
                            brief: 'завтра при заказе до 15:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: true,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.007667689119,
                        NUMBER_OFFERS: 39,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.907045',
                        },
                    ],
                },
                photos: [
                    {
                        width: 640,
                        height: 640,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1574355/market_azNYK4ZswhWl9zz4T7XeIA/orig',
                    },
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1548377/market_ZiTM4-tRJ5Y5P5uh3r2QWg/orig',
                    },
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/236212/market_bonrRVjz7bs4ZDmPe6q6gg/orig',
                    },
                    {
                        width: 593,
                        height: 593,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_7OIQ1aopgnUIPbhMfphO7A/orig',
                    },
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202108/market_sx98AIzrWj0uihKKLH3GRg/orig',
                    },
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/246300/market_sfEH05UJKi5upoSPG7YCZA/orig',
                    },
                    {
                        width: 650,
                        height: 650,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1339901/market_mWrRJZzNIZZYkGu7OKj5aA/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1574355/market_azNYK4ZswhWl9zz4T7XeIA/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1548377/market_ZiTM4-tRJ5Y5P5uh3r2QWg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/236212/market_bonrRVjz7bs4ZDmPe6q6gg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/235963/market_7OIQ1aopgnUIPbhMfphO7A/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/202108/market_sx98AIzrWj0uihKKLH3GRg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/246300/market_sfEH05UJKi5upoSPG7YCZA/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1339901/market_mWrRJZzNIZZYkGu7OKj5aA/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.892884',
                    },
                ],
            },
            showUid: '15717800175384431183316003',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-01/242788342/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 339963360,
            name: 'Держатель с беспроводной зарядкой Baseus Wireless Charger Gravity Car Mount 2 (WXYL-A01)',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: приборная панель, способ крепления: присоска, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 674,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8127360274057087409.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 674,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8127360274057087409.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 608,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id4846577333951272881.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 683,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1538707/img_id3184252749319024099.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 618,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id1498317255291617056.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 652,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1614201/img_id2442442925302365169.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 616,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id6495055336206537665.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 533,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1808939/img_id4927531876792861444.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '2190',
                min: '1082',
                avg: '1390',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 14,
                distribution: [
                    {
                        value: 1,
                        count: 1,
                        percent: 100,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount-2-wxyl-a01/339963360?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount-2-wxyl-a01/339963360/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 26,
            opinionCount: 1,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZFRruyf9VGF-1O2FfwyF_7ZxjtQMdUjl8RB7LD1tRtEnw',
                wareMd5: 'qBKLEk9bMhv7JklFsM35lA',
                skuType: 'market',
                name: 'Автомобильный держатель Baseus Wireless Charger Gravity Car Mount (osculum type) Черный',
                description:
                    'держатель для автомобиля подходит для смартфонов место крепления: приборная панель способ крепления: присоска диагональ устройства: от 4" до 6.5" зарядное устройство в комплекте беспроводная зарядка Qi',
                price: {
                    value: '1090',
                },
                cpa: false,
                directUrl:
                    'https://miwi.ru/products/avtomobilnyj-derzhatel-baseus-wireless-charger-gravity-car-mount-osculum-type',
                shop: {
                    organizations: [],
                    id: 577766,
                    outlets: [],
                },
                model: {
                    id: 339963360,
                },
                onStock: true,
                phone: {
                    number: '+7 9039607757',
                    sanitized: '+79039607757',
                },
                photo: {
                    width: 674,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8127360274057087409.jpeg/orig',
                },
                delivery: {
                    price: {
                        value: '300',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '300',
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 13,
                            },
                            brief: 'сегодня при заказе до 13:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.005789325107,
                        NUMBER_OFFERS: 30,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.65615',
                        },
                    ],
                },
                photos: [
                    {
                        width: 674,
                        height: 701,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8127360274057087409.jpeg/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1720090/market_6IGOrEMxnXiHFQcwuLwwjg/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1720090/market_wlNrBfOchR2JRgRUjYgICw/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_5aeXZ7HwM6bNr_3ZwA0qyQ/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1077406/market_zI1WeIU3HGKl2LuV-XhIJQ/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_Kic2c4CLpnrd7rG6_GbsNQ/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/330747/market_-4Rw41vd926or1625gqJzA/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1595377/market_n8flT6GUamy-gadOhrTkhw/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_0Lz4Vibz2RjJCPi4a9G2QA/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 153,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8127360274057087409.jpeg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1720090/market_6IGOrEMxnXiHFQcwuLwwjg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1720090/market_wlNrBfOchR2JRgRUjYgICw/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/249073/market_5aeXZ7HwM6bNr_3ZwA0qyQ/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1077406/market_zI1WeIU3HGKl2LuV-XhIJQ/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/231668/market_Kic2c4CLpnrd7rG6_GbsNQ/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/330747/market_-4Rw41vd926or1625gqJzA/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1595377/market_n8flT6GUamy-gadOhrTkhw/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/176166/market_0Lz4Vibz2RjJCPi4a9G2QA/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.872313',
                    },
                ],
            },
            showUid: '15717800175384431183316004',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount-2-wxyl-a01/339963360/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 193771080,
            name: 'Держатель с беспроводной зарядкой Baseus Wireless Charger Gravity Car Mount',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: воздуховод, способ крепления: зажим, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 616,
                height: 665,
                url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id166334057203560319.png/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 616,
                    height: 665,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id166334057203560319.png/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 507,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1382936/img_id1695727095850405191.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 637,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1478677/img_id3721736584143897504.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 660,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1478677/img_id7879522469989590744.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 561,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1428687/img_id2710217863928631426.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 632,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id8307923132733535961.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 611,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id3277613970666306703.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 491,
                    height: 588,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1428687/img_id3818704385300360872.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 628,
                    height: 628,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id3955166926794706791.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 617,
                    height: 668,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1680954/img_id3250869905724748808.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '2190',
                min: '1099',
                avg: '1250',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4,
                count: 23,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 2,
                        percent: 50,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 2,
                        percent: 50,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount/193771080?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount/193771080/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 43,
            opinionCount: 4,
            reviewCount: 1,
            offer: {
                id: 'yDpJekrrgZHbUBtP_tPqXS1lLXnKODOc8qOrnkk34cUbE80E0XyWsQ',
                wareMd5: '4nrf4Q2S_uFBWV15ggi-sQ',
                skuType: 'market',
                name: 'Автодержатель с беспроводной зарядкой Baseus Wireless Charger Gravity Car Mount Red (Красный)',
                description:
                    'Baseus Wireless Charger Gravity Car Mount представляет собой беспроводное зарядное устройство, которое крепится в дефлекторе автомобиля, а также служит в качестве держателя. Модель надежно фиксирует смартфон с трех сторон благодаря регулируемым креплениям, исключая выпадение гаджета.Baseus Wireless Charger Gravity Car Mount имеет компактные размеры, при этом обладает продуманной конструкцией. Регулируемые крепления позволяют закрепить любой смартфон и надежно зафиксировать его.',
                price: {
                    value: '1099',
                },
                cpa: false,
                directUrl:
                    'http://rynek.ru/products/avtoderzhatel-s-besprovodnoj-zaryadkoj-baseus-wireless-charger-gravity-car-mount?variant=5093',
                shop: {
                    organizations: [],
                    id: 110677,
                    outlets: [],
                },
                model: {
                    id: 193771080,
                },
                onStock: true,
                photo: {
                    width: 616,
                    height: 665,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id166334057203560319.png/orig',
                },
                delivery: {
                    price: {
                        value: '299',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 299 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '299',
                                },
                                daysFrom: 0,
                                daysTo: 1,
                                orderBefore: 12,
                            },
                            brief: 'до&nbsp;завтра при заказе до 12:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.006123124622,
                        NUMBER_OFFERS: 76,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.481546',
                        },
                    ],
                },
                photos: [
                    {
                        width: 616,
                        height: 665,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id166334057203560319.png/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 185,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id166334057203560319.png/200x200',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.862049',
                    },
                ],
            },
            showUid: '15717800175384431183316005',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-wireless-charger-gravity-car-mount/193771080/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 394273009,
            name: 'Держатель с беспроводной зарядкой Baseus Smart Vehicle Bracket Wireless Charger (WXZN-B01)',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: панель/лобовое стекло, способ крепления: присоска, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 701,
                height: 512,
                url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id6958982893644685706.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 701,
                    height: 512,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id6958982893644685706.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 500,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1602935/img_id1511601681521243566.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 500,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id901070521913320575.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 523,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1749547/img_id3093919596507523382.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 575,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1538707/img_id5579279512342718544.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 663,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1644362/img_id5270809249589025324.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 440,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id2079855093099672452.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 649,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1567763/img_id1671530078103283219.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 695,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id2113421689775659784.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 353,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1538707/img_id818294288612720382.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 466,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1642819/img_id1097844551828752005.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 462,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1525215/img_id6877317064013756198.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '2190',
                min: '1295',
                avg: '1899',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 8,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 1,
                        percent: 33,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 2,
                        percent: 67,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-b01/394273009?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-b01/394273009/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 35,
            opinionCount: 3,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZGgVgPsb038v7oT-e945II0evHGsuQz4_aKS65d0_jzZw',
                wareMd5: 'KeWiZl9fQs4ADWTjfffw9g',
                skuType: 'market',
                name:
                    'Держатель с беспроводной зарядкой Baseus Smart Vehicle Bracket Wireless Charger (WXZN-B01) черный',
                description: '',
                price: {
                    value: '1299',
                },
                cpa: false,
                directUrl:
                    'https://miwi.ru/products/derzhatel-s-besprovodnoj-zaryadkoj-baseus-smart-vehicle-bracket-wireless-charger-wxzn-b01-chernyj',
                shop: {
                    organizations: [],
                    id: 577766,
                    outlets: [],
                },
                model: {
                    id: 394273009,
                },
                onStock: true,
                phone: {
                    number: '+7 9039607757',
                    sanitized: '+79039607757',
                },
                photo: {
                    width: 701,
                    height: 512,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id6958982893644685706.jpeg/orig',
                },
                delivery: {
                    price: {
                        value: '300',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '300',
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 13,
                            },
                            brief: 'сегодня при заказе до 13:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.005789325107,
                        NUMBER_OFFERS: 42,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.915541',
                        },
                    ],
                },
                photos: [
                    {
                        width: 701,
                        height: 512,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id6958982893644685706.jpeg/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/480326/market_kioQGt95zPtV8nQOBMTYsQ/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1063985/market_mMrj-Saus867cJS4U7OXDQ/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168559/market_9cOreRO3y4HMo-Ekco9NOw/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market_Q40YnY421lz5wL28EBPVgA/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1654295/market_i39JK_apUn_8XlSam9182w/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1703792/market_pvGS5Ji8oVbdLTK1p6mXNg/orig',
                    },
                    {
                        width: 200,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1781446/market_Ef8J8ZTcOBZ5XaZZkYQtBQ/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 138,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id6958982893644685706.jpeg/190x250',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/480326/market_kioQGt95zPtV8nQOBMTYsQ/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1063985/market_mMrj-Saus867cJS4U7OXDQ/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/168559/market_9cOreRO3y4HMo-Ekco9NOw/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/241976/market_Q40YnY421lz5wL28EBPVgA/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1654295/market_i39JK_apUn_8XlSam9182w/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1703792/market_pvGS5Ji8oVbdLTK1p6mXNg/120x160',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1781446/market_Ef8J8ZTcOBZ5XaZZkYQtBQ/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.857047',
                    },
                ],
            },
            showUid: '15717800175384431183316006',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-smart-vehicle-bracket-wireless-charger-wxzn-b01/394273009/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'model',
            id: 193773210,
            name: 'Магнитный держатель Baseus Small Ears Series Magnetic Suction Bracket (Air outlet type)',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'магнитный держатель для автомобиля, место крепления: воздуховод, способ крепления: зажим, подходит для смартфонов',
            photo: {
                width: 562,
                height: 629,
                url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id309545979529107457.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 562,
                    height: 629,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id309545979529107457.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 618,
                    height: 569,
                    url: 'https://avatars.mds.yandex.net/get-mpic/931379/img_id5756849331961112154.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 448,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id1342665948453743540.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 637,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id6569432808130333607.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 619,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id18093946237240299.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 551,
                    height: 636,
                    url: 'https://avatars.mds.yandex.net/get-mpic/986077/img_id1569815939984062117.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 360,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1081556/img_id7621955270716293343.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 508,
                    height: 562,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id625686006542501171.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 436,
                    height: 404,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id810105414462121692.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 438,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1081556/img_id288657309575789483.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 632,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id75826250325323992.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 612,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/986077/img_id7603480614066235054.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1571888/img_id7987190187301313107.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 456,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id779874516701004499.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891866',
                        },
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 468,
                    height: 520,
                    url: 'https://avatars.mds.yandex.net/get-mpic/931379/img_id1725965295288116296.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 402,
                    height: 375,
                    url: 'https://avatars.mds.yandex.net/get-mpic/931379/img_id3200357241548323168.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 315,
                    height: 487,
                    url: 'https://avatars.mds.yandex.net/get-mpic/931379/img_id7376506248462486682.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 468,
                    height: 504,
                    url: 'https://avatars.mds.yandex.net/get-mpic/933699/img_id2558854973877446227.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 424,
                    height: 472,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id8315013908009419535.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 362,
                    height: 405,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id7536610073649584787.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 524,
                    height: 524,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1042102/img_id3066377463466628447.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 525,
                    height: 524,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360806/img_id1221209150711316114.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 524,
                    height: 524,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id5570988780422292152.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891828',
                        },
                        {
                            id: '14871214',
                            value: '15266392',
                        },
                    ],
                },
                {
                    width: 467,
                    height: 519,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1045304/img_id5657217957963108796.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 399,
                    height: 364,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id4433250632313343802.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 311,
                    height: 480,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id4183056625656030060.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 463,
                    height: 502,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id3242374684421347084.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 423,
                    height: 472,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id5652550422586468878.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 355,
                    height: 405,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1045304/img_id3414251722259922417.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 524,
                    height: 524,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1045304/img_id4174039885310211998.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 485,
                    height: 520,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1525355/img_id224612752243029110.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891903',
                        },
                        {
                            id: '14871214',
                            value: '14897438',
                        },
                    ],
                },
                {
                    width: 390,
                    height: 341,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1417902/img_id5360759060590480217.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13891903',
                        },
                        {
                            id: '14871214',
                            value: '14897438',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1490',
                min: '1190',
                avg: '1190',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 23,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 1,
                        percent: 100,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket-air-outlet-type/193773210?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket-air-outlet-type/193773210/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 11,
            opinionCount: 1,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZEsBZ7tYax04U5kJfE76pDS5z6aRyGajOXgqTRbggyAOg',
                wareMd5: 'xTJT-rGXXjOwzGU2ZfgKiA',
                skuType: 'market',
                name: 'Держатель Baseus Small ears series Magnetic suction bracket (Air outlet type) красный',
                description:
                    'Baseus Small Ear Series Magnetic Suction Bracket представляет собой автомобильный магнитный держатель, который вставляется в дефлектор при помощи специального крепления в виде прищепки. Держатель совместим с телефонами, навигаторами, плеерами и другими устройствами, диагональ которых не более 5". Baseus Small Ear Series Magnetic Suction Bracket изготовлен из тугоплавкого алюминия и имеет классический дизайн. Такой держатель идеально впишется в интерьер любого автомобиля.',
                price: {
                    value: '1400',
                },
                cpa: false,
                directUrl:
                    'https://bestbatt.ru/products/derzhatel-baseus-small-ears-series-magnetic-suction-bracket-air-outlet-type-krasnyi',
                shop: {
                    organizations: [],
                    id: 346256,
                    outlets: [],
                },
                model: {
                    id: 193773210,
                },
                photo: {
                    width: 450,
                    height: 475,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1451847/market_hzbrzor9wucfxsrv3NaQCA/orig',
                },
                delivery: {
                    price: {
                        value: '350',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: false,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 350 руб.',
                    inStock: false,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '350',
                                },
                                daysFrom: 1,
                                daysTo: 5,
                            },
                            brief: '1-5 дней',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.00601985259,
                        NUMBER_OFFERS: 97,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.220498',
                        },
                    ],
                },
                photos: [
                    {
                        width: 450,
                        height: 475,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1451847/market_hzbrzor9wucfxsrv3NaQCA/orig',
                    },
                    {
                        width: 500,
                        height: 500,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1855358/market_7NVMSXE3_fQJbFcTWqDEeQ/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 189,
                        height: 200,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1451847/market_hzbrzor9wucfxsrv3NaQCA/200x200',
                    },
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1855358/market_7NVMSXE3_fQJbFcTWqDEeQ/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.773562',
                    },
                ],
            },
            showUid: '15717800175384431183316007',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket-air-outlet-type/193773210/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZGH26yDAYRwDhE9hCUv2wkx_GuY4kj6bzqL69hsa6SNVA',
            wareMd5: 'Q5JY-sa6ytVdq63rM5hgzA',
            skuType: 'market',
            name: 'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger',
            description:
                'Место крепления: воздуховод, приборная панель Способ крепления: клеящаяся платформа, зажим Комплектация: Держатель Автомобильное зарядное устройство Кабель micro-USB Магнитная пластинка',
            price: {
                value: '1689',
                discount: '11',
                base: '1899',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpU5KGO7e4glTTM1zkj5EmHiQMm6DLZ5b4GXkLBHnRGeerFn2DTavPI2OKpJQyELTjXGcbeb0cj4fD9EOOwqJBkV-boZq_Noba4VDEDG1tKoihMskdAZ28nJ1N2ZGDGK4xxzaUmanJ7GS7_3tHiamqtR9djkGI5XYko_M3Ik1qzxI1D-64QMQoopTjVLOE9IjidIBV1hIW1kH4E0ydefli8XauShafFUw6RRvDboN--HZGY8DA-Xt4NpXH5YWcBljTUz1cDzJwdunG3Pncug2rGuUhnynSr8jMFehG3A9OJz8E71LTX9yjm3KSbfM9sTYa_B5HGBvBASjFj2jYKAfQz3vA1krv9JgyKUGjvRjWqNWM6o_RSGy-39VzNAaEOD_I8Jcn69eDu0dCfXowNNQjycJtHV5dn_HbwMTF9Z5dfW5uu4FmMuQ8L24vPoFtM3vcSJXZiuI1-SEzvP4cYeTk_KKRKSA_cQvcHPYHbpOXR51JyIT80OT6_pRW5NBANyT-JrmrhHS05Hy9OwOw0T9lt34LhTQtqlYeuy3ywzCTVMIaKft45lnIMCu1ERfOsHUOguFy4ljYnuvDl1Rl26gSoCGV9dlwFC-dXpyRILp90rYDlFwbInDsfWdI00xWHZUj-bQ1M9rbW8Q8wjQgnLeowECIG-MrhSJmfBr__PED_hzN46ejZHHWUu3SVQrZ3VLO83YaVu4KYLZpDiTlCEh8rKMWj0stK6_9ngN4qk6WUPMGTdLDUoU8BCn7XZ07JR46M0VXJMMAFMoHbdRhE9gyPdWQLJp8zPl6-PpuOjcVVVtcByvD1H8FZm0txvw3Jqqrpn4IE708iBZX6agBKPdgPP_HRfNniel-XNIfO_Vc9KcBofYKQ6S7fe-KteNZd-J7d6MmXOJ37-qGfpO1X7iScSKG-YnnhUVs2mlxMyvas9CchXGn3T_qwLMJ8D_PEqfCp-ioi2uwSIJDC6TtISR_hQe9BS8hjGMcMTAbt0ZbQd?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUhHIWnX1juHym29It1a7K-o-fVeaiVwVL7RXz-X-dM1tK2TzV8SAYzJ5KVenj1hJABcQ6Oz6teS7zLmy6Coqea-oVtFWqVbNFu374ROwWtMCjRCILHIRrsEjwnQoxBZukUtduD34UlJIcaAxRzZD4BUfeNPpv6tvB3O9C4snIFPLCeGr67zlr15zF_ETN1jk_JF8nv5rFzjl2WKbjHJchTvIXGXjSTUjGwicglA1sUPjK9e2EJu5BeuwKND9xBhP5Ilv86teCoH-4jhLoR7dx1xznh4IFB--oVzEW3q0qJY-LIjS8YxrtV-WwD6UFW8d3cAqtL73W1jYr7wwoVeLEKtlOwPVE3c44Zo16LYRmXXYCo_RTw1UD-0,&b64e=1&sign=2d997eea8b19478be8f8e76a6bc69d84&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpU5KGO7e4glTTM1zkj5EmHiQMm6DLZ5b4GXkLBHnRGeerFn2DTavPI2OKpJQyELTjXGcbeb0cj4fD9EOOwqJBkV-boZq_Noba4VDEDG1tKoihMskdAZ28nJ1N2ZGDGK4xxzaUmanJ7GS7_3tHiamqtR9djkGI5XYko_M3Ik1qzxI1D-64QMQoopTjVLOE9IjidIBV1hIW1kH4E0ydefli8XauShafFUw6RRvDboN--HZGY8DA-Xt4NpXH5YWcBljTUz1cDzJwdunG3Pncug2rGuUhnynSr8jMFehG3A9OJz8E71LTX9yjm3KSbfM9sTYa_B5HGBvBASjFj2jYKAfQz3vA1krv9JgyKUGjvRjWqNWM6o_RSGy-39VzNAaEOD_I8Jcn69eDu0dCfXowNNQjycJtHV5dn_HbwMTF9Z5dfW5uu4FmMuQ8L24vPoFtM3vcSJXZiuI1-SEzvP4cYeTk_KKRKSA_cQvcHPYHbpOXR51JyIT80OT6_pRW5NBANyT-JrmrhHS05Hy9OwOw0T9lt34LhTQtqlYeuy3ywzCTVMIaKft45lnIMCu1ERfOsHUOguFy4ljYnuvDl1Rl26gSoCGV9dlwFC-dXpyRILp90rYDlFwbInDsfWdI00xWHZUj-bQ1M9rbW8Q8wjQgnLeowECIG-MrhSJmfBr__PED_hzN46ejZHHWUu3SVQrZ3VLO83YaVu4KYLZpDiTlCEh8rKMWj0stK6_9ngN4qk6WUPMGTdLDUoU8BCn7XZ07JR46M0VXJMMAFMoHbdRhE9gyPdWQLJp8zPl6-PpuOjcVVVtcByvD1H8FZm0txvw3Jqqrpn4IE708iBZX6agBKPdgPP_HRfNniel-XNIfO_Vc9KcBofYKQ6S7fe-KteNZd-J7d6MmXOJ37-qGfpO1X7iScSKG-YnnhUVs2mlxMyvas9CchXGn3T_qwLMJ8D_PEqfCp-ioi2uwSIJDC6TtISR_hQe9BS8hjGMcMTAbt0ZbQd?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUhHIWnX1juHym29It1a7K-o-fVeaiVwVL7RXz-X-dM1tK2TzV8SAYzJ5KVenj1hJABcQ6Oz6teS7zLmy6Coqea-oVtFWqVbNFu374ROwWtMCjRCILHIRrsEjwnQoxBZukUtduD34UlJIcaAxRzZD4BUfeNPpv6tvB3O9C4snIFPLCeGr67zlr15zF_ETN1jk_JF8nv5rFzjl2WKbjHJchTvIXGXjSTUjGwicglA1sUPjK9e2EJu5BeuwKND9xBhP5Ilv86teCoH-4jhLoR7dx1xznh4IFB--oVzEW3q0qJY-LIjS8YxrtV-WwD6UFW8d3cAqtL73W1jYr7wwoVeLEKtlOwPVE3c44Zo16LYRmXXYCo_RTw1UD-0,&b64e=1&sign=2d997eea8b19478be8f8e76a6bc69d84&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpU5KGO7e4glTTM1zkj5EmHiQMm6DLZ5b4GXkLBHnRGeerFn2DTavPI2OKpJQyELTjXGcbeb0cj4fD9EOOwqJBkV-boZq_Noba4VDEDG1tKoihMskdAZ28nJ1N2ZGDGK4xxzaUmanJ7GS7_3tHiamqtR9djkGI5XYko_M3Ik1qzxI1D-64QMQoopTjVLOE9IjidIBV1hIW1kH4E0ydefli8XauShafFUw6RRvDboN--HZGY8DA-Xt4NpXH5YWcBljTUz1cDzJwduiTRMYkCvkjX1dztYb_phFLDqADoxLK67XJedf_2Nw0fGMDKJCFxN09Mvu1TmgJFBgtmQeWCy4vpsXob5UQcxZq_OFhtJkTz_HOWEnfPWKunC-8Wh9HB1_rSjw3yFhxaEru33zM1p5GHGWTvR0TfFLswqYVC6jnxVNk_yxnxneFE-D3XwUqxraWNe6UKK_0RXbQGG9Iys69kmUVlTm1PPFRbaQitv0T0ejTo57iypiGBvgFByBcVsqb4XfnVY7-0qTywgk8N4fNVVTnzHlDHJxd0mlfb9fNF2jAHIYPRlh-O8IDD7Q6UNN0mg018Cqi6SBAve5hUGrJR15t7kcjMU8aqsPrZI8A5YWmPGq0zkvfa5EE5_5Nnm25MGgkNZw5dZtT2hjyrdqune95GM6ouJF7jco3HgyK_moNYx9Ef0FFJ0kdE-NpDwBRbTJS_fn_50puDuj_cOyMH4qcm4kRkUTJYvilk9mUGzonGO4QOMCp41tOIT_biae9DcFm63kLy7V2TJUSGamGrFICK9nWNMjmi0UjHMBy00ZCvz1W0W8A-SEBEmgi4WBobRKFYhz76Q9aEUAdsiy5EkELKU6eRPKr4CKSGjuPUQgAg95NbCBnJkk34j9FQ2kPy3MN9-pPhHA8ftElHlYFmEx62apFcPQAay0rpDhuKkcMUbaiSrRxlBVvznjnhsrL7tGE8fzpdiqZo8lcTH-uf-LTgFfGG4yofq3QEukgtf?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUhHIWnX1juHym29It1a7K-o-fVeaiVwVL7RXz-X-dM1tK2TzV8SAYzJ5KVenj1hJABcQ6Oz6teS7zLmy6Coqea-oVtFWqVbNFu374ROwWtMCjRCILHIRrsEjwnQoxBZukUtduD34UlJIcaAxRzZD4BUfeNPpv6tvB3O9C4snIFPLCeGr67zlr15zF_ETN1jk_JF8nv5rFzjl2WKbjHJchTvIXGXjSTUjGwicglA1sUPjK9e2EJu5BeuwKND9xBhP5Ilv86teCoH-4jhLoR7dx1xznh4IFB--oVzEW3q0qJY-LIjS8YxrtV-WwD6UFW8d3cAqtL73W1jYr7wwoVeLEKtlOwPVE3c44Zo16LYRmXXYCo_RTw1UD-0,&b64e=1&sign=e3793e6e9503b076a853c36227cae3d1&keyno=1',
            },
            directUrl:
                'https://elementx.shop/product/magnitnyy-derzhatel-s-besprovodnoy-zaryadkoy-baseus-big-ears-car-mount-wireless-charger?utm_source=yandex_market&utm_term=%7Bproduct.id%7D',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcK5zjHOOqcdGP-4T2WsR9R7uHhTtw4ufESXCnmzFs4IxIPEtuGI-R6xOjoKrdfX9ZEhD_CyCUs4WNNPOfn7JGtXO6iQ-pKAXu_pqx7UTbePeDgO3FJWCG2fZzH9LrCDOqMqAxJc76k3o7eRG935HL0sjLRIfyd7wDCphnRbZKuBPwYkP_-kcwJQeO24CI-U5rWdSW8U6-yMg5piJ5kaW8C-Z87W1jQc3Gq_oMksdR7bWYTOSd4YeZvjCRx0aM_WqjqoS0JCtFbCqnhAtMSZ_WtJ4uRSst5mZ2vdc_XRUdKD1Vn40Vy23bFSgOw6jJJH-GT-oCVWWW8Nyj-6INmuCuSAdFRySj_zDCB2PbH-koKFYsD38WR4v0DO6APJFileaOsB2OYvi6Mtj5O6GR4Q9ClCssxYb3GWyEvuJRWFJk5iL15WgoSh7PYTLib5-vnb1i_HfXDzh2bapoH9JuBFAvGsCdTn3lofBiKACpObN_6I4eAKshpomvlP8SAAzpdOWsGHpZWhaZCLawN8mXK08p0i-r1OalHuZtqCeP4joAY0zTyFLySKL4cK-H3M8rCfHFHZF3MCou0CSNCa-9rPwmazr-EhvYeLF1GyncZfH_HuGQvFAHaNL5OFr_Cu5fbFVIqD0e1CjpwivkcKt0T_vTQkvJAqp4og6cVUS-FQM7Kz4M1MyVG86GUAuM1t7zDvSMQwZfPodRVWNM0GjeUfOGIcCfm_UCMJX0vwmw1FIlm_GQDzbABRVcepJp4qtq25X9K8DVGZehW5XpTT2G39qJ4tPkOV1ZI50BHwPErV4dvw3oQ1E3av8HV_JqKaNbvLqr_faG_uIQVt33uoAPjRglmdTdpioN1PfHQF4ooTf_CDqB3oo_i6EPHICnN84Y1ElWVIRaPkBQCIK7E5I6-EdM1Iotr-1q6zV6-Is238qn5a2dsM8759ejF7VHZuRzAlWh2PTJ6D8TzQYNjjMuWt_mKLIuhlI-DeZzMOn8Y6PD-FH?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjUlBkVRWtEB63POvuEokOQROmZHEsYdJ0o8REzQP9UerlrkW3eHptHjbXYV2BbuTYnlbNcChwG4LxRdHpyOXGvY,&b64e=1&sign=cd67ca44438c88b00886d0ed0020ab9b&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcK5zjHOOqcdGP-4T2WsR9R7uHhTtw4ufESXCnmzFs4IxIPEtuGI-R6xOjoKrdfX9ZEhD_CyCUs4WNNPOfn7JGtXO6iQ-pKAXu_pqx7UTbePeDgO3FJWCG2fZzH9LrCDOqMqAxJc76k3o7eRG935HL0sjLRIfyd7wDCphnRbZKuBPwYkP_-kcwJQeO24CI-U5rWdSW8U6-yMgYiRmFgl8O0KDXLI6vfxttf7uN64Z4DXwjykV7s5sZtQ7Lh8zD-rEtaQs0Xbi-OvD5e-0G3fGEuHhSaurHXNqKsUCsFoEe4tq04oqhby54ssLXK2far-jJPv1rU_k976S695WCxssVCkezKjp-8vkVGt-EQ4p0qzG4rFB95C-j9OLqxlSDX9j9QkW3CBzLe8WyJe0NlZDXNZ4cbmDTLlxR5eIfEToZ662-wF5ZxN6PRKQWQiv5jqixY83Rzks6cW7HjhXsBo4nPBA581L_BS3UEX4WhNd6h2W10cHMMwETSdgtPkYTw1zboHGLgF1lPX15iCLxCfJ_p-mX9akdIHkaviCHS_NgxL5RsRdjwtTpj9VCmpPD99_U0S7oFaNFkGlz_uw4_6WOecw0c1BH0TA3wewEe4ITeRTKFEceS2tDTG2-J8LeuzngYl2oHhHa2ZsepZmKleXsu5BF6jzBpFfbhCoegXP10hkb7uuURnvcA2tm7izPHOuaBC2Yq5g_-Mit-HdWVaIVqY3xiwyolBwJRgxzY6Pkyn6Yjzm8SsFzVzENeDODFJebUttX474qIQRqkCPqXBQken-mXyASjb7PfS2Ua24a0TefGQXluiTCF-nRth4_rv9_gJoEF9WpgrvjmIrUz4PBzdiilVbsRiPkvYmyT7v6lTjEov2PBPlP7-OZA90d2ehWgo9PaIpv_eT9J0s5qP-ULBmekNidRiZ-Pr6i37Z8tCeNwMPJ07lgmspeF10rz7yrJF9K3_6CjoYJbNt7kkHnJ7V409Sa87dxLVOVv66PkHH?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjXzjs1c_itgASnnWQ4tNfEzCFoyfBdPE2ltqMEh9uyLNTZtk9eMmdUX-dUzn_cWJADzgu5rOBW8K4R-TtRz6ppM,&b64e=1&sign=9a9ead7236d272a932ea7af1fef116e6&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.3,
                    count: 51,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 13,
                            percent: 81,
                        },
                    ],
                },
                id: 546830,
                name: 'ELEMENTX',
                domain: 'elementx.shop',
                registered: '2018-11-28',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--elementx/546830/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            phone: {
                number: '+7 4996771466',
                sanitized: '+74996771466',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpU5KGO7e4glTTM1zkj5EmHiQMm6DLZ5b4GXkLBHnRGeerFn2DTavPI2OKpJQyELTjXGcbeb0cj4fD9EOOwqJBkV-boZq_Noba4VDEDG1tKoihMskdAZ28nJ1N2ZGDGK4xxzaUmanJ7GS7_3tHiamqtR9djkGI5XYko_M3Ik1qzxI1D-64QMQoopTjVLOE9IjidIBV1hIW1kqdggwCAj7E7rfkZ9__eZWFg7lqpE1fMhzxze-GtIEVNAPZy1vhShTwAttq31u96hCPLLa_IXCApickawbXje0lrM-YP8nxJrL2_encxvg0I21SNW67V8LGgiaF_5CxfQRapAB0y-sJw4hfoIpneazc02jpHnwIF5BSuscsaI-kE4uZhzyQAnbE1tHasfSa9SNZoFzl5GjRcqR2lnEgDCZJlOig38sT9Fx3ETtik53-3SLi0BZAGWngiiI_UU5Ox2dChAXyRAWUb8l6FvkdCUKklwqtU1PcQpI5o3d_2FBDkDBiKw7vaJEcX_0KNv_G0csz2AmTLhpmX8o-RF2cPdMAQtrZ6BAQZQh6101OE7587rkYsV1gGroT4X4PXNtmEaAiiO02zOHqKBvH0hgc5oczd_Didp-N8f4o8KgboC1k73HGaLhBgNkSmjFVhtZFQqSEBl5_XIb8jGytvsg_RNf9hPaSmi5ZUmT4R-jMUdEujRAERAss2BcC2cpCWKTftRshMjVU8sY6lp-IuuCcCRRIxcZ1ev2puJ_nh2xhC6fN3EbWDs9hSZqaTynYKcIMsYHIsfHCVeD4lSC8fwENcp2dREkXrN7OYXHyG0keQioUvSB4n25GQJ8PBqnMJo0gRBAMpzGAxq3mVR8FC1CqHzeJZLZdNWP0Jh-ss9qkmcefWMlhbKKJXCIBFf5nRPlfdWuqBbKjiY2LhmAjdQhqQQ9Ancgir5KrgJpFY4aIcbQJ5KoJHZeh5HeEBut3KjXg1hGSM054Y5W4FCMpJNmBgg3ko_M6FUb1R_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-6UOwDxYItzXB9_LDaIK4I6psRgU1afvUvkK8hX0VJmlQLYM0602ZUWJcLGQQ0YHlGoPxPbcZCRxJiOBrpUQ45pbyIAw-56BKh7VqCw0dWVl5XUL6bv034MqK3on9DprgSGPOX5EZa8CvejhGFxx1KMGMV7PzMYFPM_45VoypIeQ,,&b64e=1&sign=832b734d602f24cef51a42ba378a3792&keyno=1',
            },
            photo: {
                width: 607,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1582458/market_TPgsZjESfojDS2C0F3rbeg/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 21,
                        },
                        brief: 'завтра при заказе до 21:00',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 15,
                        },
                        brief: 'сегодня при заказе до 15:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/Q5JY-sa6ytVdq63rM5hgzA?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtv_WsaAD2wcUQTa7G_-_atllmCBvvoD5lWF65BiLFUOp_s4KZDjrEug8fURXhrxx9giO5UBZRIXbJqRLKfqoXNjzPz-tSXSaFEK3dlY5UWwUytrzKNIMR_yrp6w7tTRl89v3cMp6FIfqH0QUMasE4Sn&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.005095730536,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.751839',
                    },
                ],
            },
            photos: [
                {
                    width: 607,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1582458/market_TPgsZjESfojDS2C0F3rbeg/orig',
                },
                {
                    width: 701,
                    height: 695,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1734040/market_qiwhCP1oYeRli9PeRHqkkA/orig',
                },
                {
                    width: 701,
                    height: 659,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1712983/market_1AoTgV46DJamb7zaq5H_Rg/orig',
                },
                {
                    width: 701,
                    height: 495,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236356/market_OWogfCh_k4LvjaHHJA1pxA/orig',
                },
                {
                    width: 480,
                    height: 480,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1910425/market_Y-WvmhN6Te1jFGr2EkPPGA/orig',
                },
                {
                    width: 657,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_0cUJfnlK4NK1anGn5CtVLg/orig',
                },
                {
                    width: 701,
                    height: 654,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167558/market_ji1f6sRxRy2tt66SMxyYxA/orig',
                },
                {
                    width: 701,
                    height: 646,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1548377/market_T0fG0IlvpbOpKDmQzvrXqA/orig',
                },
                {
                    width: 461,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173932/market_4jnY5Ec9uaYUL1dvN2panQ/orig',
                },
                {
                    width: 697,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1601350/market_uIb9WKt8I1efTv8FExqSGA/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 173,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1582458/market_TPgsZjESfojDS2C0F3rbeg/200x200',
                },
                {
                    width: 190,
                    height: 188,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1734040/market_qiwhCP1oYeRli9PeRHqkkA/190x250',
                },
                {
                    width: 190,
                    height: 178,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1712983/market_1AoTgV46DJamb7zaq5H_Rg/190x250',
                },
                {
                    width: 190,
                    height: 134,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236356/market_OWogfCh_k4LvjaHHJA1pxA/190x250',
                },
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1910425/market_Y-WvmhN6Te1jFGr2EkPPGA/120x160',
                },
                {
                    width: 187,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_0cUJfnlK4NK1anGn5CtVLg/200x200',
                },
                {
                    width: 190,
                    height: 177,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167558/market_ji1f6sRxRy2tt66SMxyYxA/190x250',
                },
                {
                    width: 190,
                    height: 175,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1548377/market_T0fG0IlvpbOpKDmQzvrXqA/190x250',
                },
                {
                    width: 164,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173932/market_4jnY5Ec9uaYUL1dvN2panQ/190x250',
                },
                {
                    width: 159,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1601350/market_uIb9WKt8I1efTv8FExqSGA/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZEmY46rttY2W6ikP1uTNNQsAYx0nq7Va5GDrz5ydqcolA',
            wareMd5: 'rNPgofjB3xemlQ0aFpzfDg',
            skuType: 'market',
            name: 'Автомобильный магнитный держатель с беспроводной зарядкой Baseus Big Ears',
            description:
                'Baseus Big Ears Car Mount Wireless Charger обеспечивает надежную фиксацию вашего смартфона, а также позволяет зарядить совместимые с технологией QI устройства. Он может устанавливаться как в вентиляционную решетку авто, так и непосредственно на торпедо с помощью комплектной подставки. Крепление держателя в дефлектор осуществляется посредством силиконовой ножки-прищепки, а на приборную панель — посредством 2-стороннего скотча.',
            price: {
                value: '2090',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZkfwbTv5q9y2_0Z0Opql1t_mOiSEBfoBzmEyWVr7SuYQMCS5_95_MlKjMYj3KwlI2g3YqwbARR7eNf7EAw7BAUMKusIG-ORgODvi1bkywgwJc7dkf3m_CSP1yVXwxdMd4kQbuA--yYFL1SmLi4VxKujJG3jpsdZz-c3B5gm45YrsSBwQIe5p8tYh-V83-5IXeMw3zbCm5m56GgJUrOj8iKp_DkN7iO2goLtKCO4cIM9YAznceJnQ92Tfnf3YYJFpIzDsVpK43zTFiGiHrnSUhUPNM-qRCK3wmiwUaPdhI0YyB_HhV9GzG8nyWkPTUzoBnHDRmpF7eqNPMDAYMS7qQiY182QEPpO7WwKdkHMJ3VgNfqen1gixl5tpukkXhJF5UDIxnaxJMYOUK7EcB9tcwFVawPHh-qQatrJygPRz7IiwO4SZ6ub2j0np5eJk_vwPILSJdMJXL3QJPDZSz7i_Ko8vBmdC63O5J-YngJMKOoLGBNsubCiNn6V7_HnK7H8C0Xpmaqihcu_PIkZHhWs_jJ1uD41iYpsMtt3ZEc1RXRzDhX6FgOgvAaEsFSRuuNfmxoIalHXXtuiwt9fZ8vMwdAytEl75zjY6Y0398w1ilfpTq5d4FIXzbCPAfUyzf9GU9drNLc_Upa2nrlrGzxSfo9fEScTbEhRQ8UjoDIRB5BsIjnsNvqeMCUt2dxsFC5UFRZbcPV7UlhrVYjORKqAL4EMh2MznmX3stGIol7ztrn7jqBvHXWWoIPNP7knuuon3RL4-jAQ_gk8dVG2L3v_KPlt9L2U7Rf8OXy2yuQFBjKLe2cdPfSDfH6K5TCq7nqXgNUqxXrr0nfMTDKFhXXtFV-b3WWTFIbJnl4R5Cbgqft85nYI7hg6x4yQaWvupSrZiZmdSwE9J4YJ9wDzHzdt1s4h3wEFvO4tfA7jd0D_2ATG_GbWcwJzBHBk,?data=QVyKqSPyGQwNvdoowNEPjUdbsKbQXG5ktn3qpVeCoW9Da4w9d0PKc5mzCXH88IblXj9KX90Q32aFQWKcP7-6niYHXL_K639pYcRhLjTsnp-4JuZnWiPZeFkYvPARk08bUNCn2Fi1jL9-6mB41NUHxe_pf5Ogg7I5QwdrXa-M5g1CbSO5TEZDiLHYLjGdh-cq6iYq9urQTldqUAk9v4A_g1J3cUpZGmHT0xZ8jIJh96YS4iHBNNMSn3BMETonH-B0K5_hWdCnOa-sEpSEpZ0TxOF4Det_oyepspTSlM9r7LPKy6-OqmV9-KvEaoZJt9N6&b64e=1&sign=ccf9102a637f875047e5a3ca90a9d6ba&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZkfwbTv5q9y2_0Z0Opql1t_mOiSEBfoBzmEyWVr7SuYQMCS5_95_MlKjMYj3KwlI2g3YqwbARR7eNf7EAw7BAUMKusIG-ORgODvi1bkywgwJc7dkf3m_CSP1yVXwxdMd4kQbuA--yYFL1SmLi4VxKujJG3jpsdZz-c3B5gm45YrsSBwQIe5p8tYh-V83-5IXeMw3zbCm5m56GgJUrOj8iKp_DkN7iO2goLtKCO4cIM9YAznceJnQ92Tfnf3YYJFpIzDsVpK43zTFiGiHrnSUhUPNM-qRCK3wmiwUaPdhI0YyB_HhV9GzG8nyWkPTUzoBnHDRmpF7eqNPMDAYMS7qQiY182QEPpO7WwKdkHMJ3VgNfqen1gixl5tpukkXhJF5UDIxnaxJMYOUK7EcB9tcwFVawPHh-qQatrJygPRz7IiwO4SZ6ub2j0np5eJk_vwPILSJdMJXL3QJPDZSz7i_Ko8vBmdC63O5J-YngJMKOoLGBNsubCiNn6V7_HnK7H8C0Xpmaqihcu_PIkZHhWs_jJ1uD41iYpsMtt3ZEc1RXRzDhX6FgOgvAaEsFSRuuNfmxoIalHXXtuiwt9fZ8vMwdAytEl75zjY6Y0398w1ilfpTq5d4FIXzbCPAfUyzf9GU9drNLc_Upa2nrlrGzxSfo9fEScTbEhRQ8UjoDIRB5BsIjnsNvqeMCUt2dxsFC5UFRZbcPV7UlhrVYjORKqAL4EMh2MznmX3stGIol7ztrn7jqBvHXWWoIPNP7knuuon3RL4-jAQ_gk8dVG2L3v_KPlt9L2U7Rf8OXy2yuQFBjKLe2cdPfSDfH6K5TCq7nqXgNUqxXrr0nfMTDKFhXXtFV-b3WWTFIbJnl4R5Cbgqft85nYI7hg6x4yQaWvupSrZiZmdSwE9J4YJ9wDzHzdt1s4h3wEFvO4tfA7jd0D_2ATG_GbWcwJzBHBk,?data=QVyKqSPyGQwNvdoowNEPjUdbsKbQXG5ktn3qpVeCoW9Da4w9d0PKc5mzCXH88IblXj9KX90Q32aFQWKcP7-6niYHXL_K639pYcRhLjTsnp-4JuZnWiPZeFkYvPARk08bUNCn2Fi1jL9-6mB41NUHxe_pf5Ogg7I5QwdrXa-M5g1CbSO5TEZDiLHYLjGdh-cq6iYq9urQTldqUAk9v4A_g1J3cUpZGmHT0xZ8jIJh96YS4iHBNNMSn3BMETonH-B0K5_hWdCnOa-sEpSEpZ0TxOF4Det_oyepspTSlM9r7LPKy6-OqmV9-KvEaoZJt9N6&b64e=1&sign=ccf9102a637f875047e5a3ca90a9d6ba&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZkfwbTv5q9y2_0Z0Opql1t_mOiSEBfoBzmEyWVr7SuYQMCS5_95_MlKjMYj3KwlI2g3YqwbARR7eNf7EAw7BAUMKusIG-ORgODvi1bkywgwJc7dkf3m_CSP1yVXwxdMd4kQbuA--yYFL1SmLi4VxKujJG3jpsdZz-c3B5gm45YrsSBwQIe5p8tYh-V83-5IXeMw3zbCm5m56GgJUrOj8iKp_DkN7iO2goLtKCO4cIM9YAznceJnQ92Tfnf3YYJFpIzDsVpK43zTF1-9IMJV70fHmBOaeHz9x0jpykMiTyPi6qSSiZfTghi-RSyzvCl62a5QT6W1ZkkGa_b7ZuATYF6D9vVRvL7V45iMeV269CXqm-LvOQ8sub8WB_zq-JZ-asWd19_96uiSdn8yvwWp7eOJ9prjJo4WPH0wdf0WVhF0UfoomeJgvKg9yQYtyf3siGgwT89o5VkdXq3XSFxQqLlj6NPeKlqmUElhLgJzEMcgtfIsK1fPub4z5oiPjdhqu8zgsk-ykIU-0GLebqcCiDoNKq1JQIqSDAzmi8xenEIf7384n0Ar08VHJUsV_qih7jLzKAxm9NH3Hn3b6vlLZDw4Lz64fZJgEh00B3D9F2a0gWknbqZwGYai6b00n8Kp3YcEkIqGOSAUjGXkRyFfTcFBH-DMa9IWv6R6mVQ48wOI2Aq0jvvDwOeBbSFwHvoQ0_fxXQTvmsYONQQJQt08C0nVGx20iDtGeCr1ku-eXL2E5e1xol3r6c7AL-jNqBO0C7OHmFHc8BRF7a8490WmrR00v161b8tstsZSdPOUCU8_mNXBxcOBmLANmu1zGV2fILKbGh0MA9XXtBFhWonDEH5a2gAYYyQQqEAR4fdDhkxFI6_jbsyxFVKFaSqoCUTbj5zzEhk8GmRkkgbEmyr3z5Mrrpbcyt59nrtYDcbIlGir002P1i9v4sts,?data=QVyKqSPyGQwNvdoowNEPjUdbsKbQXG5ktn3qpVeCoW9Da4w9d0PKc5mzCXH88IblXj9KX90Q32aFQWKcP7-6niYHXL_K639pYcRhLjTsnp-4JuZnWiPZeFkYvPARk08bUNCn2Fi1jL9-6mB41NUHxe_pf5Ogg7I5QwdrXa-M5g1CbSO5TEZDiLHYLjGdh-cq6iYq9urQTldqUAk9v4A_g1J3cUpZGmHT0xZ8jIJh96YS4iHBNNMSn3BMETonH-B0K5_hWdCnOa-sEpSEpZ0TxOF4Det_oyepspTSlM9r7LPKy6-OqmV9-KvEaoZJt9N6&b64e=1&sign=54b57a25c649112a536fbc576670b670&keyno=1',
            },
            directUrl:
                'https://discontdom.ru/kupit-avtomobilnyj-magnitnyj-derzhatel-s-besprovodnoj-zaryadkoj-baseus-big-ears.html',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcPCXQ5WJ7VQIJQvezHeR4X2UcQ7VUNlzaR2X3wd1oOZtf72_A9ksJ3hoRys7kP3-SH3AL1mH6SQkvxk5_7U-axjGp_CzgDSaR0afVOgVwjofdQoSCeK-Ibs8vMHx2Bvd46-qCLMPdGOVX4rp75kiFWLjy4xM7sqD_75pnvpGkYcmhXaxotzVtuIfaPS34FAaR4JHw1zrwNOgSFP267XnQAWRgUHQ_hWel4MLa0C08ikXy7QMBEf6_o0q31AOc9Vbkl-0HuEm-RSK1K0Y0LBW4Ts30mB5SUuwxwm3kmDHmpP6NJxazERNATwaDOseGCv8eCYYLWpQuiBCRNJBEfVlF-MiWLiOf0IfEZTAjppYk1xIabbEEG8pXK8893_HtENYquJ8rJ2OlkKuFouM5wi1wvji9llzk9j5ASlyMBAM1laVCUinyLe-cV1Q2oDzzX2E882zgoGuq7whytEzysio7oX2xLqwODPZ03F84KV3b4SPGoqy2gAVpaq2a_dthzmMINo7yaiOHj1Du0KzkC4LuyMUAdofPdCd5V50o8Z8qFDTqpnVW9967nVM3nmLGBC3TFaykdiYssuqA4hyNzGRqCJKF92VhsloXkoe_PJ5IfVgiMPij9mtPM0lOGLWCGGDEGNaO4dkpe1CwcaROeE38ggqheFPDYhIAVXYt3_m96V6iQv82zmAsWwn_e0Z-ygihISpkdBFpoYNtjwUWW8iU1wbCbKWjZWK5cIDA9hv94-dF7h-9qp6_rY4iLwaGJSau5CWPz9s_hapsp0lqQkPfuqD_aRkHbj1qJ3RT0LPfwa57MpFtiiTCJYwd5Ff5vDH37H_ZbeaYENYNKkxKxTaE82awIB7EHJFA6RVGmiGQTEovtzQsqKqFjRnEKQkPPk34DVKSqe7X6N3BUmcJZO1h_zvzBDQ5fFSZR0LyV_9laRZlqYKCsBNvVI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZoEldbh1CyaiG5y3bKJe-6h01Zx8cDY5d9QcE4u7gwvd0WTsq9EyqW4eKeG-5WPq1j-p3P4pO3568k-tcVV5GFK2ysubhRRt5qesfN2-eKybTYjA7fT9oQ,&b64e=1&sign=c0c30e187498badf7acde5c13dd08a26&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcPCXQ5WJ7VQIJQvezHeR4X2UcQ7VUNlzaR2X3wd1oOZtf72_A9ksJ3hoRys7kP3-SH3AL1mH6SQkvxk5_7U-axjGp_CzgDSaR0afVOgVwjofdQoSCeK-Ibs8vMHx2Bvd46-qCLMPdGOVX4rp75kiFWLjy4xM7sqD_75pnvpGkYcmhXaxotzVtuIfaPS34FAaR4JHw1zrwNOgR0Ij7m1Z9L9KeeSoiU3kGeTI3g8uRLAnoiP8pjmL6f_gMTDwGnUgVO2RL_LmqOL0aZttiQCctRG-XZXgyHX5_CM7MN7u5Tjxzcg3VPVDyx1bOuVP-ZRH5_96Q07TlPs37_fNmkNYGKRdPJGTKBe7nW0_hG-g5obkVfII-7vJZc0FL5nxvTg4Hk5RDxtVaZxMfKhEFo6AWnQ4WZPEqzWTh8rd45oAPou-jBA49vsJiLO4ktGRYXw_QzVxAn2IxJlt_nqYv8tP1ugIN9at5qeM5Rv8yMNOcliYANEXANxpJIKR0OVMiGQC98E6Za2qSE62Fb0oYMtnkP-R9USliR07UXyOevPxJQLd16ljoH95wMyNMmEet646PA8JC2HWl3X5P9TJ3ghTTrE2PsFrYGw8AgrEm5u2IzahEKgpvlm1NfsSbfmlL74KMGJXIJ9dao6SSDRydwr1288GgYOYkH3FbDLwOpU0wqibyulxl5EidYbxEdEUFHYIka-b-NPtsYfeTT4R5yHgT9Z3NQRxaFsB-KVhd5GubXjb3Vm7_z4FIdi5q8Wg5pvgFHGVTLBTvH3602BJksTDYpS3dxB5EpbHaAesx96UqzsJgRuqniTS5TUu-9WEnv5dfrljC5_AHf8MZXct7Q6tFue_4oOG_L5RREseLAqwlYW0fcaS5vUsfALL-c9x-jBz0f6KzMaC-JY-q6NGV72xYNFsEabxt4VbKzf7c3JB1eWmjBmjC7oaMnQ,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZoEldbh1CyaiG5y3bKJe-6h01Zx8cDY5WlvH2ClFYVPkQmXHDqj4grksc7vjIkFRNN5gENg81u-8s27B7DyP-AhzHx5vWINzacJnCKPslujwhYErQKRFOo,&b64e=1&sign=9f76571ba60a39d9e8a9f1d76b9f5f3d&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.8,
                    count: 487,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 5,
                            count: 403,
                            percent: 97,
                        },
                    ],
                },
                id: 453981,
                name: 'Дисконт Дом',
                domain: 'discontdom.ru',
                registered: '2017-12-19',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--diskont-dom/453981/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 432,
                height: 528,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1769565/market_14JOL8RQylDZThqYPK_gCQ/orig',
            },
            delivery: {
                price: {
                    value: '249',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 249 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '249',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 18,
                        },
                        brief: 'завтра при заказе до 18:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/rNPgofjB3xemlQ0aFpzfDg?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtv-yJK_h9Dj7pxWh88Txx-0pFM2ljpbWPlXYHngZAQv5tsND3DeY4UY7Ai0dhhKl4tMD5h29X7aKKDBAbStbB-lazCX_DslkETwpKvgvISJfvgB-YWHe4bj_f6n6pKZH3uEs2jzpJNJ58d_lW42yANX&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.006075436715,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.742608',
                    },
                ],
            },
            photos: [
                {
                    width: 432,
                    height: 528,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1769565/market_14JOL8RQylDZThqYPK_gCQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 163,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1769565/market_14JOL8RQylDZThqYPK_gCQ/200x200',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZHELx-Hn9w96HNSY3_a5bnXV6zmoi7FbfEg9D12gwX_sA',
            wareMd5: '8fHgyH2saOCHRVh3L_9LkA',
            skuType: 'market',
            name: 'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger',
            description:
                'Автомобильный держатель Baseus Big Ears Car Mount Wireless Charger обеспечивает надежную фиксацию вашего смартфона, а также позволяет зарядить совместимые с технологией QI устройства.',
            price: {
                value: '1630',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZSbe39YZ6UyRI96etzNKEsH-88yGdojMlNgVgzfVVTj-G2hVs5TYF9RraIK4hJ_U82z9Bv12wPqyM3Wo1tDx7RRDIwlpePEvTGqkNJ2OJvTrPMujp97w7ddQyiEaG-ohowW8KvV32_4jG2a1mLmyTFJoPclw1EU28Rr00d5-hajFTXZ6eUMRodt6wMFn7ufP-E2-bUsmyIRrV7RTOXwdwADKkN7xfaRGcRsA228leTjMOJX4zrMjDWQ7nu05y3KPTgB2YbeaANklXdKVnQZeRUHDeVTqrvAgtiYeOYui99moyi8KjDW0fkfa583WhU1RjZElKM7G4fUFIXFrvQzrPIpad_wK5cP4pba3Kj0Fg0tgDXeyWqmsqoSO9TJsAXpi_2r93p09k-jIzIjT6qncmfYByje2Pp--6jUkQF4XN86TT8JGlY9yxudeqrQYAZypeYr3xYRj9hIXQ3kh6ItVsWz991onke2qHvD80oBeclArS1Iff90Idd0ugo0Bc3s61SooQCskf8A6n9bZ3WWVYUabwxAxBua6UugAhsXxFZlOccQ32DWiDi0FvYrNa7r0OZc2Gx371DBa-Ewfytxuvwrn4m8YBK3InuYGacqNUp4PWue-9-k1EwlKSuprEMmYbouZUjMQDbmNNydeJhKt5vYtZrWjmx3rUmHLD9DRpydWXQaQsX3jlnSRzb8EXaI7DK1OiprYcevl-KpjYU7UPANwoA0zjYLHU,?data=QVyKqSPyGQwNvdoowNEPjUFq1-eOT0V4EBdoVfTo-9hMoeE7rKUUCFWL8moYV184sSZPJucRCAyUDy54flhXdmCzRxYgjA90BICAlwDNNM4IZ65gCcvAx3VQksMULt0fqUjvrfb58kS8VPD6K2MtDa6scPvcUm-YZG77r-7ucR-fMwDxCOJQ0pVQq5-mpmZ01GmUaqY1ZMzTu6eoZeP5kOglT9XJeCMXBx0oPAaB1j7hB-tRoguBwt0zi_CRkGrVCfxtC9P8jF8j5SUdhhfZlzcEMnvcUPvqglew6rGtMQIpqkcyjE_LOeoWqPjaJYV5lzyjkkqjXH8tGWgJwymhIPMaPXwIbAJFfqN7uC3Sze5XYz9yYjsn1RqCgbq0vBn3OaT9RBojbdduh9J492cZnLNZzF_SqE_8kscSe7P34lNJdsvKtzKS9gvgkvDOcs3Om-6DEeXr8w29ypuexE7sopYw6J0GP_i9hEsGbjEga-O-DQNuRrq_rFwmEfrL4ksAw2s-jKk2cx4QPLqcc4k4qo1DlP7R91LVpnfongPHVi4,&b64e=1&sign=b4e57d4fe33ceede81045f2ed9b2084b&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZSbe39YZ6UyRI96etzNKEsH-88yGdojMlNgVgzfVVTj-G2hVs5TYF9RraIK4hJ_U82z9Bv12wPqyM3Wo1tDx7RRDIwlpePEvTGqkNJ2OJvTrPMujp97w7ddQyiEaG-ohowW8KvV32_4jG2a1mLmyTFJoPclw1EU28Rr00d5-hajFTXZ6eUMRodt6wMFn7ufP-E2-bUsmyIRrV7RTOXwdwADKkN7xfaRGcRsA228leTjMOJX4zrMjDWQ7nu05y3KPTgB2YbeaANklXdKVnQZeRUHDeVTqrvAgtiYeOYui99moyi8KjDW0fkfa583WhU1RjZElKM7G4fUFIXFrvQzrPIpad_wK5cP4pba3Kj0Fg0tgDXeyWqmsqoSO9TJsAXpi_2r93p09k-jIzIjT6qncmfYByje2Pp--6jUkQF4XN86TT8JGlY9yxudeqrQYAZypeYr3xYRj9hIXQ3kh6ItVsWz991onke2qHvD80oBeclArS1Iff90Idd0ugo0Bc3s61SooQCskf8A6n9bZ3WWVYUabwxAxBua6UugAhsXxFZlOccQ32DWiDi0FvYrNa7r0OZc2Gx371DBa-Ewfytxuvwrn4m8YBK3InuYGacqNUp4PWue-9-k1EwlKSuprEMmYbouZUjMQDbmNNydeJhKt5vYtZrWjmx3rUmHLD9DRpydWXQaQsX3jlnSRzb8EXaI7DK1OiprYcevl-KpjYU7UPANwoA0zjYLHU,?data=QVyKqSPyGQwNvdoowNEPjUFq1-eOT0V4EBdoVfTo-9hMoeE7rKUUCFWL8moYV184sSZPJucRCAyUDy54flhXdmCzRxYgjA90BICAlwDNNM4IZ65gCcvAx3VQksMULt0fqUjvrfb58kS8VPD6K2MtDa6scPvcUm-YZG77r-7ucR-fMwDxCOJQ0pVQq5-mpmZ01GmUaqY1ZMzTu6eoZeP5kOglT9XJeCMXBx0oPAaB1j7hB-tRoguBwt0zi_CRkGrVCfxtC9P8jF8j5SUdhhfZlzcEMnvcUPvqglew6rGtMQIpqkcyjE_LOeoWqPjaJYV5lzyjkkqjXH8tGWgJwymhIPMaPXwIbAJFfqN7uC3Sze5XYz9yYjsn1RqCgbq0vBn3OaT9RBojbdduh9J492cZnLNZzF_SqE_8kscSe7P34lNJdsvKtzKS9gvgkvDOcs3Om-6DEeXr8w29ypuexE7sopYw6J0GP_i9hEsGbjEga-O-DQNuRrq_rFwmEfrL4ksAw2s-jKk2cx4QPLqcc4k4qo1DlP7R91LVpnfongPHVi4,&b64e=1&sign=b4e57d4fe33ceede81045f2ed9b2084b&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZSbe39YZ6UyRI96etzNKEsH-88yGdojMlNgVgzfVVTj-G2hVs5TYF9tMfbzC6bvfm3BIgFHWv40wC1x-hKgYMAKpOUz0D8C3nyJcEvs5xovsROvrGQIfifn-FX8rrxuwazpMMNj2BeK2-uYCdFpQ-3WkwcGWFXx-KgYBk-HaeRhTDhbGKvGUHeB6_VLB6OIzi7Y03NPen_KU-DkeZn7wmQZ1oMqC4ozV9vWZ9cuUM_lqOnHby8XyBWyU91I8-SR_4Yh2wVouuPWkIWV2wOCvQTPHQWdyDxZoAN3j09jTOxE8YGDks8VIQeK2h0RO5oD4yaIEerNx21qwK7wcmXWzvnjxSVM8YG4U3EwE_ceyUce7p6FbTIwzPoampl0XUKbA4KMnDMf2_eyMsBj5TZ1bh4tonoLOsa9C6VPT9r5--Z5Fl0U4HAWKgUXCDA1Frir7MiUWHChTsLe56WmL4z4CEjB3jMzJZ1nTchjs-8P7lvLQ3Qx7FyMM_WN3rCh7PiU0TlEeraGk-Y1otUI6WUXQqOAfuqBgxc9q4pctn9MixygcpGHJV_BuisUPZOGOiZQhtk6qtgnIEqL47AQkCQ-mAvVCqkz_qR-K0yVeNyaDceRuazeAVsJi7oTFrdCmTil3-tiNW8A9bLcAQ6UgJUCLWzc48tnZDNs1DgEtC88_kfMjTdzg6VQ69phCcSDXMBI9IXbFIh8yb2-UD2GmYgxWSV98pVC1Zhti4,?data=QVyKqSPyGQwNvdoowNEPjUFq1-eOT0V4EBdoVfTo-9hMoeE7rKUUCFWL8moYV184sSZPJucRCAyUDy54flhXdmCzRxYgjA90BICAlwDNNM4IZ65gCcvAx3VQksMULt0fqUjvrfb58kS8VPD6K2MtDa6scPvcUm-YZG77r-7ucR-fMwDxCOJQ0pVQq5-mpmZ01GmUaqY1ZMzTu6eoZeP5kOglT9XJeCMXBx0oPAaB1j7hB-tRoguBwt0zi_CRkGrVCfxtC9P8jF8j5SUdhhfZlzcEMnvcUPvqglew6rGtMQIpqkcyjE_LOeoWqPjaJYV5lzyjkkqjXH8tGWgJwymhIPMaPXwIbAJFfqN7uC3Sze5XYz9yYjsn1RqCgbq0vBn3OaT9RBojbdduh9J492cZnLNZzF_SqE_8kscSe7P34lNJdsvKtzKS9gvgkvDOcs3Om-6DEeXr8w29ypuexE7sopYw6J0GP_i9hEsGbjEga-O-DQNuRrq_rFwmEfrL4ksAw2s-jKk2cx4QPLqcc4k4qo1DlP7R91LVpnfongPHVi4,&b64e=1&sign=774ed6783404126f540d97a707a63b06&keyno=1',
            },
            directUrl:
                'https://clickmi.ru/catalog/aksessuary_1/dlya_avtomobilya/avtomobilnye_derzhateli_smartfonov/avtomobilnyy_derzhatel_s_funktsiey_besprovodnoy_zaryadki_baseus_big_ears_car_mount_wireless_charger_/?utm_source=market.yandex.ru&utm_medium=ppc&utm_campaign=Y.Market',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWxVi5CNJd9WDeD_8ZwFf1yodEHUEw_Zq7iKjLDtsMUdZEfnGHSO6R3jHt19_lUFoD6FfFLzFe6JEOnxGzyCjBSEBKdQAPvm1uvJl5fLgLtb-neuidkqsVXxdP1K8yvrf-2H_5-EyNDb9OBjMkoDHQgp__zVoyT6pb8eueTqslqYX3Ae-IgStTGD0bSTU8r9Sq5WfJ7gMyJZrTmkPObl1eJvka_BBy29zyACDcD11XvK25jiufEIE9m4wgNuPtkGldTexevxEcgfKDoNtIaI73iVthQ45bcq33bWPhSAmXCHRYoWzTOWLJcQiSyoH-GUKxaNeWx025eFSuGrZoDmY6rl1xqb9uhOstyWeVpIb6994DvWqKzMXBYDt2kVP5w8pf6ZLIGjNEQE0QDlZcWKr_DayfnnGeZF5Qqv3ljLJ70LlOV6AbsYWgKd_rzPJriQgVKnRxjGW2C0FC4aPAkSizJ55qQ466PK1bowbia2Av1kQHOD73Ps-Njs_i2YTk4j9lSC0mun8Q4ZywL-qeHt5c4X3iWqvffVrVJQmD6oPK50kTfX1zKw5aVBLmadyXlG0IUxB4fw3Hk9N41i87oS5mZcBXvUP8xcNyUGAxMb2h6M9C-UnytXVaW5RdNJxUL29RL00CD604pYJCjpUeJmy-5EBoQUPcNX1dmMhqHFKCCnp-VJrn8Saj1gkD0aJc4iRNB-2FauldrH57rtJLhRgtsGWSvMQ9kRHEX-9ggTrhcSw,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WE4_txDz02qRzMPngonyauJLJJrD61WvvaAycpdnpgVeKAzEkNbuozVOU6_1E4JDbgn02iuyed3P5hPmTk0k6hNWcumC0dtOlwRPTgg1IJQcXc2zZXtXrg,&b64e=1&sign=c971616c22daf269e80796febf398898&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWs8nPPP0xPkdomFqAZqgR6j-hNuzBn1PBmlYdIaqg39_zNJ9lTkx6lH0D62_9HQkMHzVvD4r6ajQcNP8nnhwi1VbBjPnFQneAbdgy9X9mIQTezQ-KbnSislCXCDrl3JZg5XMHYqVObJgR1zhWbLQPq30jtGNvh4bKVmpP5V6WGJgt9dUi13nWrMbRG8o5dgnoCGxx81blIiOD5WhNlLMxokhvYC3_XpDfDB-7EzwMom1hb2mf8_XuT21gNow2vW1w1HHZjQfUUI4XsfjzGp7-skgrMyo69llPsckeis82Vpr5mLO9VhB7Y9V7qoAQKLXFx13duPyYVPmE0ZWKG1R7xTeyp0UDu21dvZCFWYilAG4x3tYdF22WRK1tLkJqatHyX-vzpsFKZVME9fognpWuEP2PDfSFYNPgxByI7JUarq-OV4RFFeiAkix3pesHwMvxoUKjsSpwMNARYDJXYeA1xhj-rk-e33Wdz-vkKVi5AemZRr4vHCyOj8jUtjoLq9g_HvECYBCCi7IHWRHwer29pB6oz5VhfI1JSd9OqUJT_SLwRWV38kp6KovRIIz2UZMV7hDs8Bpc5_CyYDtf9VFFNAwcYDSW2BTmXQum5rCk85kuTYA747Z_G2Y9CPps7R3Z2oFLrfpW4udujXu_7VPx1rou_01ssqEI37NCsYtaoCYfmT16SmUF2k5aKkUJiEGvQ1VsFuay18aRymD-RqVmWvU96iHkKoIx55bZuZFz17U,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WE4_txDz02qRzMPngonyauJLJJrD61WvqPflIt6S1Iviemqk4l9HMglAWJxcE3Iicl0CgoxXSmtlHkbUj29OUMaqbIWEKDRGb9C-tkkdSAMQtOR8f6MK58,&b64e=1&sign=0b751cc30eff1bbdb44864b7b02c85f2&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 3,
                    count: 1,
                    status: {
                        id: 'NEWSHOP',
                        name: 'Магазин работает меньше месяца',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 5,
                            count: 1,
                            percent: 100,
                        },
                    ],
                },
                id: 585612,
                name: 'Clickmi.ru',
                domain: 'clickmi.ru',
                registered: '2019-08-18',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--clickmi-ru/585612/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            phone: {
                number: '+7 8007070839',
                sanitized: '+78007070839',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXQSMwcc1rm_dsH8TutXs2gcF9KLR1k3ueMzM94tKRyvpbmt3uTDnLrXf4HhfwjQ_8wYKXx3e7X1IjxkMkaxpiloXbUWXYeezCVkkzVUfhqNyzKIJp1gNFC2QriKL9KAZT6CvUNDZFLkyEHeTiW-og9ECynsjBvPnfzG9kouzAKkM7EnCWwsYyUPBUIzi-rszZ_XoXZy2hM1GyvlvzMKA_Fh_RLPHmUA0Xbx3MYjwvjYI263ly9Ku-bPWQDmRMEmnvYWgLr52d7FEU7ht5y5pK6GlazGoIgbvrwx9tjXp6IsDXKJoJnr-GoRaAzGD0Pxn9xxqN1kh__kkalk-2huVw4lqmKCUtMqHmEmdpsBeMbcvyCw7hZpZaUQUTEp50jnJqgGQCiT9_nk9Hf5JlKuub5U4hf0SWTJZ6pll6tVBwbXaj2r9GdKItBMcxX8KO9omWtbtZiNoX7NL-14vTgmEYEWyELa-qqGeTKZQ3oxJCveoJVwWv_2YOP9vcYDCCpJjKa28CDgSDG75eKHe-fHG7WBGMvItNKaCgY4VEVhanGzCHb148WjAAEC_sJdbVSwBDmCGmX5Yy7GSMtDptGiVEYpk9nydORoQoozvqMUddqvwS31iTy-t-oA8zo_y0GI4_D4czcUBv2ry6kSH6sJ7f4BA-PwScMyfnde4PeZgbkYpvvrRItV-4JN8OBQXeBK7WTV7Aj8gdqo7OASo7GWJXSPAI5MM1H4Ji67ANnwRLa98,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-37sjiPVderRVuCT45clFYupYKbhKeH_YI-utf1kJtft4jQm08D94sYmGS7BFMeA9dhaTF3lmjr2E-GoA-aee4wBSzP81-GV7MlL4v2hU9tJApp1yC4ohnyVIkmp7pJrs3IAKLG1yv22q68f1v6r6PjWMbnUW1M5r_6cicIkNdUA,,&b64e=1&sign=df937da6aedd460f3698fd3af4666836&keyno=1',
            },
            photo: {
                width: 800,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1876493/market_QI57hgqaR3Ulzm-uPdfeNQ/orig',
            },
            delivery: {
                price: {
                    value: '290',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 290 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '290',
                            },
                            daysFrom: 1,
                            daysTo: 2,
                            orderBefore: 13,
                        },
                        brief: '1-2 дня при заказе до 13:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/8fHgyH2saOCHRVh3L_9LkA?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtu9UPpOGgmRh5xZyP6H5WnBTIwEcbr3b1Dt3ewV-x7LiyVJZDuSnxB6sii2ZsiAqgZv4NZVHgGQoN2tZthxaJpyGv6ejmS2yjMpSNA_JDahABJO-muJGG1OGe5afr8F6CT_djjKrnP_iuKu5VbeWA4v&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.01293702517,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.735242',
                    },
                ],
            },
            photos: [
                {
                    width: 800,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1876493/market_QI57hgqaR3Ulzm-uPdfeNQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1876493/market_QI57hgqaR3Ulzm-uPdfeNQ/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZHvJGQlegOwLVO-u4lwwNQ2lRbT44YK5Hpyp5v-DUgp_w',
            wareMd5: 'mATiZfXbKVFP8TbpLEtTLg',
            skuType: 'market',
            name: 'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger Black',
            description:
                'Современный держатель для смартфона должен не только закреплять устройство на панели вашего автомобиля в горизонтальном и в вертикальном положении, но и быстро заряжать ваш гаджет. Автомобильный держатель Baseus Big Ears надежно фиксирует смартфон как на панели автомобиля, так и в дефлекторе. Фиксация происходит при помощи магнитов внутри держателя и пластины, которая крепится либо сам смартфон, либо на чехол от него. При этом никакого негативного воздействия на работу устройства магниты не оказывают.',
            price: {
                value: '1400',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZEfrGD7nmicRFOk713qldpep-28L6UpYmFab-MU-dwsNxiUt0in9aEDyPHFO7EY24WhaCxlThTmWeXYLS2RyYhOUCVCkTZStYPhLgOcIrZAPOKsrardQRPp8MH1NvFOAilyon6l-Czit7xsnJjefKeGaq1AEOTkX2-DdvTXFLYt1TnsuZGL9_-3ux5DsBUlFNfJAP0b8wXpGMmOlpouvgxW2Z7yKzEUz19peIoMtrv3at2zIt4kSdTp7b88gq0TOfFl4eOimDsAmhBLyGrX9j2hR7BlYENSfCKJs6PO9Ez2EiN83Zz39vzrq-IUUUBDo6EYmJVKzA2ASJWR7McxUUtGHMqn12oJRPNKuW9HV0SFA7WvNr6_gR7SN1UTmwbLocb02Di5y10ZIdyJ45azXv4fw1j8LhzpOrElWZ2OxK7R07ODRYjgd986CQar0M_UYZfExeZcNE83ed_rxFJPsdlDYguSZmnkCqyrnmz_otiLAjh-K4QGZ5yM09fece1UI87lD2qwv-yno9hVL8h9zfSG8AYsUxMbabcncRzxqiScLO7x-GEJIU1sJKwyQU1SOmuFFzhpZixVKg0dlF4Jua9kNs87ItRo390GvY_DiKvqX2TEz4Su5TK-0sKU1VsfQ96fBOKiMVNCBvRvbbsZzXCNnzGVeOrlul2QdBUS8jZlSt8tJTzDGUSFwI-gymHXwFyrlHBE2PdRIFRPjCsB07prIOEO-R4N64,?data=QVyKqSPyGQwNvdoowNEPjWZf9xlt7a76drBd9lLGFu7MLM4dkp92Bbr3nk2XdvKWxHZbG0P4eLZuAMeaVrTMpEm32L_1371OWgw6-u_qcYM-qw23P8PrnbcjO6vLHyyxGTpB3QwQMzTx9N6F83p8xH2W6Rhw6tlk-EUmFq97iiDI_njlGNZVkeL1eaMO-RudDEKdv-m77_VxR1rYQniSz2857KYay4dohGl7R9JF8TLLQDQ9DiYLklveV9ufQ6Iobj5oOUorHNxCS_N8fqQg3qm3GeedzDuc--O_elCwnd4R1noFlbpqEbFnz99IkaFwNLPnBHke_vLXBAN2YzhofLqm2ahjjAVUsHhdBuOl3_p4c7wnZ6sPkT8PwNNtbNmjiZYqcrkqYqpJcBxGF_yfKdH3fAhdsS_mTbkN5wttHlqdCc50l1TIwLufhotu1h8zfpXX5r-kwQ6cN70nOWBGOIwvyiHBJQZYuwWkt-q2bglKniL8iOSLLmzeqM5wQKEYV-5ks80nx5DbKUtlqEO_gT7H7820yG62wgSUnyBij58udF2IOKcKWA,,&b64e=1&sign=160afe13cda6d8e772be9a08caf79220&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZEfrGD7nmicRFOk713qldpep-28L6UpYmFab-MU-dwsNxiUt0in9aEDyPHFO7EY24WhaCxlThTmWeXYLS2RyYhOUCVCkTZStYPhLgOcIrZAPOKsrardQRPp8MH1NvFOAilyon6l-Czit7xsnJjefKeGaq1AEOTkX2-DdvTXFLYt1TnsuZGL9_-3ux5DsBUlFNfJAP0b8wXpGMmOlpouvgxW2Z7yKzEUz19peIoMtrv3at2zIt4kSdTp7b88gq0TOfFl4eOimDsAmhBLyGrX9j2hR7BlYENSfCKJs6PO9Ez2EiN83Zz39vzrq-IUUUBDo6EYmJVKzA2ASJWR7McxUUtGHMqn12oJRPNKuW9HV0SFA7WvNr6_gR7SN1UTmwbLocb02Di5y10ZIdyJ45azXv4fw1j8LhzpOrElWZ2OxK7R07ODRYjgd986CQar0M_UYZfExeZcNE83ed_rxFJPsdlDYguSZmnkCqyrnmz_otiLAjh-K4QGZ5yM09fece1UI87lD2qwv-yno9hVL8h9zfSG8AYsUxMbabcncRzxqiScLO7x-GEJIU1sJKwyQU1SOmuFFzhpZixVKg0dlF4Jua9kNs87ItRo390GvY_DiKvqX2TEz4Su5TK-0sKU1VsfQ96fBOKiMVNCBvRvbbsZzXCNnzGVeOrlul2QdBUS8jZlSt8tJTzDGUSFwI-gymHXwFyrlHBE2PdRIFRPjCsB07prIOEO-R4N64,?data=QVyKqSPyGQwNvdoowNEPjWZf9xlt7a76drBd9lLGFu7MLM4dkp92Bbr3nk2XdvKWxHZbG0P4eLZuAMeaVrTMpEm32L_1371OWgw6-u_qcYM-qw23P8PrnbcjO6vLHyyxGTpB3QwQMzTx9N6F83p8xH2W6Rhw6tlk-EUmFq97iiDI_njlGNZVkeL1eaMO-RudDEKdv-m77_VxR1rYQniSz2857KYay4dohGl7R9JF8TLLQDQ9DiYLklveV9ufQ6Iobj5oOUorHNxCS_N8fqQg3qm3GeedzDuc--O_elCwnd4R1noFlbpqEbFnz99IkaFwNLPnBHke_vLXBAN2YzhofLqm2ahjjAVUsHhdBuOl3_p4c7wnZ6sPkT8PwNNtbNmjiZYqcrkqYqpJcBxGF_yfKdH3fAhdsS_mTbkN5wttHlqdCc50l1TIwLufhotu1h8zfpXX5r-kwQ6cN70nOWBGOIwvyiHBJQZYuwWkt-q2bglKniL8iOSLLmzeqM5wQKEYV-5ks80nx5DbKUtlqEO_gT7H7820yG62wgSUnyBij58udF2IOKcKWA,,&b64e=1&sign=160afe13cda6d8e772be9a08caf79220&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZEfrGD7nmicRFOk713qldpep-28L6UpYmFab-MU-dwsNxiUt0in9aEY4cT40KyMwacKdkNZv88GW_ADccy83Yn68b_Z8Q-3HVjb8K2zPWyWDVi8zniB81G0JSdLjLxf5nGqH2r78BS-KF7Ldf9aLoLXR5swDgYVvEyKLhM0-xz794sdyrUge4E0MOFBDLN6gbLfBHc_plJxeuthd2CsRa4XDH_Q1SCyV22QBWW261X9FmILIC81DvPBDHc1jnwoxIGE0iOuVFiXRNzBPMBJsOEFDFAfIKS32SDP51ookFWBqSRXALtWjvrfTE2UH0s1j05w3RUcn6cOllqqRDbYwkQOo_hb2egMpm0qb9Y0aPMGWFwoYg977THUIIFmpKH64j4Y9mVEFAEeILG6c1HQpUp0_0isk5E2GCShSqVnm8lAJmfaDWPAqnhhl7Tm_O7pe10PESNhnbRJtDsc_V3Yre8qrcI2YOlk-WjNM1O4RBiwRx7z1txJSI6-O2uL9CH2wY_CAZClF_sutUaxNakPeQ1nxflcvAPmwArhj4-i-okc0PPMpvMLV8ZFEvVb2yXf_G93Dkk-P2wwy6hcOSB6-1PwIwx-2d2TTv-KtbW1hHQ5dead_uh7sEDICnjj-uDLHUQym69FmziaefFu9_yaAivcUKUh5qEYVhFQg7gJnR8Y-8k2CYobO9Gq1sj19VlawJdwvRPjlHcOHt96pimq38xtNIRLS9QrMY,?data=QVyKqSPyGQwNvdoowNEPjWZf9xlt7a76drBd9lLGFu7MLM4dkp92Bbr3nk2XdvKWxHZbG0P4eLZuAMeaVrTMpEm32L_1371OWgw6-u_qcYM-qw23P8PrnbcjO6vLHyyxGTpB3QwQMzTx9N6F83p8xH2W6Rhw6tlk-EUmFq97iiDI_njlGNZVkeL1eaMO-RudDEKdv-m77_VxR1rYQniSz2857KYay4dohGl7R9JF8TLLQDQ9DiYLklveV9ufQ6Iobj5oOUorHNxCS_N8fqQg3qm3GeedzDuc--O_elCwnd4R1noFlbpqEbFnz99IkaFwNLPnBHke_vLXBAN2YzhofLqm2ahjjAVUsHhdBuOl3_p4c7wnZ6sPkT8PwNNtbNmjiZYqcrkqYqpJcBxGF_yfKdH3fAhdsS_mTbkN5wttHlqdCc50l1TIwLufhotu1h8zfpXX5r-kwQ6cN70nOWBGOIwvyiHBJQZYuwWkt-q2bglKniL8iOSLLmzeqM5wQKEYV-5ks80nx5DbKUtlqEO_gT7H7820yG62wgSUnyBij58udF2IOKcKWA,,&b64e=1&sign=b28bb30c35b327f406f9eaea9ca34579&keyno=1',
            },
            directUrl: 'https://new-dar.ru/product_by_id/143193499',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.3,
                    count: 431,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 29,
                            percent: 9,
                        },
                        {
                            value: 2,
                            count: 8,
                            percent: 3,
                        },
                        {
                            value: 3,
                            count: 6,
                            percent: 2,
                        },
                        {
                            value: 4,
                            count: 14,
                            percent: 5,
                        },
                        {
                            value: 5,
                            count: 253,
                            percent: 82,
                        },
                    ],
                },
                id: 469568,
                name: 'New-Dar',
                domain: 'new-dar.ru',
                registered: '2018-03-23',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--new-dar/469568/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            phone: {
                number: '+7 4951759327',
                sanitized: '+74951759327',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXQSMwcc1rm_c53iVMeZDLMzSSg3lVp9T1hrwbhX1iJ2A7ThHheqHJLpTfY2D34kHs1YA6EiWt5O7z2V78K_P81HYHtsxvQnyz3caTUyUICyuUZebEqVktKCGRmqowh7cttpCnoOdZV0bynXnDDVhMEaf4NwCSferIO00uY386_g1HpDBYUMG8s5SMCui2EpahO6UuP4PcdbIwRQmH06S-VwA2u9l4KWm5CRSWjewJP1AiUqQ57fU6y7TULSMH7yJSn-Z5XLiO4HCJIEfmwWVu5NIDNG6qVkRV8M7jo9buYoOhzuSbdB9xWXHoREwQiUFRGYOyWCtaxyhZNLsK3AFJNyuJNNLhwIMNVaOiyg4czxyAXCJcEPIulkdJPpV6hEsoNY4Vwzh9HNtJ8eP_NTjwkSp85ZfLAY40hVT8jo2ECRNWOQmkjdAThVlPBTC3Ie4yU_H1gmHLdiXbLErh1lb4lk3oFvcpmt2yBIg4mSdRfi_OyiprY2rjS3tv95wtukYfv61qIMsxOno1EJnAs8pVAoUlAVqDNhqc-Uk3Q9aq03xCa35tWQcMzkYA0Os97_y-vF0ZFhUM9w-wgbmDzAefyfqOxJbJe7L76SjmgzmtytnrxFy-uTafgJkKvmgFsSJVkF5NKU3b8klxYEWsai2U5is7ZoMYx2i17IzxXjKKtCJ8SSh66VIYwLo1t_1F1NbGWNfbiOVVNDh-NP_KsOOT9usq5sK9cuhFwfShd6WzMKo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9ihUsBPbLHWw7U5IyrhMsRHjbDfU-6lmvSduZtbR8Ela1_JzNE9MFD4bcGvJrZxGfqNwbnl7eyxpekIcT-sP_p4UHA29tfLYTnB9alebWo2XTXx6ORcH-3C7UZmbP4R9B40m62h4lSTU6Z80LKrdIZC9cDZhjPyWMPDIgGWikPqw,,&b64e=1&sign=72d44f26535367c625b3705ca52a15d0&keyno=1',
            },
            photo: {
                width: 607,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
            },
            delivery: {
                price: {
                    value: '250',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 250 руб.',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '250',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 20,
                        },
                        brief: 'завтра при заказе до 20:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/mATiZfXbKVFP8TbpLEtTLg?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtsVyt3NsmXc4ufuMQq7le2hzD_NlvOYQzIr8OI41NWsNhOVHP6WGesEbBIHLCXGXgvZIAAMIBRCxsY2xABQnUjUuksZWhD8X5tM2jU988_D3jyxo5EeSOFbdgvRsa8g5LYCYroSUzsbbyXgJr_g7oOk&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.007772883866,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.726852',
                    },
                ],
            },
            photos: [
                {
                    width: 607,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1107958/market_SHovA3qB1ytg62EJH15XOA/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1063985/market_vt1oRW6tIzXdzMLSS6bZ_g/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779269/market_e3mbJiEsF0qIrhsyOE1oDg/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/903055/market_JnR5w8YEgSZ_2V41bKcODA/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779757/market_95xYv6Y52VuV8EBXiFwhbQ/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1879187/market_i7JdmdD4tfSkc79O1t_ecw/orig',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1584467/market_AAJaOHBtmIznEpyuwPaX7Q/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 173,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/200x200',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1107958/market_SHovA3qB1ytg62EJH15XOA/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1063985/market_vt1oRW6tIzXdzMLSS6bZ_g/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779269/market_e3mbJiEsF0qIrhsyOE1oDg/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/903055/market_JnR5w8YEgSZ_2V41bKcODA/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779757/market_95xYv6Y52VuV8EBXiFwhbQ/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1879187/market_i7JdmdD4tfSkc79O1t_ecw/74x100',
                },
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1584467/market_AAJaOHBtmIznEpyuwPaX7Q/74x100',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZFAuhNhbSv4PIN6Tp_STDY4bIPK0Lcl7rZP1IWFrCEIGQ',
            wareMd5: 'ugtnyPwAL5Edp7k9i1Nyow',
            skuType: 'market',
            name:
                'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger (WXER-01) (076863)',
            description:
                'Baseus Big Ears Car Mount Wireless Charger (WXER-01) – магнитный автомобильный держатель для смартфонов и других гаджетов, с функцией быстрой беспроводной зарядки. Два варианта крепления — Держатель Baseus Big Ears Car Mount Wireless Charger можно крепить в автомобиле двумя способами: - на решётку воздуховода; - на торпедо или любую другую ровную поверхность. В первом случае крепление осуществляется при помощи зажима с лапками, который надёжно удерживает держатель на решётке воздуховода.',
            price: {
                value: '1500',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6Z4tA2KmZ4x2fQ-OjuH-c57OaFAjFQkI2uXQGzfsAndVCqluq0r-E19r4NkGTtFUKwbpHwwtQxwKombP-Fxn7z95NNIXo1j4jeb-WYgZjtMq7gau-gnAS5HGKPEMimCQnR9eh2EID6TTAbJ_dk7VRyzFvldxFH8yH9W9ZapaAmQleIpBetgm6cEtCEtC0gCcLWQw26VhTMmftu9Ruy3cp8_xA_DDoJa3rCu36LDltaKFCYrzlh0RxnlK_2LxJhqiShkFl0D13PzoaTF9RGZ4rrgRo-dtfQIi7Qc_KMp2xu0nLlDhbdJ1w--JUwQ_oNS6RZNGnPpPeJrmM1ss6onabfJtqw5-LqKdP-j8e37BTSarmPFbMFxo8cErPkUIN7N9VWLcbM0tLlbEe-9HJgf8R0XF3APm-NQ6LqTuaH9O4N1aGeq9pnHl1ziYTWxVjKMT8ZOHzGWh9m4fteseCAOCW5c_oV61od_9VAVR7o0hIzChoD8Wu7GxDKJbRTxzXO7LMbnk1aMjT_ukyD195230HFoPt-tUXrmVEVjYVzRkU1iElfDl-dxJh_y5SR0cofgXzS7fk86QC5nWVqHAabPoQfgsDomfH7nS4NyLIGKb-PQfz5FaBwdpSMZeO-ksn4tKLeu9-oxTLWv3dfshC-A1yuV2TdvYiuRFxbimeSrXBCfAgN6gOueTq6NhvCwsaxwL3u6tfC3smI271373giJSon06Nj5zJ0V-eM,?data=QVyKqSPyGQwwaFPWqjjgNsBr7wm8jrBOIxkDJRhkoy2oI-VtLAq3-lA2FhqoQsLQuqzKWt-DVL2EInU23iOa74qFoCPbnTKuT3DX1NQjs1vBSYZrgH4c1VdqNaZTgDPbIhdaQ0B1DS2rHUoCW6HuxyA9F9_Tdci6_g7RVCn1CW-7p0TqGo4QjWQPH43o5oDz&b64e=1&sign=dc40c968c352cb1770c6120ac4eff0d4&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6Z4tA2KmZ4x2fQ-OjuH-c57OaFAjFQkI2uXQGzfsAndVCqluq0r-E19r4NkGTtFUKwbpHwwtQxwKombP-Fxn7z95NNIXo1j4jeb-WYgZjtMq7gau-gnAS5HGKPEMimCQnR9eh2EID6TTAbJ_dk7VRyzFvldxFH8yH9W9ZapaAmQleIpBetgm6cEtCEtC0gCcLWQw26VhTMmftu9Ruy3cp8_xA_DDoJa3rCu36LDltaKFCYrzlh0RxnlK_2LxJhqiShkFl0D13PzoaTF9RGZ4rrgRo-dtfQIi7Qc_KMp2xu0nLlDhbdJ1w--JUwQ_oNS6RZNGnPpPeJrmM1ss6onabfJtqw5-LqKdP-j8e37BTSarmPFbMFxo8cErPkUIN7N9VWLcbM0tLlbEe-9HJgf8R0XF3APm-NQ6LqTuaH9O4N1aGeq9pnHl1ziYTWxVjKMT8ZOHzGWh9m4fteseCAOCW5c_oV61od_9VAVR7o0hIzChoD8Wu7GxDKJbRTxzXO7LMbnk1aMjT_ukyD195230HFoPt-tUXrmVEVjYVzRkU1iElfDl-dxJh_y5SR0cofgXzS7fk86QC5nWVqHAabPoQfgsDomfH7nS4NyLIGKb-PQfz5FaBwdpSMZeO-ksn4tKLeu9-oxTLWv3dfshC-A1yuV2TdvYiuRFxbimeSrXBCfAgN6gOueTq6NhvCwsaxwL3u6tfC3smI271373giJSon06Nj5zJ0V-eM,?data=QVyKqSPyGQwwaFPWqjjgNsBr7wm8jrBOIxkDJRhkoy2oI-VtLAq3-lA2FhqoQsLQuqzKWt-DVL2EInU23iOa74qFoCPbnTKuT3DX1NQjs1vBSYZrgH4c1VdqNaZTgDPbIhdaQ0B1DS2rHUoCW6HuxyA9F9_Tdci6_g7RVCn1CW-7p0TqGo4QjWQPH43o5oDz&b64e=1&sign=dc40c968c352cb1770c6120ac4eff0d4&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6Z4tA2KmZ4x2fQ-OjuH-c57OaFAjFQkI2uXQGzfsAndVCqluq0r-E19fHNXPHAfIz38xw2sWABbhu-Rat9jjkVyCP-x3cpM_ksnKdknOdUyKIWKSWc7kDPmIfBcR4L9GYP5n1VYkfhTq_8dW2HrKkqo54dJJSLRP37G5aRL48-qFK7jfYO0XEckK_Kd4N7nIn40VNUR5oxtAlYeO3VTw6_MvLebipCBNqAkpH9GxVpY7fPVelJU356qzgPIMOPQJXgbnPEF5z8LaV6njxy9ZTEpdAfDmy4h4paayU-vyy-fpQkSHAEuO4jykzhABiDho2UNCAgX6idCgCe8hBk9desHz7jOlKdp4XTqzllM5gsUJ1MhvM0eJxjU-vZBHm9-_Gy814lBdQ46itAapanVZIuGpUn0qZ0z2AdRXxYGX4KpIO0Cja-Gwr-OUX7C9rM3YyM67cWpbJPgV_gYZ30L1GDJESPH8liZ2Z4CWl5BvSglr0IbEpVnvfJEMMEKncS9i9Yi6h9gqLgC4S4XjuFosHDlFSqxMbLGjp7wAbgkIGEknUYFNZsMwwFv7Kgxs-0xXXD00oH-mkyBaiS1rz6N_TrSxa2kNa84wYCmDk_rbx1NWtmiTJJLzZDuXJIIL5tDh8sjpXSBZfZn9RFciWasNdzmrPyq216nrniARxfkMU9cHXVMjHbdsW_tDHJOmiNEa5igveFqBTx0k4GTvZ1Heg0bFgsbY_1u9sI,?data=QVyKqSPyGQwwaFPWqjjgNsBr7wm8jrBOIxkDJRhkoy2oI-VtLAq3-lA2FhqoQsLQuqzKWt-DVL2EInU23iOa74qFoCPbnTKuT3DX1NQjs1vBSYZrgH4c1VdqNaZTgDPbIhdaQ0B1DS2rHUoCW6HuxyA9F9_Tdci6_g7RVCn1CW-7p0TqGo4QjWQPH43o5oDz&b64e=1&sign=0476e6e1190ee730d4a96d28659188b5&keyno=1',
            },
            directUrl: 'http://www.luxgadget.ru/accessories/section/3394/catid/14942.aspx',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWDvsaqhVBm_oFsfQn-vyiekdcAAANF799uphqQX4G5iYR3_bJRbLCPSwfAL-jxQjAhsyJXLCje3NDGsZAwc-ETV4jov16wKi3qWKwMkuDcSkwEHpTJ5P7Lq1TxzIb7uMoOK6F_uxyHHpf5P37F2LPJNgEcxsi449yROaLswtwAM1l6bxdbXeFyseYM_ko_QIcO-4SSfcD5HjGqsoT9pqAQ1FZQCDsYnPWLF7iAMz_7EdMpAYji3ldpDjLcXm1MUVPSyNbn3j9F5F3q-Z7Bi8r14ZjyIlvQjuBnpVCWyRTdQQ5ohtYWONsxwEdjNuZrWvuzApJRhqJylMIjult569bX9MLSf1JeCGjduL5v1CkUSBoo7YLc9ZrdKcmf6D2pf0hvCLoOq92gmI9UIgSp10V_NvffYz0jn4ctDDgm7-AsbEjxWHtJVeSJXYK6kXF48797K2zK54jfh0XFspv-tdnuTYVSafLlfUPnOL3kEt3GnY2V_FpAihIqtryzwM26kED7pvFRQjW6ng_WrUjcndDoQ2ACI-hVOKJSv7SFmGFZJXPGEad-BHLWu_UpanO6EHOQ-u-Sce5xZKKxHbXDmkMXi0-1CElwr8kTamoVre6TbJu9F-KaaZ0sC1ZhHsxRnwnvDgpDVrGdnzJ9g34srKGT1iDnavGM09CGm8V1Pah_8CaEKouex4OudmozMUY86NB8L8mMtbMZEDH_DVxUYKXPbla-0nu2qvcQ-OkWQSh_Ds,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2adjpmlKvYAqjOlMeookgrpEZ1g86oAPhygBMjy0FIPjnn1bRarrUIoD8TKynViuW3JBKthBHc2PFZoB4dL1nPVNOvNvGnY3Lo98cog_ZZ5FRbyfhnZEbJE,&b64e=1&sign=2bd4e6ff1deace534f8c20bff4cefa62&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWDttoK1UEdMGfPCuajnamMKaB9ew_4Cnwbva1l5H8Q5O6z55Gmceu9L3laejeBcVPDYK8T_kiEzAmQomoSoONbeOyiJxUOg-W8k_ztCx7u3hZZf1PNF6hutuVfo45jjBfaiiwPO4FZQx5-0_QN_napJh_agpk0y1FB6M6mJBwtXqb7EV88Ylt8H_tkBglT-dwqPTKwAuYadkpm5J8bqYY3gxBlhJEl-ip8X12Ys7o_dqGJGF5pGNMyneVHmuMQFTK14174pCYCYuF0keo_cAePgtYWW-AgupL3r6g0A3Sk4QhxsiWj-alw1SrpB_dhqz6dHpz8XXEcbU89jy-_boVxUgHNhuzaqK1vOUj2PH1lmwPLleb7hWAjyw5bNjJmxkDsq4UGHca56MOEeulwU6yIFIBjLeuzHu-jaMAZ-960cMPIKlO_YVzvOVkWSYB-n9xKg8PPq0lYubI8RSOd-D9Mg5r78opq8TBU9HWoD11TiuO2xXeziwALu0kHURvJagKWdGMCQk9z7XgKeHDw3e9h9IKos6dstjS4P99g_AptAg2uAAo6-X57hskuEHAT7jsethzaoAc5bPfk1XIzUY9FfWgslthLCE8ikV7moFPhbN-4Rr6eNHXmfJVdM-EOvmp9RNR5YoJTvopSi3U7VyFwBv3_WxQyqGIlmpS0FfJnpXWS5RAu_6YVPK2BujloWxDPGgShaXLxXsznCv5sSb0CkTNk5IrQulYFJ6K7riqtbc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2adjpmlKvYAqjOlMeookgrpEZ1g86oAPh-x-eGJauvSp0o3FeQTlGG7-yUtQ4QSVTMpNgnJXIy1fJoplO-7OJ8zJEgzEdtR1eVt9aKEB0f6n-1GETQrYQNM,&b64e=1&sign=9a185aaa3c95fb5e3827680a3330f8ff&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.8,
                    count: 807,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 25,
                            percent: 3,
                        },
                        {
                            value: 2,
                            count: 9,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 7,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 42,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 634,
                            percent: 88,
                        },
                    ],
                },
                id: 42604,
                name: 'LuxGadget.RU',
                domain: 'www.luxgadget.ru',
                registered: '2010-07-15',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--luxgadget-ru/42604/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 607,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 16,
                        },
                        brief: 'сегодня при заказе до 16:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/ugtnyPwAL5Edp7k9i1Nyow?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtsl-NMG6622NxT2bo_9zKecHq0Nf5iYL_6WujZ6zEF6pCerY8VX1pfYeN3m45piVfdHbfxkuRQDUF0pKytY1bHyuKvxgVC1hlXX4FYvU7hfjVHCJ1iAV0As6u3vnm8FbDixEkqfwGvoNrrG5WSU6iPF&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.005353318993,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.723862',
                    },
                ],
            },
            photos: [
                {
                    width: 607,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 173,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/200x200',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZENNligOUJ11PmHlHYID_JdB6QbVpJJesbnA_eTpIxtXQ',
            wareMd5: 'n0yqMv0dM8Juzc3cWtN-4Q',
            skuType: 'market',
            name:
                'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger (WXER-01) black',
            description:
                'Baseus Big Ears Car Mount Wireless Charger (WXER-01) – магнитный автомобильный держатель для смартфонов и других гаджетов, с функцией быстрой беспроводной зарядки.',
            price: {
                value: '1690',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSuIAIEPYSJyI-jiGKV_EEnVaFdPoRDMlrvlVvA_fW3pFpb5MWYg4RKsV0CT0QhH-2GSpIKLapEhz3cl0Mfxq8Jps7ZPuI_RoKr3AqTUZ-O-OfEb7hT_ENnkUmUD6dNzQS4Fv9AEY1ntxbRmzYsMWzESdbA_aPAzxj-CJin7qbxoU3Wt74bjPXIGYB1XcjDlLzIlFBOowSfnJtxoQoGUqduGPNpViTxgrvL5-KZ7_z1qX5qqjY38cA9Ms6xKTVG9_SMgEXq-jEf_eiJjsOtArpvlU7e1q1FWFOnuq4_frVcbDz_UlgELGYvrOlKPC1a78eWIKUG_gfLfyvEl-N6JXelzncJwvT3AXdH4aNw2G15ax9e_NgBJOdOi6rTTFwcyXEllA3h42AwRqMnn7EolSaxsL5BcrIHZlg8kvgW2Lz4mHc9zTTqAyS1RNjeVITNMndYi-mC03NcpHCmPncyf0j0WGqS--e01nLJy2BroxGZHalJpITNT8pmoBkVgDqlE5u-91giAayCbSqEi7aH-T_KXVhlaajfI3xiw8QlSz6toAtXBwolsEYRS9BBAQycWGSRoh1hThy22Qi0ENvBWATt7RkuaWiagBN3mMmTAUwiO2kKRiKz8-czrPMRwyDXo4ZBCcxuMzuCGlC1UmO6eebD-A7lep-c8-iDP1msh2Zxogbczh9szGhatNDtdVIQEeGId4tesbUy6iEYOQISyEQUPbpk3bGZEPHA,?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5CFSFrg3edj5xofFQnJMnMUG4UuzjPyoG6HO9iZesFu7V8hFNQPSP0h2q-ei2qE8kTae1EHfiuoMvrBt262-Ftf-AOuvbIryX1V2M_y9fsjWh_XiuB3ohRxIC4-v-8dI4-aErW4QNba0uq82BCjbA2IDLezWDbYaPvM1zjTfyemr_pykHIddmMIjVbQSV03xwrelIelvC-NSq041nixSz81xBkzOkyIA9YhfwlSgqQ8hnXB5Y1tJrvz1RD1-gnujyzB0qSOrpf4VHr8UuB79C-r4nHVqkfSvtH8zTTQAHdcGyrVjzZYcpx0zVQXgDR8iPhfcS3SuIWTfGZ9xiBORwoZCDcI6D_uymOW059Hn4h2T33RGUy92HJf_AmwUbvlm2VAQRm3zLFOksleVUW4Ft-Y-Hy8ekFZ8j3SHjNLWkVKTb_g8mpl2pdxRz1U4qalu7xPo6_a9mzoPClWGROgwyFIEOyZKo6zJXQ,,&b64e=1&sign=699fc79ea1c26165462d76dc0baec804&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSuIAIEPYSJyI-jiGKV_EEnVaFdPoRDMlrvlVvA_fW3pFpb5MWYg4RKsV0CT0QhH-2GSpIKLapEhz3cl0Mfxq8Jps7ZPuI_RoKr3AqTUZ-O-OfEb7hT_ENnkUmUD6dNzQS4Fv9AEY1ntxbRmzYsMWzESdbA_aPAzxj-CJin7qbxoU3Wt74bjPXIGYB1XcjDlLzIlFBOowSfnJtxoQoGUqduGPNpViTxgrvL5-KZ7_z1qX5qqjY38cA9Ms6xKTVG9_SMgEXq-jEf_eiJjsOtArpvlU7e1q1FWFOnuq4_frVcbDz_UlgELGYvrOlKPC1a78eWIKUG_gfLfyvEl-N6JXelzncJwvT3AXdH4aNw2G15ax9e_NgBJOdOi6rTTFwcyXEllA3h42AwRqMnn7EolSaxsL5BcrIHZlg8kvgW2Lz4mHc9zTTqAyS1RNjeVITNMndYi-mC03NcpHCmPncyf0j0WGqS--e01nLJy2BroxGZHalJpITNT8pmoBkVgDqlE5u-91giAayCbSqEi7aH-T_KXVhlaajfI3xiw8QlSz6toAtXBwolsEYRS9BBAQycWGSRoh1hThy22Qi0ENvBWATt7RkuaWiagBN3mMmTAUwiO2kKRiKz8-czrPMRwyDXo4ZBCcxuMzuCGlC1UmO6eebD-A7lep-c8-iDP1msh2Zxogbczh9szGhatNDtdVIQEeGId4tesbUy6iEYOQISyEQUPbpk3bGZEPHA,?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5CFSFrg3edj5xofFQnJMnMUG4UuzjPyoG6HO9iZesFu7V8hFNQPSP0h2q-ei2qE8kTae1EHfiuoMvrBt262-Ftf-AOuvbIryX1V2M_y9fsjWh_XiuB3ohRxIC4-v-8dI4-aErW4QNba0uq82BCjbA2IDLezWDbYaPvM1zjTfyemr_pykHIddmMIjVbQSV03xwrelIelvC-NSq041nixSz81xBkzOkyIA9YhfwlSgqQ8hnXB5Y1tJrvz1RD1-gnujyzB0qSOrpf4VHr8UuB79C-r4nHVqkfSvtH8zTTQAHdcGyrVjzZYcpx0zVQXgDR8iPhfcS3SuIWTfGZ9xiBORwoZCDcI6D_uymOW059Hn4h2T33RGUy92HJf_AmwUbvlm2VAQRm3zLFOksleVUW4Ft-Y-Hy8ekFZ8j3SHjNLWkVKTb_g8mpl2pdxRz1U4qalu7xPo6_a9mzoPClWGROgwyFIEOyZKo6zJXQ,,&b64e=1&sign=699fc79ea1c26165462d76dc0baec804&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSuIAIEPYSJyI-jiGKV_EEnVaFdPoRDMlrvlVvA_fW3pFpb5MWYg4RKsOi8TYNyeDPzCvGsDd8Zz0Rpb0FQ9Kk1eSglpKkQaqCKsfclXImxFwOkf2mEL5iz70GGdw8DlU-f48qKcRzELxFWcsmZxWqzBPoB-xj8z4ATSCJMKRs14sdZwvFCuhrH4OitkcsaLSe94x6iHAPtPlly0ByTNlrkRalpHQNhWlRBIdohnqbOL7uz_f7TjHPeG2wTmHhFNAG_RB-1Gb9SuBvSFRFMefbdbs2zXoPV9nzNJvoAQ-q71Bl5wgt-Q8qBZ-67-cwRvyuQfVnMh24o-81G5n5YN5C-8c60CW65ncZQd3iEhouN-kCJIIpD8U1tIZCcdYPRhZsVZtXl7m5Fp9Cm4I3FdOlcIT-qPSnDj1ShayIVd4CyndNCYGdykeQKyCgjLKHH26GX7Cuxon_NrY4Rsatcl3bQju5hmS5GglhYHw1TzlWx2KjTDShalUuD7y4ToxnrGqK5L2eWvgqHDy-CO-qooXT73pc309p6L8YPdgfyrpywNzDuA5tuecm_O9n0D5niJbqSa1s92lL0_g9CKimNraBxiyAGFnMAfrm6iEyH0UYAvf8CIgwrepzooyr5NnfDysF_Ap7eBvriHvpWvrxZmSQjRMZKt61Idu4I31wSpwG3xxLgM3HQYAibCzSSfYsNtlK5duRA1FEgVuozyWYcqjwgpDzSAU12snSg,?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5CFSFrg3edj5xofFQnJMnMUG4UuzjPyoG6HO9iZesFu7V8hFNQPSP0h2q-ei2qE8kTae1EHfiuoMvrBt262-Ftf-AOuvbIryX1V2M_y9fsjWh_XiuB3ohRxIC4-v-8dI4-aErW4QNba0uq82BCjbA2IDLezWDbYaPvM1zjTfyemr_pykHIddmMIjVbQSV03xwrelIelvC-NSq041nixSz81xBkzOkyIA9YhfwlSgqQ8hnXB5Y1tJrvz1RD1-gnujyzB0qSOrpf4VHr8UuB79C-r4nHVqkfSvtH8zTTQAHdcGyrVjzZYcpx0zVQXgDR8iPhfcS3SuIWTfGZ9xiBORwoZCDcI6D_uymOW059Hn4h2T33RGUy92HJf_AmwUbvlm2VAQRm3zLFOksleVUW4Ft-Y-Hy8ekFZ8j3SHjNLWkVKTb_g8mpl2pdxRz1U4qalu7xPo6_a9mzoPClWGROgwyFIEOyZKo6zJXQ,,&b64e=1&sign=bf2d8dea825715955275e2b4d207da1b&keyno=1',
            },
            directUrl:
                'https://topradar.ru/product/magnitnyy-derzhatel-s-besprovodnoy-zaryadkoy-baseus-big-ears-car-mount-wireless-charger-wxer-01-blac/?utm_source=YD_market&utm_medium=cpc&utm_term=t23389&utm_content=Baseus&utm_city=yml_msk&utm_campaign=msk_',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcLgICx7WlinEHgQ4T9PzgLziHB4I-ivoe4gv9V25U5P2VnzjSDnY8YQv2Ub7QMO4njKUCe3UwPiSx-YNBARAdKg8hD8vJVtmM0_7i5S2CK-4kQsB2Rgnj7dWDtFBY-PxUJ_YNiy69nVy8HjUsYPcQ9FoIcJ0bwrKDbzPmnfkNSAvrsUXU6NsoVv-8qZaOVNITtCjvjGREywk7c4PCVnYfPhZAQGP2kTb-03AWywlHC0cjSChEZS0oFqOB9OAW7clpxDDidWE-VNjhUyUIWPUetwUzlUH7ajjq1eqFcCOcqqQqTSbNwcGYxQe6xnR-OInto0jXt3fJPTgsPke9vwDsULl5LQ7tM_vp3XvYpozNhrqBgAjpBnv_8UzAJptrwW0GV_umwPCKc0WxI-rFOr9EPLxMjpG7yehTPbiKl5BSkKufz2SFh_vByA374zjDOc8N1d1VyNVYXMMalTiBqarf0OiYneY5tFzmojiEh-MiDBtFFAFl4bSCAA_fVSXPNgraKGmZSxGxynvxFGOMGQx9bXWn04HcKr8yI5cCWoZ3FF21GJXgaHiVGGZ2oLWzn9nPSICJBVjTJz5AeTfVmSp1JOWo5GSUv_ZhLWhbW4dKJTFtJB2oDHEv1TTcJxgXBao93eK7v84h8qTITTtjKzCGVp2nO1_9aK0rEeOcQUneUrTi_rgPnstHqJDM-ZgU6xZjm3JH-wuXs-ANL8b5STBMRsH-fu5ztQ9DL3vhCsTdW3GJgTPc6UBZ5LMs6aUmUcT71J0k4-0XCq_ecfyIgpD2rKT4mAN1b_zBXQF4kkUmXOgSOzimLfIj67iSCHLjBQanB_XTK836BSzy1hoHzgnPf2Y7e0I4I9Z7K-hXYkg2WPAXWlubVBC14Nna-hDQUZZiMitQBuMRIMp9MWxhScDx_aFEGFDM60dj011mKs-3JIEIdu5aLw5cRg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e1tabMJuQ3HKSSFijUQl2jS0tazhAi8n2-imjHUT9MXAvZ-ApiflUusPBoJt5z2rTb8tN8NcqDbwtXoQ8uc_Ve16AeZbPEiClD3Hr84l7fZGUPoMQqHowg,&b64e=1&sign=af2f52e2167140d828cbb14d155291a4&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcLgICx7WlinEHgQ4T9PzgLziHB4I-ivoe4gv9V25U5P2VnzjSDnY8YQv2Ub7QMO4njKUCe3UwPiSx-YNBARAdKg8hD8vJVtmM0_7i5S2CK-4kQsB2Rgnj7dWDtFBY-PxUJ_YNiy69nVy8HjUsYPcQ9FoIcJ0bwrKDbzPmnfkNSAvrsUXU6NsoVv-8qZaOVNITtCjvjGREywk10Vs2mO7C3l8sT7vGqPaTUJ6wZH1omddYOpw_xUQkRJFKJ5L1Nbsmo4y1iitWG9nHZLMIkavNHl4R3oYGb92_Wt-kwwOpBoTR3LHM1UvITRQv_Rrz3i857oL2kiIeAurzeff_sz_TTKVJomP49RQNOZYxsufi6eL0hYsFWuEVYwOCW-oyzn1wBG8v4eowhC3oFoU8x5OFYvBtPPsebMc3z_drYfG_p6K29E8sQBPOUGiFJEXCM-veD7G1s45FMOdm8bal2yAN-SZuDe4eHUCRbgdCJAi3pLJcCMT3lN1c-NMApuOXEGonIeGBfZ3GtH-uXYy2SM5o_Z-NN0xuAx34tb8wlF4Oiu2nSBq_wR2Hy9it6_sbjIDtBZWMOTRtbEPnqRlzNz-Rgt-MfeaB364s0P6OY2CBHtz3a1l0Xo-I0kZjHibA7avTPpS_LdB9Id6vh1IOLLJrIryzOmtU7xYsMa9kVue-6P46_saWa6JJGvGVtQkiUurCSg-pjiztMvldY3GcAw2NG64Fef2Rie_kFZkV55TAKJGCirDlsm7A0oH_iwsOUK57y7s2ZrbmMKP64B8N42tlnAt5k1qnNRUsH0XRUk5qd68NNkA5gKe_gzN41A6l7LowNDXmbuvicr8LZTg2ZTcyK6mSnksww5LYOj5ywiT6fpJ4iyC0Puu3UB9eeVgy7eBGUC2iWJ2Yloz2Eb4plwKVpzodyWTuMtqTMmGjgqT-ovTDCz2kvhPWAc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e1tabMJuQ3HKSSFijUQl2jS0tazhAi8n3JpsxLevdwunpXI9qx54lt3mf9AHZPU-9NNId9AlQWSIjwjOfrfHJ6rIqJFLOw201fiuJT9CsPSZxPwLFuPhag,&b64e=1&sign=e885522bea5009d92bcb86bbc26215cf&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.8,
                    count: 5426,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 5,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 4,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 18,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 148,
                            percent: 4,
                        },
                        {
                            value: 5,
                            count: 3870,
                            percent: 96,
                        },
                    ],
                },
                id: 65404,
                name: 'TopRadar.ru',
                domain: 'topradar.ru',
                registered: '2011-06-01',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Барклая, дом 8, ТЦ "Горбушка", павильон 253, 121087',
                opinionUrl: 'https://market.yandex.ru/shop--topradar-ru/65404/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 871,
                height: 1000,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1882989/market_Z0J0xzYfUrU3_QGwf0nu2w/orig',
            },
            delivery: {
                price: {
                    value: '350',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 17,
                        },
                        brief: 'сегодня при заказе до 17:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/n0yqMv0dM8Juzc3cWtN-4Q?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtvAUeA0pcOv-jp6x4XBFnhxCo3b5wEndzl5WcJb5rRXtH_un-G5FA3fp-XCMfjupfpsqCVI01QboU03srtdqJuyYsdnGb_U7BLv4n3Dq9EACItUOThzrxtX4tjCb46SO7JLcIoD9-zozRd3asSyzUv-&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.003030044725,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.722853',
                    },
                ],
            },
            photos: [
                {
                    width: 871,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1882989/market_Z0J0xzYfUrU3_QGwf0nu2w/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 174,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1882989/market_Z0J0xzYfUrU3_QGwf0nu2w/200x200',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZF9_oFaEMv-7SIuArPlJFxBg3INm-9WNDL7JBFwzPpwYw',
            wareMd5: 'oVAS96BBtyHLqFTx71jwkg',
            skuType: 'market',
            name: 'Автомобильный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger black',
            description:
                'Автодержатель для телефонов и других устройств в комплекте с быстрой беспроводной зарядкой. Ваш мобильный будет показывать маршрут и одновременно подзаряжаться. Производитель: Baseus Модель: Ears Car Mount Wireless Charger Номер: WHER-01 Надежная фиксация 4-мя мощными магнитами Возможность зарядки смартфонов совместимых с технологией QI Не блокирует сигнал Тип крепления: на торпедо или в воздуховод Вращающаяся 360° градусов подставка Материалы: алюминиевый сплав, ABS пластик В комплекте: держатель, зарядка',
            price: {
                value: '1692',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6a2pCQdb4pr_tXD_chh2QRmsZGctP-atFIi3HfGgPU8v_sGxPO0g2zDmJQAm1Ls6rk889Dq2OZKtP7LFQ7OmCsW7ewiYidV5YJIQKSinz6xUbH9insFWD8yS5fX6izm5jkX5hPoHsN8v4oxYjKUvsXrW4-dzjYI46xVpTKcOlrSpRPFndrj4n7dP_SgXuXBO8aZ_EZx8nYpfcIUPzElW_t5WNchJYn6ktCMBJXY226Hc9nVVJ6GWom6k7nDemsgP14bYrUJ6PWvU16KT0kE_brKJt7Br6eezJw9Xx2ke_50J5vAtFOQV_S8MeurinC3vgw7tmP5DBXPqoGPiieWvuFIMOdZtx6Rinjm61tiYMQDjQIARJWsgOlF4r-xFbvcC65SsQhod3z90ufUDIjfEm-dwJBKJj4Uns4pTG7-_uqiPpyd_LfCKizkz-gbhSAFOcfhk1lIjnoJhNp7_NUm9Hv7N8x_AyqDlZ1guL25kOT659aICIPo8KK07D9JR9OVIOoJIeJSw3xv5wpwCmssWhRsGPGUYVxfPaD4s5SW-vucalzdYqI175XSk7h0cxDtuB_irmPipPlRlosMlmi_d2YDcZ1NQkPSbDW_-SHHoz3MBB94gQKf1WzpDImVLXZfwHexdubuVx_Jn30FP6JkcVYCgzYsBhbGy3QQ34DIQ6TaJ592_nqauLxgyI8XgEshlv0IIJoksAPYxxHh0MBDa16MgCNVfHP9QAQ,?data=QVyKqSPyGQwwaFPWqjjgNrDBnFqmirtJJcO2TynOs1LyujsfCyGRQhEV2WpA3_w5f3J_HDa-kieOWADapFp0lqw3UHkXZNtsy_gNW0uMcG7m0pwyAJUpPSsdwhrg2IekCFyvJlO07NhW9IP6Hkllsq-hGR68VqXMG4b6_UUQU-bffw94SInoVJ6tinxpc7UskfB-sI2rMRT33N5SXC0h62kUR4_P_iYfcTugHnqh6SuSut_AjhmjA_1zexecJyb4ib1Z1v56rWyl8n0mfZCmUw,,&b64e=1&sign=3fbf6f057b36441232a54f5b482457fd&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6a2pCQdb4pr_tXD_chh2QRmsZGctP-atFIi3HfGgPU8v_sGxPO0g2zDmJQAm1Ls6rk889Dq2OZKtP7LFQ7OmCsW7ewiYidV5YJIQKSinz6xUbH9insFWD8yS5fX6izm5jkX5hPoHsN8v4oxYjKUvsXrW4-dzjYI46xVpTKcOlrSpRPFndrj4n7dP_SgXuXBO8aZ_EZx8nYpfcIUPzElW_t5WNchJYn6ktCMBJXY226Hc9nVVJ6GWom6k7nDemsgP14bYrUJ6PWvU16KT0kE_brKJt7Br6eezJw9Xx2ke_50J5vAtFOQV_S8MeurinC3vgw7tmP5DBXPqoGPiieWvuFIMOdZtx6Rinjm61tiYMQDjQIARJWsgOlF4r-xFbvcC65SsQhod3z90ufUDIjfEm-dwJBKJj4Uns4pTG7-_uqiPpyd_LfCKizkz-gbhSAFOcfhk1lIjnoJhNp7_NUm9Hv7N8x_AyqDlZ1guL25kOT659aICIPo8KK07D9JR9OVIOoJIeJSw3xv5wpwCmssWhRsGPGUYVxfPaD4s5SW-vucalzdYqI175XSk7h0cxDtuB_irmPipPlRlosMlmi_d2YDcZ1NQkPSbDW_-SHHoz3MBB94gQKf1WzpDImVLXZfwHexdubuVx_Jn30FP6JkcVYCgzYsBhbGy3QQ34DIQ6TaJ592_nqauLxgyI8XgEshlv0IIJoksAPYxxHh0MBDa16MgCNVfHP9QAQ,?data=QVyKqSPyGQwwaFPWqjjgNrDBnFqmirtJJcO2TynOs1LyujsfCyGRQhEV2WpA3_w5f3J_HDa-kieOWADapFp0lqw3UHkXZNtsy_gNW0uMcG7m0pwyAJUpPSsdwhrg2IekCFyvJlO07NhW9IP6Hkllsq-hGR68VqXMG4b6_UUQU-bffw94SInoVJ6tinxpc7UskfB-sI2rMRT33N5SXC0h62kUR4_P_iYfcTugHnqh6SuSut_AjhmjA_1zexecJyb4ib1Z1v56rWyl8n0mfZCmUw,,&b64e=1&sign=3fbf6f057b36441232a54f5b482457fd&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6a2pCQdb4pr_tXD_chh2QRmsZGctP-atFIi3HfGgPU8v_sGxPO0g2zDJ419zG_MhwqJlAi-BbGrKzSNHPvrEtrGop0VUaKR_NcnhRBlD1HOrs0L_fgxaR2xNHZp-7ViEuo-keRCN9sZHNa-XNlujUtcvgwkaILJsIXvziUb9rknnNLv3_yaBoWRylJUESiaJCI70TbipXgJwpP7IAonarb9KEqp522S5sKzQlOB4s2IF7UnpkE8DVuUhcHewzBiBwItaaO4XpRVerQ79BVLjMLoEIQ7R1FA3aRutN17jekVDA1ZX6iPc9OyN5Wk03utREEQk4z0mVWyXWIOlLkT0LZ4JuMPvLCpnZi9tarekrv0nzmweHER4yGnIJTV2jxoVKd-GV7XKwYWz93NgG1FzP_6cc4mcb9dcpHkNqReelCKAWxR83702U9woQOU_W8J4pwA8J8VdeW-I1lxlRtomiYvBAoZozpT-ORNtEcTYmITfqcFMNawVEYcoreQCgKjEE5x_QHXsfufuhjah5rRcjOBs6uVcUWBn7EwOP_m49z7E3pZOtQxQwgfQgBHhHc5QP13jI7W-9pxVfygFU5lOK2y50DdD9sn3F9qaltael-S8TKr7fzSUDb2XpoxmvKIAsYUXsqMa5aVBu5I8nLE1tugR0IDlG4J--QdqyFEiwvbjtCQvmV3DHg9viDGIQPRK3wLDMQO6MGUwhiaiyRS5Ssw9_Wx3QXp7as,?data=QVyKqSPyGQwwaFPWqjjgNrDBnFqmirtJJcO2TynOs1LyujsfCyGRQhEV2WpA3_w5f3J_HDa-kieOWADapFp0lqw3UHkXZNtsy_gNW0uMcG7m0pwyAJUpPSsdwhrg2IekCFyvJlO07NhW9IP6Hkllsq-hGR68VqXMG4b6_UUQU-bffw94SInoVJ6tinxpc7UskfB-sI2rMRT33N5SXC0h62kUR4_P_iYfcTugHnqh6SuSut_AjhmjA_1zexecJyb4ib1Z1v56rWyl8n0mfZCmUw,,&b64e=1&sign=9364a719fa7b76c995a8af11b5d7ed09&keyno=1',
            },
            directUrl:
                'http://www.routerstore.ru/16927-avtomobilnyy-derzhatel-s-besprovodnoy-zaryadkoy-baseus-big-ears-car-mount-wireless-charger-black.html',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWcLjgn1DyAfqu3-MFTpkftKucEFuvnOs6Q9s9oe3yOJnCxdON6X6mk7NSogcb35uC9L9akRzYW0IkDya7NrlMUDIUyo-xWJGUqKrBdgoPPxWh1ZDW4A3VsBQlUX2wh6EYq8MZ_Ai4AqWbFedwHo1pwhLinduV9FyQz6niHahODtuSmIhVgPTyyfAIgdZEQpc6B2zsQOKmXirFsU5flE1Uf_YHeUoHeDev4KQhd_n_uF8ih-PdeiGIexRLEUBN9P7_YTSqTGazE48QGTVaXBXQDqquPPUTtVE_LFCRN8SYjRb0D_8fLzrtbyXWAnTJM_uzjGQOIjWYq9UDefBrQgr30CPsSckGbykRQsvjZfdA2RkUqnt_RAlN5sXdKvtqVL575KgjGvw3BxkqmJqfRG91a0znZIQtTCrKcWfBxJTdSVeZh6ll8Pukka9DvyesFpFxemgW49nLLUBKhNhsUOf6mV0Yy857aEr_8ynDHWyp7T1O0aXi1tJRHDNCUaS82I1iXQ-xpC3eLQ7yJzoxDi_bqYFc_sebhrBbwIafVz2K1dRxF0WgFJR0872iV31Y4kykTcz4A4Ybb4zgi3KdMYr7VnOulYJtZpO4hyZza0BnlIUzao2R0Bs1aXLXcwut5PUZZt0rs_y9zxu_eNwN7uqG2xy1wHWkpKstl16A1a01Y0RDYkzd95PZ0tFgzFLYG6iS1MYEkruTEb4NltsJGwG--88chB215PvkL3AT3WduAlI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XpBbLtDcvrqPIYDuvKZLQ7UUy_PZRno-9Z_tp6cdaxci-hDnWuTcNvFRYX8NGCNNFNRBV3Ok0RUwG9HIYsrfd83xM76fclrtFJEwjgFqt1IbaIK1_VOKps,&b64e=1&sign=5fdc04f68b021fc4ae5657fb29f10111&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWzOI-PTak3UoOFKIIuYhafZkjyUAco3eo4FE-NEoEvvcMjZQAiG9pV0nZ_trIGECKmg1oAsaHEOUASJk5k2BW0gQiZbftOdx54mCT0PlTNfB7nOmIlv3iD6dm6ign9pX2phoFiKN1Bik5wWMRq1Nih943PNYFx8Fvd7jNxxZJc6nob_e8RDfbzMrEiF4Zw2fJi7Uh86JsP5Vzq7S8U4rK2dxRCiWe9R2WDgD9e3dWzFOBI1wAIFn86jxiFuRzXIg7lNlTruXmxWhuLZDCwg0TDuZ1zIosZ8scHPsnTXnnQm8FT11qShf9OSrSt0S3V2jKh8EMazhDLmBySdjg-L4my8R5Q_z1KWklxtoEo9_nL8ptZbKE71tRJb98xkq3kwr4p10jptM7z5QSDU-4k8X0UZt5RjvTXDSUIH8BhQgqMWPmLiV1Nfk0PK-q3EGkOKRcfDrccomxQOP1g5ajkvqKtLgAZ1dSXtObt7EbrAIDATHJGGI35ic6KxFBv4dPJL4So1aorZTLzMeWNS2lmwYHo-ExQ2w07AkH_6HSnWoufQQujYl6fddqhI6sI36puVbg_C4xTVeigC9A3Bv0ytjBGhApG2eCYPYkyXkMctrYbW_3drS6XJEC2q5rPRjLIcJE8c0NFzm_c9pzwBuOSpLNyiXZV3GWPL3xjDeaz6NFjRjkA3PlEoNeAcGkXRWauTnrs63Av740BuNTsyjFt5CTGetENB5XbxQ3aTZP66Pvg9A,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XpBbLtDcvrqPIYDuvKZLQ7UUy_PZRno-49P6HwcX6Hr8NGvwc7K5fRIYNcLoXKsEIpddBU5SeZSUhaeygbmlww3A-nguYhvk4-SQkCa9vTAx5SW25JivnM,&b64e=1&sign=85756fd8fa400ce79fc4e78528826011&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.1,
                    count: 124,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 6,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 2,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 5,
                            count: 86,
                            percent: 90,
                        },
                    ],
                },
                id: 142620,
                name: 'RouterStore',
                domain: 'routerstore.ru',
                registered: '2013-02-14',
                type: 'DEFAULT',
                returnDeliveryAddress:
                    'Москва, ул. Кировоградская, дом 15, ТЦ «Электронный Рай», павильон 1Б-48, 117519',
                opinionUrl:
                    'https://market.yandex.ru/shop--routerstore/142620/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 361,
                height: 450,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1394832/market__J-GOrIMXRklNGeN0hyqOQ/orig',
            },
            delivery: {
                price: {
                    value: '350',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 2,
                        },
                        brief: 'до&nbsp;2 дней',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/oVAS96BBtyHLqFTx71jwkg?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtvspRvG4x6ibxuQy36GqnR5Js31KE15HUYKDglvhXwXss5hoqdIbRKOmJwxcmUhR0v8OdEFkxenPjq8iNP8OFAQh2hv8NEqIJpuX6VztMMfyXFMlspvM9cTkCEZT-IfqqGAfGB8L0Ze9JQuFGfHWlFc&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.001630707528,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.722283',
                    },
                ],
            },
            photos: [
                {
                    width: 361,
                    height: 450,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1394832/market__J-GOrIMXRklNGeN0hyqOQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1394832/market__J-GOrIMXRklNGeN0hyqOQ/200x200',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZEUZ7nnFUiFnw_uCSdqc0itssErDsSENRKzoIrlPK-MpQ',
            wareMd5: 'EMc5lYYRPg8OdNwZ9vAbKw',
            skuType: 'market',
            name: 'Автомобильный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger',
            description:
                'Автодержатель для телефонов и других устройств в комплекте с быстрой беспроводной зарядкой. Ваш мобильный будет показывать маршрут и одновременно подзаряжаться. Автомобильный держатель с беспроводной зарядкой Baseus надежно фиксирует смартфон на круглой поворотной платформе за счет наличия 4 сильных магнитов, которые не оказывают влияния на работу коммуникатора. Зарядное устройство поддерживает протокол быстрой зарядки QI.',
            price: {
                value: '1485',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZmZPiHIqcrZvE7UC5s5Xwl6SNtPO2AWgpLA9LpG1uwLmy2NY_12JgUcXKDP6xuVQjXAjprv58JR3_5yOVg1c1xEOxor1FWnAIuzmfSr-N5FhDPcU_f-zlwfuik-rHa6dc7LY_fi-qORCMFrpvAi1xtnI_EWXbEJm7evYtLEY8EysdkE_9ncWB9zK7X22h2L1PwxSUNcGAf6UHGRnnQf2S2pkj5i6Imfs-hdqJ7uHXUyF18ZzP3aTuvbGE1SixEPKUA-sjv24hx3zf8RkmzDUZNdP2bfaYyA0PnxIt5cjYGLILvrtTXlom4qzVHIQD-oGCil6h5eVNc_OpVq2j_VvbwrPClEj948Tg4we68kOAOwgskZgkafg_III701Fbtq5JvdFBYFfPnGGQ1WMMj2O5ovZqFFSITONyy5MDZw5x4QAL6_MYxiE_UTRewVufZi4mK2YkNYZKDt5yageU7Xi6ukNnxnIVYDU6C0RECPpLWW2YFz1A2Foz5qUbd00DAuxyWQmksMLS-THL5G7ttcpLynWkETY0FKDJSaTEwowBucPDCq9vnIJ2SDbItFdkR-Dke2tVk5Jg9bt-O8wfo_qifAZ1J8GzOKrShUc5x6bwY64aQEpjTyaxF7js9_IG6qx36OtUurF-SyVtcRsYkefT-JqkQMjUrYgt9KphG8r9i8s5pCV8Q4DnMlaerprYr_b6AcbCw8XIg-a0BzhpYXmOlGOi4eHUwdq8,?data=QVyKqSPyGQwNvdoowNEPjatuYs1yVCcww_xZOAlXIJ-3fbnJdUIcnxnPnBHwFFunpcrTf8VEQAOBu3DSCdI1k850UhLVx9flvhDxLw5-IqxV-ukKeEcFHi9-2n8WHCAnZpbk1w4jzNTb5WD5MXRe8TMS9p5GWUHvdf3Z9Bsexxop28OMIv9iUpMCQ9Xssu6ywBdg46iJQR38SEDJkScMWX4wNHTdyVjcdZaTvMdopINFy7uooLvTKl8TevIDoyJy4n25mHRjdEG7N-Mtmd-Ldy8ZAp4QK3tRnfHgufhJEFrKkYQjTQv6ozLhKpYizwPfG9I-l5S41g9Wh66fVnkpkd3RhfO01UXbUYErOAIqcu1sIMo34sadLwFVnkQgkXp2m_Sm45MzD4V35CR-8OCRnVc1yTNXMHdvFhr3q01CMVlR42RwBfUDbqCU9udmuu0djXfWnRDJpoLDKUauAz-Lkr7oJWZZKaeoW9Lx3bm9s3pv6vEy4ZqH31g0BvRVn7Z5sGN5TmV93cH_vLWR-nRbhn51gQsjKOey_KJolg9Wx5Opyd8WC5PSJMEvTpgclufc&b64e=1&sign=e0f32e1c022dd8f58c2fa03885f78f8d&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZmZPiHIqcrZvE7UC5s5Xwl6SNtPO2AWgpLA9LpG1uwLmy2NY_12JgUcXKDP6xuVQjXAjprv58JR3_5yOVg1c1xEOxor1FWnAIuzmfSr-N5FhDPcU_f-zlwfuik-rHa6dc7LY_fi-qORCMFrpvAi1xtnI_EWXbEJm7evYtLEY8EysdkE_9ncWB9zK7X22h2L1PwxSUNcGAf6UHGRnnQf2S2pkj5i6Imfs-hdqJ7uHXUyF18ZzP3aTuvbGE1SixEPKUA-sjv24hx3zf8RkmzDUZNdP2bfaYyA0PnxIt5cjYGLILvrtTXlom4qzVHIQD-oGCil6h5eVNc_OpVq2j_VvbwrPClEj948Tg4we68kOAOwgskZgkafg_III701Fbtq5JvdFBYFfPnGGQ1WMMj2O5ovZqFFSITONyy5MDZw5x4QAL6_MYxiE_UTRewVufZi4mK2YkNYZKDt5yageU7Xi6ukNnxnIVYDU6C0RECPpLWW2YFz1A2Foz5qUbd00DAuxyWQmksMLS-THL5G7ttcpLynWkETY0FKDJSaTEwowBucPDCq9vnIJ2SDbItFdkR-Dke2tVk5Jg9bt-O8wfo_qifAZ1J8GzOKrShUc5x6bwY64aQEpjTyaxF7js9_IG6qx36OtUurF-SyVtcRsYkefT-JqkQMjUrYgt9KphG8r9i8s5pCV8Q4DnMlaerprYr_b6AcbCw8XIg-a0BzhpYXmOlGOi4eHUwdq8,?data=QVyKqSPyGQwNvdoowNEPjatuYs1yVCcww_xZOAlXIJ-3fbnJdUIcnxnPnBHwFFunpcrTf8VEQAOBu3DSCdI1k850UhLVx9flvhDxLw5-IqxV-ukKeEcFHi9-2n8WHCAnZpbk1w4jzNTb5WD5MXRe8TMS9p5GWUHvdf3Z9Bsexxop28OMIv9iUpMCQ9Xssu6ywBdg46iJQR38SEDJkScMWX4wNHTdyVjcdZaTvMdopINFy7uooLvTKl8TevIDoyJy4n25mHRjdEG7N-Mtmd-Ldy8ZAp4QK3tRnfHgufhJEFrKkYQjTQv6ozLhKpYizwPfG9I-l5S41g9Wh66fVnkpkd3RhfO01UXbUYErOAIqcu1sIMo34sadLwFVnkQgkXp2m_Sm45MzD4V35CR-8OCRnVc1yTNXMHdvFhr3q01CMVlR42RwBfUDbqCU9udmuu0djXfWnRDJpoLDKUauAz-Lkr7oJWZZKaeoW9Lx3bm9s3pv6vEy4ZqH31g0BvRVn7Z5sGN5TmV93cH_vLWR-nRbhn51gQsjKOey_KJolg9Wx5Opyd8WC5PSJMEvTpgclufc&b64e=1&sign=e0f32e1c022dd8f58c2fa03885f78f8d&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6ZmZPiHIqcrZvE7UC5s5Xwl6SNtPO2AWgpLA9LpG1uwLmy2NY_12JgUrmLQ3uipoIOmo7YuQYcdezTSil7heX2Hm-WbKWwLmZhEbp3AHGmzskO9bJUlb8cKAUCt8EdLLvTOgzneDJmFbv6mBEOE7CmysX926xyjjbEdWctxopAKLXdmrW0vSrozZXunjHw-17seYPOJXcUk44DvrSYjlRmXWC8mw3dz_OpGzlDlAaCE11ttf7wQT4Y9QGQMZyYTltyMTOXm5-IJm4KoI0DmL071RTMV3OAHxPf7DkXnhtJPNfGNTIZ1hVVqtlXyi1meR3l_SWmMAZ9H2KDLqQBOD-Cw1uOkuQOsK7n5UCgjoZJzykNAvNkuotze7nmFfJHhvPUs_iimrWjXhCT0h9NQ7UoyGE7QQlvRNgTfEMEDBxZbCCpTlVQqaaMIfhOu69Xh2zAw2LiK_1T9VdCMbYXNlL8SYX4Zlc82l2hV3h6rGkDGuooYXZOC9HuitazhO4inBpctPlD36inCOjQPbnOf-KQzJbJTh2y-Y0QCZnPPVVRXx4bnSd_1cSeO4JYHIQ9SmfE1zsC2lk3nZVNau2tYD88t1C0MmSUN4VksrxbZhfFrpJXzjvPy1yRN5h8DvGNwkGMPlSJFxTxM2gqB-4hCTMsyMUalabo2WBGi2SJ7h12DUnnIfCcJ7wM0JctTvtoyIa6tgPRs0VhyKH_EwH6y0gpRNNoThAvmP7w,?data=QVyKqSPyGQwNvdoowNEPjatuYs1yVCcww_xZOAlXIJ-3fbnJdUIcnxnPnBHwFFunpcrTf8VEQAOBu3DSCdI1k850UhLVx9flvhDxLw5-IqxV-ukKeEcFHi9-2n8WHCAnZpbk1w4jzNTb5WD5MXRe8TMS9p5GWUHvdf3Z9Bsexxop28OMIv9iUpMCQ9Xssu6ywBdg46iJQR38SEDJkScMWX4wNHTdyVjcdZaTvMdopINFy7uooLvTKl8TevIDoyJy4n25mHRjdEG7N-Mtmd-Ldy8ZAp4QK3tRnfHgufhJEFrKkYQjTQv6ozLhKpYizwPfG9I-l5S41g9Wh66fVnkpkd3RhfO01UXbUYErOAIqcu1sIMo34sadLwFVnkQgkXp2m_Sm45MzD4V35CR-8OCRnVc1yTNXMHdvFhr3q01CMVlR42RwBfUDbqCU9udmuu0djXfWnRDJpoLDKUauAz-Lkr7oJWZZKaeoW9Lx3bm9s3pv6vEy4ZqH31g0BvRVn7Z5sGN5TmV93cH_vLWR-nRbhn51gQsjKOey_KJolg9Wx5Opyd8WC5PSJMEvTpgclufc&b64e=1&sign=0d636460c6795cb12d8814350bae8623&keyno=1',
            },
            directUrl: 'https://unibuy-shop.ru/product_by_id/150317922',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.2,
                    count: 75,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 6,
                            percent: 11,
                        },
                        {
                            value: 2,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 5,
                            count: 50,
                            percent: 89,
                        },
                    ],
                },
                id: 553660,
                name: 'Unibuy Shop',
                domain: 'unibuy-shop.ru',
                registered: '2019-01-25',
                type: 'DEFAULT',
                opinionUrl:
                    'https://market.yandex.ru/shop--unibuy-shop/553660/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 436,
                height: 481,
                url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_DmWgL-rPOBhSrJLpA-bZEA/orig',
            },
            delivery: {
                price: {
                    value: '350',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб.',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 1,
                        },
                        brief: 'сегодня при заказе до 1:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/EMc5lYYRPg8OdNwZ9vAbKw?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtsfeKB-ehAv9uBbsjgk5_bgolKE7eBLN984d3gDM74jFR3GJ0F-YEQFsDlCRVsvnXiSvD5xONV_7BJ8ndkSe4YEhtw3TB-fp_QjPEFWBup-IZhgdK0uT6Gn4jvLxNvI2050icBnE4ZDU-656hTCsraB&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.003721754532,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.721682',
                    },
                ],
            },
            photos: [
                {
                    width: 436,
                    height: 481,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_DmWgL-rPOBhSrJLpA-bZEA/orig',
                },
                {
                    width: 394,
                    height: 491,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_BqPphiXv7Ao74o9LzBZ2Pg/orig',
                },
                {
                    width: 441,
                    height: 487,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_PIsr7aieg64kKqDTFV2IFA/orig',
                },
                {
                    width: 437,
                    height: 453,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1062628/market_3064fJ5ritKshA7OnW8XYQ/orig',
                },
                {
                    width: 434,
                    height: 473,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1584911/market_7Bi-Fpo2yKDfBZaALUiK2g/orig',
                },
                {
                    width: 581,
                    height: 585,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1600211/market_IMI-OTycT0RjUzb7hAWzTg/orig',
                },
                {
                    width: 520,
                    height: 469,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1598130/market_bq9dDD47ISZjOHowVwV7CA/orig',
                },
                {
                    width: 514,
                    height: 466,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1857661/market_oGODB_sRv4aTuzR5kdYB4Q/orig',
                },
                {
                    width: 645,
                    height: 571,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_MrBROWDYCSNG-7Kku6EbfA/orig',
                },
                {
                    width: 581,
                    height: 585,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1720090/market_562TaZxcn9dJB9ZYim7eFA/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 181,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_DmWgL-rPOBhSrJLpA-bZEA/200x200',
                },
                {
                    width: 160,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235547/market_BqPphiXv7Ao74o9LzBZ2Pg/200x200',
                },
                {
                    width: 181,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_PIsr7aieg64kKqDTFV2IFA/200x200',
                },
                {
                    width: 154,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1062628/market_3064fJ5ritKshA7OnW8XYQ/120x160',
                },
                {
                    width: 183,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1584911/market_7Bi-Fpo2yKDfBZaALUiK2g/200x200',
                },
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1600211/market_IMI-OTycT0RjUzb7hAWzTg/120x160',
                },
                {
                    width: 190,
                    height: 171,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1598130/market_bq9dDD47ISZjOHowVwV7CA/190x250',
                },
                {
                    width: 190,
                    height: 172,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1857661/market_oGODB_sRv4aTuzR5kdYB4Q/190x250',
                },
                {
                    width: 190,
                    height: 168,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_MrBROWDYCSNG-7Kku6EbfA/190x250',
                },
                {
                    width: 158,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1720090/market_562TaZxcn9dJB9ZYim7eFA/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZGXqTGSM19-h3y1OJ1v1eETrYdCzSw8VaaEWazd8GK1zw',
            wareMd5: 'BQqejt5oGXmmemXXigk7bQ',
            skuType: 'market',
            name: 'Автомобильный держатель для телефона с беспроводной быстрой зарядкой Baseus Big Ears (WXER-01)',
            description:
                'Автомобильный держатель с быстрой беспроводной зарядкой Baseus Big Ears предлагает премиальное качество и дизайн. Магнитное крепление надежно удерживает смартфон, беспроводная технология QI обеспечивает быструю зарядку мобильного устройства, а возможность крепления на торпеду или в дефлектор обеспечивает непревзойденный уровень комфорта при использовании. Держатель совместим со всеми мобильными устройствами, поддерживающими стандарт QI (на некоторые модели необходим дополнительный ресивер).',
            price: {
                value: '1890',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSsf2uU4iEkkeH7u_qxZdfP-Y2U3_g9p6QpfXC_Tq18IZe-e8s0k7DkPRC2Og-NFcNP2PxNsB70y_c62z7dUdKugBMwlMWjAwCXmB7VlDeopXfkGEv7kMIYV7aV_spfOTReSKnfBf2CcJgUg3Tmp0_zMoXaPnkSYgkd9c3ePNsbIfzcLyLcM4kB4mx_nOXisx4oHiRaSisx7SABqqRDZ7x-i7EZIz5ONpF5LlcB4mrfTnn6GRXH0PYzdIqC6Qxk9UTCkUCuKfkILo5dkAa1teNY1XjlDjZM1gLtINchjjxAIkg6xu1VG1yDsuaWXT9-7E6ElWfMyBt8-HExoU9EVyVOEnHb1AMpEjFXb2lgPg4LHmGIyQcN-X_bab5c2YPEFCElGOe4J3wRd1n1vmTMqGfv0nH3xdGYP1UR84fvRxunh4684uqW9csbGEdPmF3HH2m1HCMCotK5DKtRH1i638FMgo2yN1Btmr5xXK85RhK7ysg3b2gZ_JPg3ur2H9nKWzO4AcActsCLnoWlg-05GF9mB7QASxQxD45FNlOSV-VkK7QUZLXcS5MDyzpfssBuVw_Sh7kFjuQWMChd-cQnwleVYHpktDpEZ5DU2L6s0Ui98ppdqXTd1RnlC3mwLPinlaOl4LOx_L3Qg32dEC6WoSt29tElyDMAkdcsEBt8AjUfI2qisdYVuLUeRlFJThQDNeRcTqHHAohtmK8gNtNR08U25QwRiCUe46QU,?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaB_tIgkECyAPFqQ0E9NkpMXg_GIsxolqz2W4sM45tVcvMYKVmL3AnEh5-h8M86a9lAURjHKLsC-F32yjKc69ZbHPGlJzVr5dvln77fxWJ-N6CAk5GpSGQdivQ7Z8ed3nOBGaRwQPtpGoegmhQXJ8RkPmgp4JxUttqnYyBuoUWkzu0G6InDhRg_je0KcrLs4r9rR3MZ_y9e4oTUA1J3bG52BAyg5xVovjtsc4RxV1m68hzSNGKdZXxgLSJ-SeGLUSB3n4HpbOaDTJIydrcCChfUaaNmSn7SCKVo7CK8ccADCb8FRvvkqzRNba4G8wtzTaLkHTSq6MT2QjvAa48mettWvkDB1cdSV02a6mQBQjU6xAOGFYrLEC7226_v1GjuJQ7tFsYQd-pRocMBGJdNm0IbXOwdpgv13n6igJnA1lna5Uyz4EG4jWmls5zmLbREUE-wx8V1wsv12M4PuBRJBUkPWFCwq5lz3yVa7ULoPVHtmACDCd5laSuqYhlV8-R94dYifk1WZUmBbpa_biq96oK4FS5x9WMwUF52q4HqrFdNpE,&b64e=1&sign=a483221092080788071eb5f7b1c49635&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSsf2uU4iEkkeH7u_qxZdfP-Y2U3_g9p6QpfXC_Tq18IZe-e8s0k7DkPRC2Og-NFcNP2PxNsB70y_c62z7dUdKugBMwlMWjAwCXmB7VlDeopXfkGEv7kMIYV7aV_spfOTReSKnfBf2CcJgUg3Tmp0_zMoXaPnkSYgkd9c3ePNsbIfzcLyLcM4kB4mx_nOXisx4oHiRaSisx7SABqqRDZ7x-i7EZIz5ONpF5LlcB4mrfTnn6GRXH0PYzdIqC6Qxk9UTCkUCuKfkILo5dkAa1teNY1XjlDjZM1gLtINchjjxAIkg6xu1VG1yDsuaWXT9-7E6ElWfMyBt8-HExoU9EVyVOEnHb1AMpEjFXb2lgPg4LHmGIyQcN-X_bab5c2YPEFCElGOe4J3wRd1n1vmTMqGfv0nH3xdGYP1UR84fvRxunh4684uqW9csbGEdPmF3HH2m1HCMCotK5DKtRH1i638FMgo2yN1Btmr5xXK85RhK7ysg3b2gZ_JPg3ur2H9nKWzO4AcActsCLnoWlg-05GF9mB7QASxQxD45FNlOSV-VkK7QUZLXcS5MDyzpfssBuVw_Sh7kFjuQWMChd-cQnwleVYHpktDpEZ5DU2L6s0Ui98ppdqXTd1RnlC3mwLPinlaOl4LOx_L3Qg32dEC6WoSt29tElyDMAkdcsEBt8AjUfI2qisdYVuLUeRlFJThQDNeRcTqHHAohtmK8gNtNR08U25QwRiCUe46QU,?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaB_tIgkECyAPFqQ0E9NkpMXg_GIsxolqz2W4sM45tVcvMYKVmL3AnEh5-h8M86a9lAURjHKLsC-F32yjKc69ZbHPGlJzVr5dvln77fxWJ-N6CAk5GpSGQdivQ7Z8ed3nOBGaRwQPtpGoegmhQXJ8RkPmgp4JxUttqnYyBuoUWkzu0G6InDhRg_je0KcrLs4r9rR3MZ_y9e4oTUA1J3bG52BAyg5xVovjtsc4RxV1m68hzSNGKdZXxgLSJ-SeGLUSB3n4HpbOaDTJIydrcCChfUaaNmSn7SCKVo7CK8ccADCb8FRvvkqzRNba4G8wtzTaLkHTSq6MT2QjvAa48mettWvkDB1cdSV02a6mQBQjU6xAOGFYrLEC7226_v1GjuJQ7tFsYQd-pRocMBGJdNm0IbXOwdpgv13n6igJnA1lna5Uyz4EG4jWmls5zmLbREUE-wx8V1wsv12M4PuBRJBUkPWFCwq5lz3yVa7ULoPVHtmACDCd5laSuqYhlV8-R94dYifk1WZUmBbpa_biq96oK4FS5x9WMwUF52q4HqrFdNpE,&b64e=1&sign=a483221092080788071eb5f7b1c49635&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpRZuWLr1RKmXehTp2arP4_DmYT0dIpHzKXHoeUoNBFNSCs1gZGkVUZxmh2J5auEHZqUO3VSH42eDLCau5EhE8TvytTaOu1OOdkD5AggTAscgDfoY0WJGEGxWm_1X2EX2C46w1g1JgLnzG08KbpPnqQyKfdWZfZ1pgxWICzSNOAoJeHMuC9zGKEvo7FEvAP--gIJbsi5e_I8S464LaTUQSsf2uU4iEkkeH7u_qxZdfP-Y2U3_g9p6QpfXC_Tq18IZe-e8s0k7DkPirnTER0pEEU8_E0xQgkIxl5KP3NVTZG1IYo8NMd_C6VksxBIJ0H1urgL0kYWG5qZSCP7XgzVgksIDUJrXgQtxMSvH3B7IKJwuOspTTbqRV9b9675MPJb-rRQXiwG4X0ZuIpAVaLYpKwqHD81cptM9mdbhzf9RS1ppV6cEwCRSS-vTb6T3nceSjuU5EVwS3_RL_pMFf0ZCvAfBEqJObwGuyKFpZj_rkfDsg69-MnK74sPiFwuajU7MWuyF0Mah6sUpSin4zu8AVaMXxObh2Gt_quiT2ONA58fIX3b3tQuswgiiFIogbqtUdizY2rjuE8NxyBanDo5L0jdTDofBl-_z6VVAgO1MMG53tmlEfaAzRemCag2BmU40ipQxeUueCOXMtegWq8MYczde63nmrLpkjVDLTsqiu4u4wGH2eDBBCbtJF73-fRx3Ad5uc6ELgKEn32a55xaY1k0B8fl4ijgkOQYf7Pjru2RHcSIa171JkEP2RdDLM46z4TAV0ul0Yy5I-JYJ9zlsv-hCpvbm6I3IFPSFUpHiNWZc_V7_dEZjbohbvjFvTxeI7L-sKVI6as0EVFplZdlQyL6j_N2_Ldtm4GciUIDwGfHqBdGuMw8JcK3onTRo9n3D0OuROaF--mk61va3m2VDan6HeKtUPh53ATIF_rK3pWQz8H5cfTa0TY,?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaB_tIgkECyAPFqQ0E9NkpMXg_GIsxolqz2W4sM45tVcvMYKVmL3AnEh5-h8M86a9lAURjHKLsC-F32yjKc69ZbHPGlJzVr5dvln77fxWJ-N6CAk5GpSGQdivQ7Z8ed3nOBGaRwQPtpGoegmhQXJ8RkPmgp4JxUttqnYyBuoUWkzu0G6InDhRg_je0KcrLs4r9rR3MZ_y9e4oTUA1J3bG52BAyg5xVovjtsc4RxV1m68hzSNGKdZXxgLSJ-SeGLUSB3n4HpbOaDTJIydrcCChfUaaNmSn7SCKVo7CK8ccADCb8FRvvkqzRNba4G8wtzTaLkHTSq6MT2QjvAa48mettWvkDB1cdSV02a6mQBQjU6xAOGFYrLEC7226_v1GjuJQ7tFsYQd-pRocMBGJdNm0IbXOwdpgv13n6igJnA1lna5Uyz4EG4jWmls5zmLbREUE-wx8V1wsv12M4PuBRJBUkPWFCwq5lz3yVa7ULoPVHtmACDCd5laSuqYhlV8-R94dYifk1WZUmBbpa_biq96oK4FS5x9WMwUF52q4HqrFdNpE,&b64e=1&sign=4b86a1ca1d3f5b3e514a0ae2ff927da3&keyno=1',
            },
            directUrl: 'https://www.audio-drive.ru/catalog/233/14246/',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcLgICx7WlinEHgQ4T9PzgLziHB4I-ivoe4gv9V25U5P2VnzjSDnY8YQv2Ub7QMO4njKUCe3UwPiSx-YNBARAdKg8hD8vJVtmM0_7i5S2CK-4kQsB2Rgnj7dWDtFBY-PxUJ_YNiy69nVy8HjUsYPcQ9FoIcJ0bwrKDbzPmnfkNSAvrsUXU6NsoVv-8qZaOVNITtCjvjGREywkUE6XBvaMNZzBi0G9EXrf0dmyvmpNaT1Wk4t0YTkjJ1wnmIcwV65kQabulrv-UmZztuVgdXMRDC1BXfc7r2Hd4H9J5093HG1e7gOi4SzZ3FoKnCfGgcfanGlmIhmUGQpChtIX13H8r8aL21xanz146poyMSbd2ukPKAk_G4egpamKEF86jg7XvCJZIPC6cbtD0lrTypQWprd3eny-g0LHIyUyWGv-Z0RTYpBBsbWaKifzGTVXF1lwzP5NHWF4rvsWjCvqWrZunYWKtxLR7EueAFvqSXPPwhTzu7mt69tqunN_WxHTXdA5d-WLQeBq5vlYHkMLqiiqVIJS7SfcP2kz_DkCa-PYTjBs05Rui0kkePvS5CEVDeGDCZ4AvROpn39ed-jBYjXmx7i7FugQtQU-2HPIgzmgGD9cfSRUsBw5vZkHPT2ok0vAukBK2i8cY1I5n6lfaAdEoZzG0p6zxYE6huECd5RM_BrkF3MLJRBB79zk2k0cJXnVLYKfv7ZLKeb3rUa3acndNWQii3XRjsfcqj9iZ1Xwp6abseecylt29XN9s_Bs_quA2Osdg0TboYDNu2qIiTavKxXzBkUn7BPFvkCelx2OGHvXhUV37sxeqSVQo0IQXtFSenI6atk2jdHm3cnB7uJQNy0QN8l33w2UV9Nrw97nvTl6GsYQfMPd0_vgIGkYh-CW3se7HRTp2FVdOQnhE_aYjXeiXDS9lb9FEKrm1DH1OUBCKB82Ln6Bg4c,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XkML0AvZJ_T66Fv79z32RtWCMa9u1iwcTxrzqSZw0HGHNBh1Hz2_VOhUNVCpC3UfN8d-unlAsutqwRttBy23KLTjTA7wDbBmuw5sOq1IYf7xolnekUPUB8,&b64e=1&sign=2ea709a233bf4e0986bf2a8a32a8544d&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcLgICx7WlinEHgQ4T9PzgLziHB4I-ivoe4gv9V25U5P2VnzjSDnY8YQv2Ub7QMO4njKUCe3UwPiSx-YNBARAdKg8hD8vJVtmM0_7i5S2CK-4kQsB2Rgnj7dWDtFBY-PxUJ_YNiy69nVy8HjUsYPcQ9FoIcJ0bwrKDbzPmnfkNSAvrsUXU6NsoVv-8qZaOVNITtCjvjGREywkNMZC9yRP1QLg0Ltkxo6vAbAEzjz8CXaniZbxlh97IHqyjnblVKhVi30vo1hW2wnaYw35RUeh_ovIujcIc2hqjcdOq29Qmo5mbmj1up4Ui0AE5esbGL6g5TmcQr4tTO831FDdBaM95JukLCoN0uI7t__hL2jo1103wbGhrBF1wJWhl6rIljjdy_U1y74LE8fYP0MMtIugWGxH4LYzy971o9vsCPKbEIuny0FHwRXlqQvimh2TLaWuauYxInD7tYg2yNaE_pZkbXC9T_i6am9r7U7CkRscNZs0czwm4vMtDhs3YW3v9WTLgjtHPBvHPooQwAbxpzRbG6-3o-HBiKZiHdViKHtvVdIxK9d36cIiHCuFYwduStyYXHkyNLD8if2ym1sjKPsFPd2Gu8Olc2S6eWhnPjZmfhgkPyoP-ZWN4O_tt_DF9l7YSEK6bdMCDj5Cspuai995Zri6rYucd-3eSCUUBU_xto6aWMZ1_zQiBoel4DdnVGLoLKDsVtNvmzK-Br9PXzBLqfA3JMEscekroYk7ZdXajjo0O68Do-pNYtDA64c-V0UTSrG-GXPqsSaDzcBBCnDeP5Qa7xZ2SCIYTfsdsar393K-kmR23IFwNW8zk4akV95T2sEG1Al_Wiv9Tk3MFcGJ4BpIRzYcTJlQVOAs_2bD4Gxdx8Nk9ILwPbScG7VroqSOrXVmcX4arq8ZBqx99vMiYgkfq2hCRZ6LXJ08ufpl0u7B2-i1FAtEBUo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XkML0AvZJ_T66Fv79z32RtWCMa9u1iwcT2VNNOS624qFbF1M4MtU0QyIF4kN-RXgFy2M24joTFw6_QFIFS_ICyBFn8jESX-ojTQb6jKG3sGdHl-sdemMog,&b64e=1&sign=0feb6fadcb27023afe5430e89abd7a6c&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 930,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 11,
                            percent: 2,
                        },
                        {
                            value: 2,
                            count: 5,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 7,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 23,
                            percent: 5,
                        },
                        {
                            value: 5,
                            count: 444,
                            percent: 91,
                        },
                    ],
                },
                id: 141152,
                name: 'AUDIO-DRIVE.RU',
                domain: 'audio-drive.ru',
                registered: '2013-02-06',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Березовая аллея, дом 5а, строение 1-3, 127273',
                opinionUrl:
                    'https://market.yandex.ru/shop--audio-drive-ru/141152/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 761,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/941727/market_yV6FPWm17FX-y_93co8GvA/orig',
            },
            delivery: {
                price: {
                    value: '290',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 290 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '290',
                            },
                            daysFrom: 0,
                            daysTo: 1,
                            orderBefore: 11,
                        },
                        brief: 'до&nbsp;завтра при заказе до 11:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/BQqejt5oGXmmemXXigk7bQ?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtv3I5ItORCGgK6u4o5jg8ItxwcEzIcFoVHYRirGNah3U_itZ3RQA4ItBRXnKm6A7eQvvqyF9VTzC134fhSA2ADnbbdS3JugzOXecJO70M8XgOWNl1yesMGA7M1Xu-W68yc0Dxw5HInKuNDdLIZfo1xU&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.002563969698,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.720344',
                    },
                ],
            },
            photos: [
                {
                    width: 761,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/941727/market_yV6FPWm17FX-y_93co8GvA/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 190,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/941727/market_yV6FPWm17FX-y_93co8GvA/200x200',
                },
            ],
        },
        {
            __type: 'model',
            id: 341289023,
            name: 'Магнитный держатель Baseus Small ears series Magnetic suction bracket',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'магнитный держатель для автомобиля, место крепления: приборная панель, способ крепления: клеящаяся платформа, подходит для смартфонов',
            photo: {
                width: 701,
                height: 377,
                url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id279252353479408240.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 701,
                    height: 377,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id279252353479408240.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 134,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id862172009727581319.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 609,
                    height: 628,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1749547/img_id9199766411368963418.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 665,
                    height: 542,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1642819/img_id1121792995364110447.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 637,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1605421/img_id5449319956439311620.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 583,
                    height: 672,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750349/img_id3496147677392410399.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 411,
                    height: 299,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1081556/img_id1432195262279341807.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13898623',
                        },
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1290',
                min: '1155',
                avg: '1155',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 5,
                count: 6,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket/341289023?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket/341289023/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 2,
            opinionCount: 0,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZG0pbuKMVS33HEa-RjqftLPP88lxHGlBNaMX5PAKDhqIA',
                wareMd5: 'm_48L9wLAZfOWGy05_666g',
                skuType: 'market',
                name:
                    'Магнитный держатель Baseus Small Ears Series на клеящейся платформе, цвет Серебристый (SUER-C0S)',
                description:
                    'Baseus Small Ears Series – стильный автомобильный холдер на приборную панель, предназначенный для крепления мобильных телефонов диагональю до 5,5 дюймов',
                price: {
                    value: '1290',
                },
                cpa: false,
                directUrl: 'https://www.evrika.shop/magnitnyj-derzhatel-baseus-small-ears-series-na-kleyashhejsy/',
                shop: {
                    organizations: [],
                    id: 465642,
                    outlets: [],
                },
                model: {
                    id: 341289023,
                },
                onStock: true,
                phone: {
                    number: '+7 495 088-71-47',
                    sanitized: '+74950887147',
                },
                photo: {
                    width: 970,
                    height: 865,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1401054/market_UXmQB71WU8pk_uSUjNqlaw/orig',
                },
                delivery: {
                    price: {
                        value: '300',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '300',
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 23,
                            },
                            brief: 'завтра при заказе до 23:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.004080992192,
                        NUMBER_OFFERS: 43,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.127082',
                        },
                    ],
                },
                photos: [
                    {
                        width: 970,
                        height: 865,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1401054/market_UXmQB71WU8pk_uSUjNqlaw/orig',
                    },
                    {
                        width: 970,
                        height: 528,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1071167/market_NQzFYwpQYee0WD5ml6fBLA/orig',
                    },
                    {
                        width: 970,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1041839/market_MbKz96PWfdHEVtjiPapxgw/orig',
                    },
                    {
                        width: 970,
                        height: 786,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1047642/market_NVMz_MN0U0-MohsWeiDaOg/orig',
                    },
                    {
                        width: 841,
                        height: 970,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/935279/market_0pbneJBGMRsnYEXTtpB8BQ/orig',
                    },
                    {
                        width: 701,
                        height: 377,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id279252353479408240.jpeg/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 169,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1401054/market_UXmQB71WU8pk_uSUjNqlaw/190x250',
                    },
                    {
                        width: 190,
                        height: 103,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1071167/market_NQzFYwpQYee0WD5ml6fBLA/190x250',
                    },
                    {
                        width: 190,
                        height: 137,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1041839/market_MbKz96PWfdHEVtjiPapxgw/190x250',
                    },
                    {
                        width: 190,
                        height: 153,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1047642/market_NVMz_MN0U0-MohsWeiDaOg/190x250',
                    },
                    {
                        width: 173,
                        height: 200,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/935279/market_0pbneJBGMRsnYEXTtpB8BQ/200x200',
                    },
                    {
                        width: 190,
                        height: 102,
                        url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id279252353479408240.jpeg/190x250',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.718908',
                    },
                ],
            },
            showUid: '15717800175384431183316017',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--magnitnyi-derzhatel-baseus-small-ears-series-magnetic-suction-bracket/341289023/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZGluLpe8pAj4Lgj9oj5qPlb4WLzNY_N9m0N_yJtwi2GOA',
            wareMd5: 'tEPKhMo74gjOZzEhC0D-ag',
            skuType: 'market',
            name:
                'Магнитный держатель с беспроводной зарядкой Baseus Big Ears Car Mount Wireless Charger черный (WXER-01)',
            description:
                'Автомобильный держатель с быстрой беспроводной зарядкой Baseus Big Ears предлагает премиальное качество и дизайн. Магнитное крепление надежно удерживает смартфон, беспроводная технология QI обеспечивает быструю зарядку мобильного устройства, а возможность крепления на торпеду или в дефлектор обеспечивает непревзойденный уровень комфорта при использовании. Держатель совместим со всеми мобильными устройствами, поддерживающими стандарт QI (на некоторые модели необходим дополнительный ресивер).',
            price: {
                value: '1234',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6aKgzjFNMKWTyh0AHIB1pTcAm47E9v2-wUwXVLRtA7YjWqK0j2gdokxLPLCtLVDsCuMPOrXu5HW-84dhScVSBtzhQl-yVxu9HVdHz5XiEJd-n5_SlKrkgeNyEZt4GwsZf9tK_xXruilMxGY6vbZWklOhDKF9I5lsm6XtwOEn3QKzfAoMdERjppwsimWPn8QFHkr2ud9U6Ct13PvgaxJXG0tA1LNGmRlsFFoF8VHfFSBt9K6ekFVCFyUWrCN_bexy-hiaYZEhZJguL8rmvSUw8IniGK-lBMiJ6JUsubT4kSNJYS1dXqvAY8_YZMOv_mB3F5PBva71_g8JcIrmlCuSOjHJ6YJ3kGwr2QJCht8YG5plS5rXMWDQ_PainKxDFml_x-RoDRX9TKTwthFNfK3VBbdcw_9Vrg4ZLwGWizX7uVCqzHY3MkdtuO4-377cB6iMOAFJ0PXS0l0lnM7eewUbBpHzKrwlmGP2EieVYThdUi2Gs3HKq0ODED43k-5N7fi4_5uuJYrP3E0q9H7SI5-i0htsFX8m27fstBEXorssAEkOjBIhzWiXPgdC_lm6CQ_PKPsP85FS_gKSRwjrWwTY2X515rUaG52ATBP3G4GhKznO4WTzpzOF3E0o8FaYIYf8n_-MSLaHdO9pTTudc5lnOqeXTXg4DGqd2S4y9a4RBGLQ63UBOJYfyQT6ybp0FUT6AZxh2j9O5rCJxXGTVOP7WiFZpW4-Cjv7Jo,?data=QVyKqSPyGQwNvdoowNEPjWE79YKW3b5aCYNK4JKXUPGMPEWur5gA2FzcuU0zedsE9qmomfTqYR2-9audyLxZSvxayyYk-Fi08HZOX95dSnVkz01RCSHvvm8IP_3M7CD4JKC6mP5Ax4hg-Kq0Sq7VICS3RneMv1vBwYkkAForA_L3ODYrskpJ4DoIfgop1RSu7YTthuigstqAainImvxW6vL47TlOB-auFEeFTQw6JSiPokk3S0f-ZxsuMnDwYfS_lf20Oq_vO79Zhd3JdX9SE2P6J7FsZHHsfNwerPHf8E-PkbBJwWJMvPXgQ_vZu6VrkgA6CvouV4MsHddjpmOBHByX_tuqljz6vuLHiMCtxqq2A5JjHhF1qOC5xDJtUskllWEyK00w6wFGUlCoDaLrP02A8yQVB4U7Hi0uqYVMLaYo087mlxw0wv7hI9TqDqeo3B45ZKEwl9taXOSpE39hHKJrU0S7FTV5KbK7hYQid-7ZyUY7QZPVVEsEMcKCX6x0phjAQTOs9qDqmlu0oBgFpYesmxD_R8bp2DZBgiDeZKLEcIRweA1fJi1ngrmd7Jje9xPUUX4aZba6nOypHuwJPtSRbCVoO72DUaFWxRh_G4hXDGvzsO9LMJ5RR5Dbf7zLGA34ZlI2ZLw1KZOS4dRjQiuaBkgMN9dQSStOU40Q2L-3jckqLiktiiRtWu0_ZrFvc8fY2WYzgKs,&b64e=1&sign=bb88c6f232ac7e17a398d7b689dad8fe&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6aKgzjFNMKWTyh0AHIB1pTcAm47E9v2-wUwXVLRtA7YjWqK0j2gdokxLPLCtLVDsCuMPOrXu5HW-84dhScVSBtzhQl-yVxu9HVdHz5XiEJd-n5_SlKrkgeNyEZt4GwsZf9tK_xXruilMxGY6vbZWklOhDKF9I5lsm6XtwOEn3QKzfAoMdERjppwsimWPn8QFHkr2ud9U6Ct13PvgaxJXG0tA1LNGmRlsFFoF8VHfFSBt9K6ekFVCFyUWrCN_bexy-hiaYZEhZJguL8rmvSUw8IniGK-lBMiJ6JUsubT4kSNJYS1dXqvAY8_YZMOv_mB3F5PBva71_g8JcIrmlCuSOjHJ6YJ3kGwr2QJCht8YG5plS5rXMWDQ_PainKxDFml_x-RoDRX9TKTwthFNfK3VBbdcw_9Vrg4ZLwGWizX7uVCqzHY3MkdtuO4-377cB6iMOAFJ0PXS0l0lnM7eewUbBpHzKrwlmGP2EieVYThdUi2Gs3HKq0ODED43k-5N7fi4_5uuJYrP3E0q9H7SI5-i0htsFX8m27fstBEXorssAEkOjBIhzWiXPgdC_lm6CQ_PKPsP85FS_gKSRwjrWwTY2X515rUaG52ATBP3G4GhKznO4WTzpzOF3E0o8FaYIYf8n_-MSLaHdO9pTTudc5lnOqeXTXg4DGqd2S4y9a4RBGLQ63UBOJYfyQT6ybp0FUT6AZxh2j9O5rCJxXGTVOP7WiFZpW4-Cjv7Jo,?data=QVyKqSPyGQwNvdoowNEPjWE79YKW3b5aCYNK4JKXUPGMPEWur5gA2FzcuU0zedsE9qmomfTqYR2-9audyLxZSvxayyYk-Fi08HZOX95dSnVkz01RCSHvvm8IP_3M7CD4JKC6mP5Ax4hg-Kq0Sq7VICS3RneMv1vBwYkkAForA_L3ODYrskpJ4DoIfgop1RSu7YTthuigstqAainImvxW6vL47TlOB-auFEeFTQw6JSiPokk3S0f-ZxsuMnDwYfS_lf20Oq_vO79Zhd3JdX9SE2P6J7FsZHHsfNwerPHf8E-PkbBJwWJMvPXgQ_vZu6VrkgA6CvouV4MsHddjpmOBHByX_tuqljz6vuLHiMCtxqq2A5JjHhF1qOC5xDJtUskllWEyK00w6wFGUlCoDaLrP02A8yQVB4U7Hi0uqYVMLaYo087mlxw0wv7hI9TqDqeo3B45ZKEwl9taXOSpE39hHKJrU0S7FTV5KbK7hYQid-7ZyUY7QZPVVEsEMcKCX6x0phjAQTOs9qDqmlu0oBgFpYesmxD_R8bp2DZBgiDeZKLEcIRweA1fJi1ngrmd7Jje9xPUUX4aZba6nOypHuwJPtSRbCVoO72DUaFWxRh_G4hXDGvzsO9LMJ5RR5Dbf7zLGA34ZlI2ZLw1KZOS4dRjQiuaBkgMN9dQSStOU40Q2L-3jckqLiktiiRtWu0_ZrFvc8fY2WYzgKs,&b64e=1&sign=bb88c6f232ac7e17a398d7b689dad8fe&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6aKgzjFNMKWTyh0AHIB1pTcAm47E9v2-wUwXVLRtA7YjWqK0j2gdokxXjKoQr1cF8k8llTx-ixK6eXCUdMaElD6z3C0ExaVhv-fke6XeXhM6kCgmidDfd2HYucX6XmM7uo300AAUwZbdkNAL3fG7jNhs-7WuRzOojmHCNb2MlaQAqohd1HWp_EJaJcpiQs-BdsELrx5_yU9TX8UXr7Mgvi1hZYRrJPHz0PZ4HoeZFhTU5mqdgPhdzQtANMoloEzfVdCYikDfnOcs8kvo2JzRgRXKPKD0FW7VR-tgoi6pqNCI7LYvyb3H_lo3xdw2wZTysjr4fwnUwY5tQQHsimzFFChM6oHTQIqcvXrI-sOlrRoxzNbyp_tnFEA6U7q9ik2SxWksO9QoP1jcLU_R5_BNz15zbg7vWRUJRbEOr7HMgwvBq_no9LofcyicDhfDo0K5vMrmUE-sHG9Jw8alnruBR5N1GRaGPZECKHEpY0zbbA5yb2ySy4Un111legxAiFuZQb-jntU9JHesdbhn7dZQKOjfQ-WYqCZqUtypueRD6khO0_AfbqfYDGH5-4t-k_dvmRl6MKYd5zBaeguC7H6gduvybYL3byKTTzdvIqJWwxpc7b2M3X_V0UE6zHjwR27Z81ZvjwS5K2Y062Pi_UVb0V9rj2DyCs1lTMB2jpvxLUoYQ1uHm1LWBRD4yMwIQb2KqYWNMU29_9EZXKZOe7_OPOtrvy6hHi7igI,?data=QVyKqSPyGQwNvdoowNEPjWE79YKW3b5aCYNK4JKXUPGMPEWur5gA2FzcuU0zedsE9qmomfTqYR2-9audyLxZSvxayyYk-Fi08HZOX95dSnVkz01RCSHvvm8IP_3M7CD4JKC6mP5Ax4hg-Kq0Sq7VICS3RneMv1vBwYkkAForA_L3ODYrskpJ4DoIfgop1RSu7YTthuigstqAainImvxW6vL47TlOB-auFEeFTQw6JSiPokk3S0f-ZxsuMnDwYfS_lf20Oq_vO79Zhd3JdX9SE2P6J7FsZHHsfNwerPHf8E-PkbBJwWJMvPXgQ_vZu6VrkgA6CvouV4MsHddjpmOBHByX_tuqljz6vuLHiMCtxqq2A5JjHhF1qOC5xDJtUskllWEyK00w6wFGUlCoDaLrP02A8yQVB4U7Hi0uqYVMLaYo087mlxw0wv7hI9TqDqeo3B45ZKEwl9taXOSpE39hHKJrU0S7FTV5KbK7hYQid-7ZyUY7QZPVVEsEMcKCX6x0phjAQTOs9qDqmlu0oBgFpYesmxD_R8bp2DZBgiDeZKLEcIRweA1fJi1ngrmd7Jje9xPUUX4aZba6nOypHuwJPtSRbCVoO72DUaFWxRh_G4hXDGvzsO9LMJ5RR5Dbf7zLGA34ZlI2ZLw1KZOS4dRjQiuaBkgMN9dQSStOU40Q2L-3jckqLiktiiRtWu0_ZrFvc8fY2WYzgKs,&b64e=1&sign=e69673a2adf2211eedffffb6b4150700&keyno=1',
            },
            directUrl:
                'https://lirider.ru/products/avtomobilnyi-derzhatel-dlya-smartfona-baseus-big-ears-magnetic-car-mount-s-besprovodnoi-zaryadkoi-iq',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrW-4v6wupnzp_mUNDz-1STRLJ6RfNjncwZUZ7AZTs9Xtrf_0rYqdsCb7NF8YOF0js4QRkuxw62o2E6CNCv3FNGlKW6G0uJQJUXeILf2JG6_8d4fwIWQsU1Au6rROsi__zUjrC5zpylIhxQS3eD4_L8ILAeIpgwfXIEt9X7rUSvkMl9XEF3XNc9IxNXV6-YQRHYO-S_CD1GB4LpBfvIdMiEAQfBHUz9-WFgnDHNetsJzZv4GWiJg4-bXVkXBE8YjpOHSg6_A-kyMZ6X2tPN7YimWNv7VgvEDIU-HDfwC_xE_OIrg7BEqYEByY08KhgRBRsIH8a5W5pKGZsj22cEy1H1N9eJVfes18CVa9gD7m2mxz81CmKtZfLRPpOs-5O5ed2YtJfvUcfWv0xe_St0Kmq8-rYge22zqcPK-vo_npU7MM2-zhNoTDA2HZ_WPJA0zJfmN45P6PaOmLd1NUJzBEjjdTBCm5b-_8IwYLNc9Oj2j4yCvwRPJ7asoB5CtbH4jaDodlQHUGoni65ODGDRj4JembCxUtX0779G4pSKotQCYIoVVMN3dX8Zk5lF2SIk5Kil3c91Kh05-BGH1cv2AHSoktx9G6LFmoeA5tOLs6wkndCVhDVcP1em6WR8LIcoCkVh4rJpv6aE8YVlkY1mJsB_5PXk94NaswnDRukBafD2OrcXqTl7uopnjmV2VvM6fve01G4AjBLUO9GG-nxdlQzW-KKmE8VJhjAVJ6AZ6_Kfti0,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TD7fKCIHnA4C2IEl9QAbUWzDX1PDh94H3sbvigoUgtzf9R7JCfwiiWZGrmSjmRLHxyipRAblbu3Z9kUaQ0NeXSjE_A4XW2j_VbGbfqiMzPd3xaJGYZze3w,&b64e=1&sign=2b29a243778e8559bc1084ae677e9872&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWurpRK8oiqMDIAXZTYCpA6kiHuTzHWNPpzPZNtGSAU2wKSQAhoMb2pHY1-sd49Z8seBKXZC9k9luefVThq1ea_bLKQK9XJz7Mt4b6Ryghect-PQqIs8Wu6kckwTvH7k95UID8rMsQgE7S_ppuyylMlfcvqB0Ti6kqTBJLl9dqsUdM6NAzMzp8IAD7M75fTVdw1zqJl4psUj4C93xkULqZ2s7owqwhp8R8ZVJtBhG2WOOrwDy2ZU93B0laZFttmYVLPJ1KcSQ3gP7gBNP1NamupuuE4oljl3WyQTxPt8DwnmbHJOgLxFkMc4pISgqvlUg1pOcBs3jLi00NisI_3jD8tiBeqFKuVlL_JgSlNZgslSuwbLxvUEJhp3z-pC19RtJuMH9qxMnU-uDOxLyE0kABLhL14s8BF-NSWTZXFlNra_2LRkrMjpNZPyISHR5ANWtGRKa994XXvLY0k6LDyMM-nz4_1iPkq-CBNGMT2W4Yv1UnCeGgzA0TpYeNrCcN8sdklrn_3oDaGD-rCXWSFRWusk8qudnzZnszKlX6XaLJjgVivqbRrdYFjL2pER_Mn1j7ArMU3qgozyHQRlAtjw5FTCG0r28QtekNdxq-UoWAqd368bUvP7alS92zPGr9Fz9BUaju8gGoOq31GBb-wHJzsEgjFTUCyE9icAxWK89MxD8475BQNuZ67lRFSkQ6TWxzCyrsdvQCgB9_CSOvR2yo5jWluOHfGdqbaKhm2RJDQyc,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TD7fKCIHnA4C2IEl9QAbUWzDX1PDh94H0XiaNaPDRVjJH-WddrxiFoUH0HJCV-yKPQ6IOe3b5PdSewjqq04USSYTitjb68eMU9Iqt0jXxDgFb413wCCufs,&b64e=1&sign=6b342ba566f4c35bb99376b401ef4a0f&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.5,
                    count: 363,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 9,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 1,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 5,
                            count: 136,
                            percent: 91,
                        },
                    ],
                },
                id: 386543,
                name: 'LiRider.ru',
                domain: 'lirider.ru',
                registered: '2016-11-08',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, МКАД 72-й км, Бизнес Парк Greenwood, корпус 23, 143441',
                opinionUrl: 'https://market.yandex.ru/shop--lirider-ru/386543/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 761,
                height: 800,
                url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/orig',
            },
            delivery: {
                price: {
                    value: '285',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 285 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '285',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 15,
                        },
                        brief: 'завтра при заказе до 15:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/tEPKhMo74gjOZzEhC0D-ag?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtu7u7UrCtZdtQFEpxWbwuGLMm1WypCIprUw_mIxz1meB27zyutLuwKtrtN68vX4I6gubYTlNgJ-uxgqQLeLvo7M15jpn6Gktrop385peusGlKm1PQRnyRH_mQo-UUCzdS534BYSWjFzPiF52dnd9cdL&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.007667689119,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.711801',
                    },
                ],
            },
            photos: [
                {
                    width: 761,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/orig',
                },
                {
                    width: 952,
                    height: 900,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_pTwP04iq0So_nOkvvBabbw/orig',
                },
                {
                    width: 877,
                    height: 900,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_WylPjGSAp6bcXfx_HqQV5g/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 190,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_iuYoo0KNDVm1IHTO1lNxqQ/200x200',
                },
                {
                    width: 190,
                    height: 179,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_pTwP04iq0So_nOkvvBabbw/190x250',
                },
                {
                    width: 155,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_WylPjGSAp6bcXfx_HqQV5g/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZH90RQufeoSxZLq5YRbQQrL_wZ_5crFc-65Q6O6uS28UA',
            wareMd5: 'kY8w7dDu87WaWoidJvYecw',
            skuType: 'market',
            name: 'Baseus Магнитный автомобильный держатель с беспроводной зарядкой Big Ears WXER-01',
            description:
                'Автокрепление + Qi зарядка Теперь не нужно возиться с проводами, чтобы зарядить смартфон, просто прикрепите его к автокреплению Baseus и наслаждайтесь поездкой. Зарядка начинается автоматически с момента установки смартфона на крепление. Два вида крепления В комплект входит 2 вида крепления (зажим для решетки вентиляции или подставка на приборную панель автомобиля), которые Вы можете использовать на свое усмотрение.',
            price: {
                value: '2199',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrtRjZV6LL4Uakhym4dQM7fHvBUDQZnDzj6RkzgYpr3yQBlhpHGr0MNRXPUMFEGJQ3NQYZbckatBKg9p3wn1cCSKoI6ASUSRMBTTeHpswT_5hZmwVYkGh62OOTJcbyEROfmdqE4rFAz1C_NhRmxQA9lMwzEUaseU1ciDROwl4h-WHMmMsJBJL-rerGJiRzCEijnaf--SdYi0x3mr83SyXVpLAELVhy9kY1S3J4JT6YJP3XtRwidlDnthCIQ_pBRXT6CJV2QsPyxrM8K8CGDAuVQaBK26gX_lGCxKtWFvwIJwKdgdMvOgIRo16xTrVAN9y1zz5Pf3mhmWm4xlWC-ec41z0f73EB__wz7wI-wK31V5QB-sCD9qRHI6yq40Zyim9DPcgaPUZAL9ndmla8EjSsi84iS_72D5vt2hcacmOFc9Ydwm0OHQUpge1qraZ5oEtp9L7CyGYQ6lhIOaTjBa7jQ2u6hK42tPuEWv9JF_lw158SQsJxI6zHV7i48cZj6B9N-2XPbbMVtRR-2mhUOd9-iQ3MgyszExXp0zOhgzxbnZI-XVmq-2OgtM-aKHde70Svv-XgYmiYLnsW428m-oQyevteQqAePpQgC_MNJu2yELtAdzZkZPuLrAaeVMgqeZ3A7KdjWJupWmHV-ennM663PaGGxKNBysDoOwq1QJuGBYmNJuvd7WpiJNWTKMIFxHPvlezq4q4UNAhjJpilsChRSS6xxE3E4P9osjCFFSltyjnaRU_eBxAkgBvwk3asGIzmrRg8gmdjAZFanGnug9axMshYAbbddYagiBGJqETbc1qp3hJCztBVH-ZFkkxARVrDCH7aG8EtOOixONWlxBcPlak9Mr5nNpTScZkT3i9kwU9NzcsC42IrAijqpIgpeEauD3B5pgTF2JcWrvgtlA_0xvJYag_SFLKIMec3AcmYND0Abe2Y-q48g,?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_p3nsTVhHfFSK-DW_obKbFI_gr-WZGXsh_jQlpVgJ3bx7sUn9CZ7j5Y9UcPjo75BzXcvSFta-iCoyYfCf3c3vhDD7nIyYg-UG_UgDrXveBdbGOvlA_32CzeQeAxQCWyt_Wm3L5rvcsTECPMYf2jk8lC9FTE-Yva9gJwio_RWB2dTTFscSG4VDdx_aXIW1EW-LmEaN1YsilUjaMkEmApdJHj9jsVhRZOW6m4bcqoj1DZOP-fZRgvqa_7fnf2kX22_hinO3rnJGopJUKcFk7WV_xoXpTXKETeO4_khEjC2KEMQH355Q62UQVg0BGam0wuF1iTaYH6IZKtaSFrQtbVOnYlEnAnF0Y0Yg-k2vkNJ5lesaSs-mEQm9Twl4-v2XntfmdEfWwwD7_wiiB-XaEaTMcqp6sX4Az_js3ZdB9EZS3W1twWDDj7BNg0xrO3HFJ8enOub5SiWROqgdmm-6ix8hxVscauGhgMV6OiZXDg4TOoh4IptgEFG3x3kC-QR5O83VgewU_zZ-pbca8SNAzWroED6VgBtOOoCE,&b64e=1&sign=cc9585d9e52b7acdca03fcb807cf9e43&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrtRjZV6LL4Uakhym4dQM7fHvBUDQZnDzj6RkzgYpr3yQBlhpHGr0MNRXPUMFEGJQ3NQYZbckatBKg9p3wn1cCSKoI6ASUSRMBTTeHpswT_5hZmwVYkGh62OOTJcbyEROfmdqE4rFAz1C_NhRmxQA9lMwzEUaseU1ciDROwl4h-WHMmMsJBJL-rerGJiRzCEijnaf--SdYi0x3mr83SyXVpLAELVhy9kY1S3J4JT6YJP3XtRwidlDnthCIQ_pBRXT6CJV2QsPyxrM8K8CGDAuVQaBK26gX_lGCxKtWFvwIJwKdgdMvOgIRo16xTrVAN9y1zz5Pf3mhmWm4xlWC-ec41z0f73EB__wz7wI-wK31V5QB-sCD9qRHI6yq40Zyim9DPcgaPUZAL9ndmla8EjSsi84iS_72D5vt2hcacmOFc9Ydwm0OHQUpge1qraZ5oEtp9L7CyGYQ6lhIOaTjBa7jQ2u6hK42tPuEWv9JF_lw158SQsJxI6zHV7i48cZj6B9N-2XPbbMVtRR-2mhUOd9-iQ3MgyszExXp0zOhgzxbnZI-XVmq-2OgtM-aKHde70Svv-XgYmiYLnsW428m-oQyevteQqAePpQgC_MNJu2yELtAdzZkZPuLrAaeVMgqeZ3A7KdjWJupWmHV-ennM663PaGGxKNBysDoOwq1QJuGBYmNJuvd7WpiJNWTKMIFxHPvlezq4q4UNAhjJpilsChRSS6xxE3E4P9osjCFFSltyjnaRU_eBxAkgBvwk3asGIzmrRg8gmdjAZFanGnug9axMshYAbbddYagiBGJqETbc1qp3hJCztBVH-ZFkkxARVrDCH7aG8EtOOixONWlxBcPlak9Mr5nNpTScZkT3i9kwU9NzcsC42IrAijqpIgpeEauD3B5pgTF2JcWrvgtlA_0xvJYag_SFLKIMec3AcmYND0Abe2Y-q48g,?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_p3nsTVhHfFSK-DW_obKbFI_gr-WZGXsh_jQlpVgJ3bx7sUn9CZ7j5Y9UcPjo75BzXcvSFta-iCoyYfCf3c3vhDD7nIyYg-UG_UgDrXveBdbGOvlA_32CzeQeAxQCWyt_Wm3L5rvcsTECPMYf2jk8lC9FTE-Yva9gJwio_RWB2dTTFscSG4VDdx_aXIW1EW-LmEaN1YsilUjaMkEmApdJHj9jsVhRZOW6m4bcqoj1DZOP-fZRgvqa_7fnf2kX22_hinO3rnJGopJUKcFk7WV_xoXpTXKETeO4_khEjC2KEMQH355Q62UQVg0BGam0wuF1iTaYH6IZKtaSFrQtbVOnYlEnAnF0Y0Yg-k2vkNJ5lesaSs-mEQm9Twl4-v2XntfmdEfWwwD7_wiiB-XaEaTMcqp6sX4Az_js3ZdB9EZS3W1twWDDj7BNg0xrO3HFJ8enOub5SiWROqgdmm-6ix8hxVscauGhgMV6OiZXDg4TOoh4IptgEFG3x3kC-QR5O83VgewU_zZ-pbca8SNAzWroED6VgBtOOoCE,&b64e=1&sign=cc9585d9e52b7acdca03fcb807cf9e43&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrtRjZV6LL4Uakhym4dQM7fHvBUDQZnDzj6RkzgYpr3yQBlhpHGr0MNRXPUMFEGJQ3NQYZbckatBKg9p3wn1cCSKoI6ASUSRMBTTeHpswT_5hZmwVYkGh62OOTJcbyEROfmdqE4rFAz1C_NhRmxQA9lMwzEUaseU1ciDROwl4h-WHMmMsJBJL-rerGJiRzCEijnaf--SdYi0x3mr83SyXVpLAELVhy9kY1S3J4JT6YJP3XtRwidlDnthCIQ_pBRXT6CJV2QsPyxrt5Y865ldMpjJ-jxR2k71CnaF6Vz4qeuaEY-96RLnIGK7dcsOy5VYLsXWjbQudeKSIYiK6-3Orwsh-cCIuP9L4DBJhnYEaFKFSShrT5MPPruZ9-TL0R4yVpYTDSZi4BqxdlcTBKK7r1KERaXk87u8h0QwP5DsEQr-7WuleqN457R05TuDaJRrVELVbjpOLE-fO0z8uZbRwk1TdqTtpcjdvQxAh-FzSbmsvHdF1_HsjStjniPwm_vbUtqpcRH1la8ProURmawBuESzbfgF28awYzIhAPy4nG8_JqJoFbSX-9JHUd5Tg9v1LOwuiRbeu6ATrcov3KdON6PqO-5cv21uFLFBVbfGXs3yv7XUfaZ8AFJUHFG39YP8j1pfCweAq6Whw6g1oRDap_5K5Eh4S_cV8BgMpnjDN7WzvRDPpf3uiZJ2dd4HjbHdAHBL9Tno_jI0OwJM8C3EIhG5bMgH0NhOkmCpRGwMCfgEC_yK1embID57wacPiM-vlmsC_vSJ2E5ZcL8Y50ULyPaVsZuh3oHrq5lsyhU7lpMvQVXOfYBQWNuek-PjyW3taNc9BipnGW9FPKdEw0NEnmqC6ZK1oXdz8QBR-c4dtrg5WU38Ik4lmTDqwTTe9K4d0v9CZ3VJPsQYPW2-ICeNWR7oEELyjmb2XoQLkkW6rYTPBe0efMiy27g,?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_p3nsTVhHfFSK-DW_obKbFI_gr-WZGXsh_jQlpVgJ3bx7sUn9CZ7j5Y9UcPjo75BzXcvSFta-iCoyYfCf3c3vhDD7nIyYg-UG_UgDrXveBdbGOvlA_32CzeQeAxQCWyt_Wm3L5rvcsTECPMYf2jk8lC9FTE-Yva9gJwio_RWB2dTTFscSG4VDdx_aXIW1EW-LmEaN1YsilUjaMkEmApdJHj9jsVhRZOW6m4bcqoj1DZOP-fZRgvqa_7fnf2kX22_hinO3rnJGopJUKcFk7WV_xoXpTXKETeO4_khEjC2KEMQH355Q62UQVg0BGam0wuF1iTaYH6IZKtaSFrQtbVOnYlEnAnF0Y0Yg-k2vkNJ5lesaSs-mEQm9Twl4-v2XntfmdEfWwwD7_wiiB-XaEaTMcqp6sX4Az_js3ZdB9EZS3W1twWDDj7BNg0xrO3HFJ8enOub5SiWROqgdmm-6ix8hxVscauGhgMV6OiZXDg4TOoh4IptgEFG3x3kC-QR5O83VgewU_zZ-pbca8SNAzWroED6VgBtOOoCE,&b64e=1&sign=cb61aea8f8275d0471df40202a8969ec&keyno=1',
            },
            directUrl: 'https://www.lifeproof-store.ru/products/baseus-big-ears-avtokreplenie-qi-zaryadka',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.9,
                    count: 491,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 4,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 10,
                            percent: 2,
                        },
                        {
                            value: 5,
                            count: 388,
                            percent: 96,
                        },
                    ],
                },
                id: 239284,
                name: 'lifeproof-store.ru',
                domain: 'www.lifeproof-store.ru',
                registered: '2014-07-10',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Луговой проезд, дом 2, 109652',
                opinionUrl:
                    'https://market.yandex.ru/shop--lifeproof-store-ru/239284/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 700,
                height: 700,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1662088/market_utUjEQp6htiMSf9Uwff0kg/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб.',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                        },
                        brief: 'завтра',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '500',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 14,
                        },
                        brief: 'сегодня при заказе до 14:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/kY8w7dDu87WaWoidJvYecw?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqtshHi9MGFuaGz0aE9Aus6_KqkHE_B7wKA9I35FWjjvWJnWVKjpt7LDFWh2b1UdFTGCW2QZk2JhQwmDx5aUO7XkuRMsNLZULLjf7-UjEgVpscKNMKimSUGutUoo1JJzszAdboW_MuknOcNa6WBsTYjD_&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            promo: {
                type: 'promo-code',
                promoCode: 'Market19',
                discount: {
                    value: '5',
                },
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.004209954292,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.703261',
                    },
                ],
            },
            photos: [
                {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1662088/market_utUjEQp6htiMSf9Uwff0kg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1662088/market_utUjEQp6htiMSf9Uwff0kg/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZGI6HsxIWTClaZbnYDRxhnzMjg0XKEc0qaq2LuVY038Og',
            wareMd5: 'QWWoG6d6AOZ0PzQrLb9R6g',
            skuType: 'market',
            name: 'Держатель с функцией беспроводной зарядки Baseus Big Ears Car Mount Wireless Charger Black',
            description:
                'Автодержатель для телефонов и других устройств в комплекте с быстрой беспроводной зарядкой. Ваш мобильный будет показывать маршрут и одновременно подзаряжаться. Производитель: Baseus Модель: Ears Car Mount Wireless Charger Номер: WHER-01 Надежная фиксация 4-мя мощными магнитами Возможность зарядки смартфонов совместимых с технологией QI Не блокирует сигнал Тип крепления: на торпедо или в воздуховод Вращающаяся 360° градусов подставка Материалы: алюминиевый сплав, ABS пластик В комплекте: держатель, зарядка',
            price: {
                value: '1800',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6bgyOYiLozGFzNfcgj_V3H85kwzDy9hAj4ByGrAYhKpDqkl7ncNCeZtj6NeG6ILmMDISlSOZuaE9pgIflOVX-9Cf3xmhXDGsi0mamn1cnoswaghBYiX-pqm8jsFYKTPIQ89PoxFstOki9F_ENSRyE4SFWq3bDwO5vFFsDMYjg719hR2bjqA8op64npvkSJMxyXwK0n_VBD1cost3bMl8azNpTLHe-vsoXNIBoaGNuUDPte5jdoRE0lrm6WDEAlrAwXvtdlf-dsZkbIEFSzse2mXUTUF7f0qpvBongl0BT-TXD0G6GpEZsG8d94OS8sY_NvHBMg_gGyyCmXtvGta-qscdSzbzhKZVgmR6LiA7VNTZv8op72J8jKOKifpijZ0kYgyHJ7OHp8L4vE7fXzPTO3_bZA4g5zTxLGJj7TiOjzppXT2A8RZLXCO4egYFE4lx0UGJlYi75wgRCRB_H7AxuhzH80BWchyapbLECMAotVonQBOuCpq6t7U1P0Uw7dmzZcvnufAGiVC7xDAkSsfKsA4msAq3p7b9aZOgiU08DMQ0FEpJ7OpCjMIQeaBpPyM-uFA6bBFteOJh-q-VDTnetyDdW_2DZc6Zg36vkbw_PVl2sDanXa-_4wLCXl1xrqDtfwmlR5DnFb-AtO79S1tt6yqU9JfyzqXPDS7zDS-xtCif3oWOioSVbHgLYQUPHGY_WxIcrI9ZGBLgM7V1akC1xr30qpY_0PDyxQ,?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmRe5tcX4AgzKID2Nr30RnO6WDSDkzKUiJ5vphefjGO6ATsc1LGD8EyXictAfKxrVpn4EO1qGze99MNffaIGwrUJqb1L7iW6YGb&b64e=1&sign=5144247004021b9ad1ccdee5f608bd1c&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6bgyOYiLozGFzNfcgj_V3H85kwzDy9hAj4ByGrAYhKpDqkl7ncNCeZtj6NeG6ILmMDISlSOZuaE9pgIflOVX-9Cf3xmhXDGsi0mamn1cnoswaghBYiX-pqm8jsFYKTPIQ89PoxFstOki9F_ENSRyE4SFWq3bDwO5vFFsDMYjg719hR2bjqA8op64npvkSJMxyXwK0n_VBD1cost3bMl8azNpTLHe-vsoXNIBoaGNuUDPte5jdoRE0lrm6WDEAlrAwXvtdlf-dsZkbIEFSzse2mXUTUF7f0qpvBongl0BT-TXD0G6GpEZsG8d94OS8sY_NvHBMg_gGyyCmXtvGta-qscdSzbzhKZVgmR6LiA7VNTZv8op72J8jKOKifpijZ0kYgyHJ7OHp8L4vE7fXzPTO3_bZA4g5zTxLGJj7TiOjzppXT2A8RZLXCO4egYFE4lx0UGJlYi75wgRCRB_H7AxuhzH80BWchyapbLECMAotVonQBOuCpq6t7U1P0Uw7dmzZcvnufAGiVC7xDAkSsfKsA4msAq3p7b9aZOgiU08DMQ0FEpJ7OpCjMIQeaBpPyM-uFA6bBFteOJh-q-VDTnetyDdW_2DZc6Zg36vkbw_PVl2sDanXa-_4wLCXl1xrqDtfwmlR5DnFb-AtO79S1tt6yqU9JfyzqXPDS7zDS-xtCif3oWOioSVbHgLYQUPHGY_WxIcrI9ZGBLgM7V1akC1xr30qpY_0PDyxQ,?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmRe5tcX4AgzKID2Nr30RnO6WDSDkzKUiJ5vphefjGO6ATsc1LGD8EyXictAfKxrVpn4EO1qGze99MNffaIGwrUJqb1L7iW6YGb&b64e=1&sign=5144247004021b9ad1ccdee5f608bd1c&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZjECS40gNLiORY-Fmod1_H4uWy1S0F2h8t3wyRdz5RTjLhI-WUhv2Oq_IT9so3fXNQjcWQnG-K9q2cv6_l-rkWRterxrP70wGNWfk707LWxJ1O-2OHdvyAVQIOHko_jaw80FSvQYhEUl8SFq4Y8vmmYRisXbXfJJzM36yX1xDl0IXIKPrf3Asuk773aQlHbRtfAJdU1_2AWXAsVrtZmea6bgyOYiLozGFzNfcgj_V3H85kwzDy9hAj4ByGrAYhKpDqkl7ncNCeZtoLg-6dPMO7dOajAezA2zQUv1RkrttUWU0Jmj5ojNswnEZfbF2RcOHo_07oZwYJZXT4dByshWsREFz-_Or74H4CShEClJd4LTHADnvxeOCKbnNSabyZ_EeuZ9VLffvGFAYcfop-f0cZDwpckqt3HV3mLd-OBiebP5NU458KvnZONHWI1-w4Z725586b6lSb2e3XRqqhVoHWor0dV6sYVQ04iLALJE2z-EQZ5UYad0uocHx45GMM3b7UZnoYGsMU3BDFdlTj5xO02RncM4dA4LjdapQafmHi6x6yjO9wVLw4YdILXJtGkRc-q3rnMsGxQCUhan_cMYW5LhPXllNg0D5V_t9egZo-AWKb88b4uFf4yI_9yqjIi0wF8dyqW7ZMWOXVbTYEC2fsSn0uJmpCvnPGFpoMqdgARqB41v68FLNerYZhWCE9XReiMEpHZ5vUryockPGTkM-io-B86FKzs8KZ1f0Jwf4M1vyK2xF7sfm0PWDqRPFJFShWq0td36jqaYudjnlzwLAdxteAdDdXjq1iOxymnMVa1557vCu4OK09Lo2heZSuMOmEPpmE0tFadBzzLnIeTbuE6H0kZJc_80sjQd4Z7M_4LsnDw3cYTdW6GIoCPMSCLs6KSfrqfPxh1jgBqY5Tpm1m5PM7RZMgBxfXazZD1MQV03pqQ8V_Hzhj4,?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmRe5tcX4AgzKID2Nr30RnO6WDSDkzKUiJ5vphefjGO6ATsc1LGD8EyXictAfKxrVpn4EO1qGze99MNffaIGwrUJqb1L7iW6YGb&b64e=1&sign=d2474754364ad2ffd2604a1ca6143d81&keyno=1',
            },
            directUrl: 'https://deshevlevsego.ru/plansheti/derzateli_dla_plansheta/2-5507',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrW8pU1cXURlJSgH3r_e803hh2Hy_f9_3r67DPU3Ug7qWprwkcGmMfA7q6fsNRa6lTkHPKxbxm5xcDSbul-_yBRzGRcDHvZFuyQ552CO3KXR4tBn1u0cmHHYdB2U8YXpTDSWBoI1eLU2WbVp6KvZrCroM8ZaRB3DjPNuPG4P7MSW6tcX8qfP90nHEeBtXGoEJpqMnJ-bIMxyrD7gWGWeF34z_RxKT16k6BrjOrNxQMab57O6nSpEkhYkQbOYB1vRMmXKk_RwHdaTz2d7wB7TGayQ80lrQltVKbLUCAgf1WxTFlweosDcNC6GUSwPCZdb4l5UE9hquR_ot_fqfSGRympKym_S95ANKE3girULa9SxSrdpCwNjsf7bmEbmsrTrS-pVRb0F2sYKArg0ORvqEe2YbbfOHx59ENv3vJLCFOAesudx4cC9o25gvNPQtVmslwBCx_9UaTaJmCsiMkIerTg91MeRx7Y72eE2mMyk-sSymBgIK6P4TVRwEHD4s7cQqoLeSqZaOZjEyU3zLZb1rVvb1SSS3UCEUFrujxuZvM0EgMv9_7UPLTV7-K2mZSMPqQn3mlCA2mPcTKuifLcqmp_D63Ax1epbmRFywBlE6U3OHeonrG-oAC0_v_ENwcZPgw8w4u-SUh249eFN2z0CiHoJIrH3m-L_Dm8UPGhCh8MAWlXeQ_LwTLn-s2Cj_rsyKIWW6ISIt0KhIhu982F6iJ1VGvDsR8pTVrIS_hE8mKWp6s,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S9F-JKUREI92qgTT9QKtPS7IsY7xJlFOo4J8GqeuZdhU34FiQRqyIYaiYVXrF5fY0OPYS9HVhet3CNkYYHSkEPegfltrlNXbtgWLxTPeKKgveM2VBVMoAI,&b64e=1&sign=d707350f16ef1c57afc3c0fe244fca06&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuACb_JGC4DcNGWSgOM1CHCUxFsYFhBcJi5pj1QObVyDVNgahmgIVZ8uk7A7jXD9Yb60gwVBdnAIvnyAoI4YmK5qbe-ArwBQSzIhYe-ynOuO6oHX3wCeUeERaJkSLlv491GPdnyNHl10V4OMhcIs1g5qRTzgDi-_jmOZ0mXS5yBtydh0p-G1bL6tJ_nnDIjkAOLE9iyXtQRpgYx948ZtJrWz1o6L3CiAgoqJ_xKncfJuSEJRx-7IyZjzkAgl_Exq3HnuylFNuh3ozjwgOzo4hBYde9YMi1DIYxLZfCx0DRfd2EHhDnE3gQBj8QDfpRh80mEZQxohRdMaENiieDfR9LNhYzG7wzWok6Bl9IrdnQm8Xvkyohn7VXcTq5cgdb7U6HiN7iPsC-bo-aIUeVo6vsHAE73bpqUr_fz43v-4hFTWLjirtlknkk30XyKgSIvnWxQOKVUTO7Ac_4erbO9aEPQZbS2WxFMC_ovFXU7ewk8Nbf-sbP5FG_Dxr0DF8kT2BK4ihZypsXSPu7uU-N2Y_hXiimdZ5ohgS4631WsLdq-LOpucYIbxPIyLhLrf_G_09e3OY8dYnxGx5yqqhPwCMfWFAw5yrk0gCAn70g806Q6BLiTlOCU51os02p7_i5q9G6qEfq70ICTvmTKWGmvQjJHK5Bz3o8eNUew-sowi4SrbDpt0pbFyz6jnzwVxWH_WhT9Qc4oG9ZVShgHwTCxppy934OdU_UcAvkXUDy-cWFmkrERyU9dwMyMUOgsUz20jGsVXON7X3orBD-Ev_DVOc17gNISCZmwpCRKeVdeDTDg7TQGMqfDU592CyeOxoJ9zi8YlKfr7_y_e2qEOLCOlQV0dtKueEZsr-leEw4U8vf6E82of8Gl4YS9DvQ813bOjwwVqJR0_PGoiysqTb7r0NbZe5qdqxqivuNwEGnSWeOYI5qB308qh0u6yQ_gGNp_4I8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S9F-JKUREI92qgTT9QKtPS7IsY7xJlFOvSMWfhFrtJ8GcQaindNVl0jJ59NMAnlIcBIpevS1hKRuSlpHHFiXoVQwMBJhFNtAaGsOWmj0ojJDvbygILqD6k,&b64e=1&sign=b358ad314de439eb2a266ae41fbb8cb4&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 227,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 3,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 3,
                            percent: 2,
                        },
                        {
                            value: 4,
                            count: 7,
                            percent: 5,
                        },
                        {
                            value: 5,
                            count: 127,
                            percent: 89,
                        },
                    ],
                },
                id: 133318,
                name: 'ДешевлеВсего.ru',
                domain: 'deshevlevsego.ru',
                registered: '2012-12-13',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Смоленский Бульвар, дом 24, строение 2, 119002',
                opinionUrl:
                    'https://market.yandex.ru/shop--deshevlevsego-ru/133318/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 193771144,
            },
            onStock: true,
            photo: {
                width: 607,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 12,
                        },
                        brief: 'сегодня при заказе до 12:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/QWWoG6d6AOZ0PzQrLb9R6g?model_id=193771144&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=6ntuLgvDqttQ0Qc1WTOtCZ-aN_Fxm1paPE7Zmk3Z05ZtKM5-6aqOmLhv8YnbW_9kj3zXUKnWlrq04uO86tPxDQVj384Sx45x4qESCZcSCSTh_nQJFztEdlKHNSlqEB08cO41DCTn72zlZQ_gt7K-1KZ_-yk8LF92&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.004513686523,
                    NUMBER_OFFERS: 36,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.701232',
                    },
                ],
            },
            photos: [
                {
                    width: 607,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 173,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id2592865496023873307.jpeg/200x200',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZEHXGU_yXRlSDqFtQpIUhsDEnuxMMzfhhZsH7oevAAv2A',
            wareMd5: 'zs-Yy5ztD1RcCc-B3fuVMg',
            skuType: 'market',
            name: 'Держатель с беспроводной зарядкой Baseus Smart Vehicle Bracket Черный',
            description: '',
            price: {
                value: '1569',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlnl-xHpy-9Nw660d78URz-HC4QLbmNvcIhNdoZsPmJMfbqmNH0ZYWLrbq0FlRexH8n3q2b0vz2G91D_foKOftBZJ2tEkUsxYVfVeIOm86QZPF5-U0LkaW8PeSsN4PCHksvIX0DESSuoqR5yn6kkaHxjGKJryoDEhLtaQzpM2J2l5xq9g9q5KxeBm5wYaiI3lUQY7i6ZVngV0MAqVQA3aHFVlH3TCNnIzGLuiHSB2vUIUS5hQIy99zaW8R6K7PVsrcFk3608Jb5UeDFRGydRziVxRkyU-9ex6OpsiFkuqyhhuIXs7WdvexpHeSJbyl88vqkSSlkfJuSgoeV3h4A6nfY0D9xHWiq_2ILNUqgLbl3eLNt7Cl8TACSYM5_zEvgZL9jz5gbIog4-FJWuS4MV3uPA9g3rmGEWVR8R_Bhg1juo6By_b-e0GEju2uVZZr8YayKaloMLSQxI7jdvpMQp9Imbe_lgaAqDsivuAFBuj-BnCGzOa2H8UhmFtfqwZtVl3InSzevDAz-K7XL4tX85s4jdyIXnUHs9A9eyRc8VfDGAGAL7XiM8wUMATy3-7shFe59C7vyNOTYYKZAzXKjyeR48pPKUlckfKE0LWxOg3-JAQiLjTvG8JR20Gg_nwN7EWokKNxyM4J-itDz-yDQJ1vk9gEnbAVfzkXNwR50dsSYKnm2i0DcA1X2yPxYV_g9vUy94Eil4sjsJeCwUnbTYhQIFVjk3coW1D0Lr2m-6BxuAZiI_YpZN8Pxl4z4ap_KiCh6ElmPEVG3DwCC3f26i_EntqSQ0nSoQGgDGKp3k3quTEdqFzjKO128ZTBWp-pe7RJcx9V1S3mTIN1so9jcpkg9gUADkXafnOZtjFAgw_Jx9SCGQmWpGDWlDJvOe5y_sWdXNX8jN--f5pCW1gn0CEQV3Bhazds_2Vz9XBMGgvD8XJ22UOFLHHhg,?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH9H6BC4_AHNaMhMjTibyilsDUA10ewlSruPGY5mO9pdhFgRMMe5WBfftHW7e3eOi4REzFzXlPdoyCaT5l_RaEbeyfxv6OZW4j2HKLNylK139t43aTlG4ZOioD02Srrmk7abp615EYVBFbOz2gMYZy55mnrTh3C6vAQF6_8PWgbHEaqSva7zO6N9OD_np9eoQaHfkVCKTlcK5zeX4izg7t3vFsNQzIwYaq792BT5NhLrr0pZQhbNe9kHy1VPrpyAahvFA7ZbVx9qvw,,&b64e=1&sign=d3c91413f15cb8c1b4ea48f1aac660cb&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlnl-xHpy-9Nw660d78URz-HC4QLbmNvcIhNdoZsPmJMfbqmNH0ZYWLrbq0FlRexH8n3q2b0vz2G91D_foKOftBZJ2tEkUsxYVfVeIOm86QZPF5-U0LkaW8PeSsN4PCHksvIX0DESSuoqR5yn6kkaHxjGKJryoDEhLtaQzpM2J2l5xq9g9q5KxeBm5wYaiI3lUQY7i6ZVngV0MAqVQA3aHFVlH3TCNnIzGLuiHSB2vUIUS5hQIy99zaW8R6K7PVsrcFk3608Jb5UeDFRGydRziVxRkyU-9ex6OpsiFkuqyhhuIXs7WdvexpHeSJbyl88vqkSSlkfJuSgoeV3h4A6nfY0D9xHWiq_2ILNUqgLbl3eLNt7Cl8TACSYM5_zEvgZL9jz5gbIog4-FJWuS4MV3uPA9g3rmGEWVR8R_Bhg1juo6By_b-e0GEju2uVZZr8YayKaloMLSQxI7jdvpMQp9Imbe_lgaAqDsivuAFBuj-BnCGzOa2H8UhmFtfqwZtVl3InSzevDAz-K7XL4tX85s4jdyIXnUHs9A9eyRc8VfDGAGAL7XiM8wUMATy3-7shFe59C7vyNOTYYKZAzXKjyeR48pPKUlckfKE0LWxOg3-JAQiLjTvG8JR20Gg_nwN7EWokKNxyM4J-itDz-yDQJ1vk9gEnbAVfzkXNwR50dsSYKnm2i0DcA1X2yPxYV_g9vUy94Eil4sjsJeCwUnbTYhQIFVjk3coW1D0Lr2m-6BxuAZiI_YpZN8Pxl4z4ap_KiCh6ElmPEVG3DwCC3f26i_EntqSQ0nSoQGgDGKp3k3quTEdqFzjKO128ZTBWp-pe7RJcx9V1S3mTIN1so9jcpkg9gUADkXafnOZtjFAgw_Jx9SCGQmWpGDWlDJvOe5y_sWdXNX8jN--f5pCW1gn0CEQV3Bhazds_2Vz9XBMGgvD8XJ22UOFLHHhg,?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH9H6BC4_AHNaMhMjTibyilsDUA10ewlSruPGY5mO9pdhFgRMMe5WBfftHW7e3eOi4REzFzXlPdoyCaT5l_RaEbeyfxv6OZW4j2HKLNylK139t43aTlG4ZOioD02Srrmk7abp615EYVBFbOz2gMYZy55mnrTh3C6vAQF6_8PWgbHEaqSva7zO6N9OD_np9eoQaHfkVCKTlcK5zeX4izg7t3vFsNQzIwYaq792BT5NhLrr0pZQhbNe9kHy1VPrpyAahvFA7ZbVx9qvw,,&b64e=1&sign=d3c91413f15cb8c1b4ea48f1aac660cb&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlnl-xHpy-9Nw660d78URz-HC4QLbmNvcIhNdoZsPmJMfbqmNH0ZYWLrbq0FlRexH8n3q2b0vz2G91D_foKOftBZJ2tEkUsxYVfVeIOm86QZPF5-U0LkaW8PeSsN4PCHksvIX0DESSuoqR5yn6kkaHxjGKJryoDEhLtaQzpM2J2l5xq9g9q5KxeBm5wYaiI3lUQY7i6ZVngV0MAqVQA3aHFVlH3TCNnIzGLuiHSB2vUIUS5hQIy99zaW8R6K7PVsrcFk3608Jb5U_bWBdC72p2Us4664q40fpLATPPzM-8Pp2iLUyd6nSkraArPUuZje8ep4EzPdkCVD1n7dVE8CQ9bgHMezci-FmN30FBWp3Y_tBA4oLPYXJS4uR_3VNeguWDnmXLnxPqtya6GJM1KgveFuSLJosm6zvyEhmPVjithsjE8hFmlzbdQJOTwEbeU7_g6dIXG_c6V5thi1nY1Yx-hgqtOgcN8iC1LkeizBFoC3Hgrg88OMgSdA16W47K192ZvnH4214qSZzffVjPyHteC2uXt0Ua1P52V36rdKLMYdLN8du71e0rOHGMLiLDPz2S34vCVptE6MkAUrkXYp4JtcgY3ScP7BMdCDma3VtFszmUZVSUhximL9KL6dbmo0f0jHDyaBSpSP9z5YNXaWM9pOLlqCQWR6GtWoZmEQLd4qIff4Iytj8y8XvAmHQVvKoMD69THoP53P5J1nR2FxHmMqjC2uoqWdmiCY5Hh3B5RNCgsxjdyEtSpiSyFULZvk50qmOVzN8Q4DwdRquwy5Jw5OgxOa3il2nvzlRVlUfgBIiS8keoiugKz71j5c7zRlrnc7MBl2OZ-a-i1-e9seMsZn-aN8ujz6QjosethdpXFZ1HwFOfFpeLvtkybkD6eXDYY87R3DnIHfW3Mso_V42JpUruEPcgnfSeVAMMahXCcOTkH4XljZkSQ,?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH9H6BC4_AHNaMhMjTibyilsDUA10ewlSruPGY5mO9pdhFgRMMe5WBfftHW7e3eOi4REzFzXlPdoyCaT5l_RaEbeyfxv6OZW4j2HKLNylK139t43aTlG4ZOioD02Srrmk7abp615EYVBFbOz2gMYZy55mnrTh3C6vAQF6_8PWgbHEaqSva7zO6N9OD_np9eoQaHfkVCKTlcK5zeX4izg7t3vFsNQzIwYaq792BT5NhLrr0pZQhbNe9kHy1VPrpyAahvFA7ZbVx9qvw,,&b64e=1&sign=4f95da238e2bfbea88abaf997a3356d1&keyno=1',
            },
            directUrl:
                'https://elementx.shop/product/derzhatel-s-besprovodnoy-zaryadkoy-baseus-smart-vehicle-bracket-chernyy?utm_source=yandex_market&utm_term=%7Bproduct.id%7D',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtJU_GcCQGf12Ufha9JD3v6Efjj7DoqhE-87HUScu_OAcx8H10RjhaKkIistRHVYeJUrlhdFoyeN1QgkbhUorD7eovQf3A8XujXB7uuAz45skFOrcYhHABBXi8WxNK5lbeBC67fQSFYx7yd7tH4OP0CbJ1hBlXiRUuVXHA8qh-4g841mlWMYpjheCATSZtIboHHcBfNoo7E0ExVS7GwiAxei7QVFKShpZbTK5taRXE-KPPF8tTcjBPnlxAxECTIZ1WZskYK8pTZ4UrKgpBq3dE2xh_pQKBR40GQWYryu2bjT9Rv0IJQiYiwV9O6ZhJE-lPyg_ot7TUdQUnIeGGnJdhGsRjp053Xn_Edz_mPZj_txfu-0for3T8mM8YXwms8ZuXr5_HnEhnI-2UJa8WT_ILUPbG5-B_LciWhDObfZnFy3wZ2uUBUB9Z4f_RQ_6KsQXYBEY-J3q8EgwIhEnUhJEFBi_Li7vW9MFWHbrvipbg1ipFCgJTXGB_TlGd6dIxWHHNErSiDGeKlhqUCMJBtQhV6061Qu_sI29m8JiPJgCe70DFqMMNT-jvvMEvTIZ33fVbFSj6pe-SGaRz-ylILAbZQ1-DUPBZxNZZFuupK8xfzhfLNsmV_Nq6V1oiC6_q4NyzZ6A8uRixIeAgxha0K95wjgFSlRii0XE1VZoC4_DfEkwQzg0Xeb0fwP8nY-9O2y4eu-DtOTiapxXaUCKjCuNBLDuosVe1K2Gj1AsNts0rHJ_oDTNjYxbnB-hXJuiqlUxY7xK1SwU3gSWa5FlljaB2w3JrtE47BQ7lrMpDuMx-Vbfa9QvIDhausJ8ocPdfkb3RMhRPgYqGWyMOBXjzPsj6MQg60uu96UgotrYHCpKRwlt9KunPkB0kl_ZnCqUZzaJYZRWKMiXQ6cf_kq43WvUKmJedNK-vIK9OLMmM-Jgq-mn80TOcMRizge9D-TXFGGMU,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjUlBkVRWtEB63POvuEokOQT0Sy5x5gJdOwDdQymq2ac2JKn6aTnYyxengdJ7i380YWY7BXR5tXsPb8umUeowmnE,&b64e=1&sign=137fc87a595a1f2da54057b1b5a5d9df&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRtJU_GcCQGf12Ufha9JD3v6Efjj7DoqhE-87HUScu_OAcx8H10RjhaKkIistRHVYeJUrlhdFoyeN1QgkbhUorD7eovQf3A8XujXB7uuAz45skFOrcYhHABBXi8WxNK5lbeBC67fQSFYx7yd7tH4OP0CbJ1hBlXiRUuVXHA8qh-4g841mlWMYpjheCATSZtIboHHcBfNoo7E0ExVS7GwiAxeXMPeLA34FrdsMPFj_hCHnZmr7GAy8_zsmtFE6BkpU-X8iClkxpT-GXgXMvI_WCYuWKHBd0I9NPxOvgK0dg8amaFV6bftKYCL45tT30tHr0NLDBNiOwjSywD4Ij-Kj5JohhufiiSfco0RoT36CNVf7dTbjeJwVSgJApMqDMijz_jHwgbrOL2NYa1xcN-9SlUMqKaimyN2G-XhMV9XnpLu9IYC45t48QlM96wSFs4JbF4UeTZF2YCvfEvORUijIO2iUDeNg0VeEstFBWqBhekbxYKi7CdYyOGjFvqYv_wGL9D8M55Uzsrz9J349-4MppLvBWdDL2KiMZ1ST1d2_SzsZlCJDI73ozX2xCsJONlkWVRP5-iTQeQqRKikpbVieujqF50fVswbI0QRjMNsW-3yksdV7MWOfYn_6vAE5VEycTKeWLdNYumhSazwlCP-m9xlVjy64WPkcrUU8obnOt-KtRjjAFomykcVVwY_2Ke8_-vqrVWSFOKVyn_1yKlrUX_wGN95QqJsqXGVgihKQgnC8DRZm7LLnfYu2fFnACal7h_xDOc_cwWu_vYNQMjkSJT5C10XfxedpLpJkfaCnTd6-45RZ5huTba3WKblzL_Qok6fkzLxiNpzo-Uo5nAyhHCLaT0TbWduve0LwZE6hJ_7tJJLQmGAnf27Umke53yAsY1Aes9btIVsRnGTSppDaTnWewQUEnmAU3eoBR2tiu9Ed2Ls1jEHpFtaryza4OEPjFg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjXzjs1c_itgASnnWQ4tNfEwTEqkERFSnpI_rs8v5Yqob4pY1oqEafOQA7ppyuf6F0oLDrzEzA4rL0FCjG8hbh7c,&b64e=1&sign=c6ce85dc85019cb67df0b8856261d256&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.3,
                    count: 51,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 13,
                            percent: 81,
                        },
                    ],
                },
                id: 546830,
                name: 'ELEMENTX',
                domain: 'elementx.shop',
                registered: '2018-11-28',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--elementx/546830/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 242788342,
            },
            onStock: true,
            phone: {
                number: '+7 4996771466',
                sanitized: '+74996771466',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlnl-xHpy-9Nw660d78URz-HC4QLbmNvcIhNdoZsPmJMfbqmNH0ZYWLrbq0FlRexH8n3q2b0vz2G91D_foKOftBZJ2tEkUsxYVfVeIOm86QZPF5-U0LkaW8PeSsN4PCHksvIX0DESSuoqR5yn6kkaHxjGKJryoDEhLtaQzpM2J2l5xq9g9q5KxeBm5wYaiI3lUQY7i6ZVngVgRkwZ_IRvKKshQkKx1VT4KVFLxnmjhZ2Y0jYuerw9zb8NrDixfFfxzRQVp10i6p8L7eHkXCGieJcbA8CvBKE-wv_Ldw-cVNRhiTixEdxcirAA2fOGW_eQYufHtG-GpGwOv8iUSgpzpN3WfTpbfrmIPJDRTtYl4fA34J2bn7sFSNDnJeyHM1CG6y8RG5OdGA2V_JxkFqycjkpGeSak42w2ljmKk07XK3VujAUz_InDsFYzFwlpXx8rMgtr40K_CQDPXnZZgOhx6cCheMifD8BZTtlVVBK09akwdwMhQRzzPcnISln7edXJcpiarPjsuZjn0J-pTyt8W8lyBPxVnEWZfkdjmSK0SgdKsaYBjbIkJAlOlJ3RPCJqJ9Fdwj59-DUR4EqDkFo5-_g0SpXEhD-4MLbkCkSXabhSAQ-a92lDs2IEK15Eob6SfN3ZqWQfgYas63-t2nK1IM5O3ojDCZBvWtkzxIe5U7Pt2YNBQFVRzOpavXzZ-ne7dk2xDpFxWAIUFR2aGGie_r40jas5zOq8fPMo3mZV9IDyNJHSGxHOnOEP7nHuXR6y45Qdf3yrQbHLs0zpNlXB3oV68T4LGniICsC66Dv3pB2pINwZ4nidjDGlI6jD8juX2jrYHpOOTZbtM3qcmULOw-GobTsBBBxOLaK2oWQXDmULFbVhpWqHFNDNuex6YdRTfiiPOjecQ2opkLz0OKKjrTwwHhmI5hmOqAEPbCBp4ojhedfCHpA5mw,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O88L9zXLF5jbDyQhCjIg4mmbjkepnSHC9LA1jwykxUKABOSOZY7kWr7XMIi2vEjJnyWlsNNzkjGHl0gmjCofdWHs-pMKQS5u-ZM656aFy8U6dH602oS2uEnuG6PCUuZetIY7sIdpLZuFKJNP0zWlPxotz93HScQD1xHRnFEXN8cyA,,&b64e=1&sign=4561ef2f830d36e8d1ad980f5ee9812b&keyno=1',
            },
            photo: {
                width: 544,
                height: 500,
                url: 'https://avatars.mds.yandex.net/get-marketpic/236356/market_B5d_ZxcsDkOCqryQQVel6A/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 21,
                        },
                        brief: 'завтра при заказе до 21:00',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 15,
                        },
                        brief: 'сегодня при заказе до 15:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/zs-Yy5ztD1RcCc-B3fuVMg?model_id=242788342&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=lKlQB0wV-a7ST3QhGoYXzxnX2Hh7Ubeaay4_VXQ7XSs6FxrCZpm3B27HQabmYCFi5fdiEZD1nx2hkilUZvCBrqq4J7nLX8NuIm0pygPLGq3P7A9Ir5jR0TCH0omqw3yKUlNAFp3gwKjcr7YbBM7wh5hqf0nhjxvv&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.005095730536,
                    NUMBER_OFFERS: 39,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.510318',
                    },
                ],
            },
            photos: [
                {
                    width: 544,
                    height: 500,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236356/market_B5d_ZxcsDkOCqryQQVel6A/orig',
                },
                {
                    width: 522,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/907814/market_Zbeo-3Mwk7liYZw9MEKWvA/orig',
                },
                {
                    width: 633,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1734040/market_zGqr3tTkmY6bVLK87n_zqQ/orig',
                },
                {
                    width: 701,
                    height: 680,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1907452/market_4uOmdC5b2TUmo0LO-s7TqQ/orig',
                },
                {
                    width: 701,
                    height: 687,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1570741/market_cZBzB-XFlg3AUjUKo8Dudw/orig',
                },
                {
                    width: 619,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1545910/market_XgfMIeydsy8k6YaT5dSv9w/orig',
                },
                {
                    width: 407,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1394832/market_JxLCNi0t0wKDF_kKvgoihw/orig',
                },
                {
                    width: 701,
                    height: 402,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1339465/market_XRJhCe6YM4QnOvA_KOdtxw/orig',
                },
                {
                    width: 407,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1770294/market_v68Ix77XvBCgL7QN0-pqZg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 190,
                    height: 174,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/236356/market_B5d_ZxcsDkOCqryQQVel6A/190x250',
                },
                {
                    width: 186,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/907814/market_Zbeo-3Mwk7liYZw9MEKWvA/190x250',
                },
                {
                    width: 180,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1734040/market_zGqr3tTkmY6bVLK87n_zqQ/200x200',
                },
                {
                    width: 190,
                    height: 184,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1907452/market_4uOmdC5b2TUmo0LO-s7TqQ/190x250',
                },
                {
                    width: 190,
                    height: 186,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1570741/market_cZBzB-XFlg3AUjUKo8Dudw/190x250',
                },
                {
                    width: 176,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1545910/market_XgfMIeydsy8k6YaT5dSv9w/200x200',
                },
                {
                    width: 145,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1394832/market_JxLCNi0t0wKDF_kKvgoihw/190x250',
                },
                {
                    width: 190,
                    height: 108,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1339465/market_XRJhCe6YM4QnOvA_KOdtxw/190x250',
                },
                {
                    width: 145,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1770294/market_v68Ix77XvBCgL7QN0-pqZg/190x250',
                },
            ],
        },
        {
            __type: 'model',
            id: 192793405,
            name:
                'Держатель с беспроводной зарядкой Baseus Metal Wireless Charger Gravity Car Mount (Air Outlet Version)',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: воздуховод, способ крепления: зажим, подходит для смартфонов, зарядное устройство',
            photo: {
                width: 613,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id4353833872828940642.png/orig',
                criteria: [
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 613,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id4353833872828940642.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 384,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1056698/img_id6322545848980886525.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 571,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1332324/img_id7752734494786099289.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 583,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id2132905351182520335.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 599,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1060343/img_id6866121483692670894.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 602,
                    height: 655,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1345185/img_id2882093039220319610.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 515,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id5803161696294867872.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 614,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id4134268502456708250.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 443,
                    height: 461,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1417902/img_id5130739171875342056.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 402,
                    height: 436,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1605421/img_id2793021759749059627.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 390,
                    height: 455,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767151/img_id7905183297617620825.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 391,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id7129513719138910854.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 570,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1567763/img_id6985091407465434626.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 516,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id1317402433363458764.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14898056',
                        },
                    ],
                },
                {
                    width: 614,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1453843/img_id1173286422664318715.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 514,
                    height: 559,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1514097/img_id8833153172354679600.png/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 395,
                    height: 452,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1642819/img_id6337224859369816729.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 450,
                    height: 466,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id6037161142405946066.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 384,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1521939/img_id884752343349305194.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 569,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1767151/img_id6827671708122231004.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
                {
                    width: 515,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1750207/img_id2940743748441455223.jpeg/orig',
                    criteria: [
                        {
                            id: '14871214',
                            value: '14897638',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1999',
                min: '1050',
                avg: '1199',
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            rating: {
                value: 4.5,
                count: 10,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 1,
                        percent: 100,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-metal-wireless-charger-gravity-car-mount-air-outlet-version/192793405?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-metal-wireless-charger-gravity-car-mount-air-outlet-version/192793405/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 31,
            opinionCount: 1,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZFrtETziwMBjFtRUfkiOsBP_Kao8qjXkfF8pkGTQXixAg',
                wareMd5: 'nw83_Sr8gfjv8A9luK6PIQ',
                skuType: 'market',
                name: 'Держатель Baseus Metal Wireless Charger Gravity Car Mount Air Outlet Version Red WXYL-B09',
                description:
                    'Артикул № 660837 Baseus Metal Wireless Charger Gravity Car Mount представляет собой беспроводное зарядное устройство, которое крепится в дефлектор автомобиля, а также служит в качестве держателя для телефона. Модель надежно фиксирует смартфон с трех сторон благодаря регулируемым креплениям, исключая выпадение гаджета. Baseus Metal Wireless Charger Gravity Car Mount имеет компактные размеры, при этом обладает продуманной конструкцией.',
                price: {
                    value: '1106',
                },
                cpa: false,
                directUrl:
                    'https://www.pleer.ru/product_660837_Baseus_Metal_Wireless_Charger_Gravity_CarMount_Air_Outlet_Version_Red_WXYL_B09.html',
                shop: {
                    organizations: [],
                    id: 720,
                    outlets: [],
                },
                model: {
                    id: 192793405,
                },
                onStock: true,
                photo: {
                    width: 768,
                    height: 1024,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/907814/market_ijfM41562Z8Tm4js31o-jw/orig',
                },
                delivery: {
                    price: {
                        value: '268',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 268 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '268',
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 18,
                            },
                            brief: 'сегодня при заказе до 18:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 10785469,
                    name: 'Baseus',
                    site: 'http://www.baseus.com/',
                    isFake: false,
                },
                warranty: false,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.01445470471,
                        NUMBER_OFFERS: 55,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.612667',
                        },
                    ],
                },
                photos: [
                    {
                        width: 768,
                        height: 1024,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/907814/market_ijfM41562Z8Tm4js31o-jw/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 187,
                        height: 250,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/907814/market_ijfM41562Z8Tm4js31o-jw/190x250',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.50547',
                    },
                ],
            },
            showUid: '15717800175384431183316022',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-baseus-metal-wireless-charger-gravity-car-mount-air-outlet-version/192793405/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZHkY6BbISIY713eRL0N9oTEI9bkUVSi51-B8raUB-qI7g',
            wareMd5: 'zJCarmWtdMXI-FWZyGJsgQ',
            skuType: 'market',
            name: 'Держатель с беспроводной зарядкой Baseus Smart Vehicle Bracket Sucker Style Черный',
            description: '',
            price: {
                value: '1659',
                discount: '8',
                base: '1809',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZuqWx324Jzb26DGkrxgcCnF3nJlv37V4DNGX-4-zYDQqsc9TebjMrByhpG9CFdzD0S2crY1DQW9V6faGaQsrlpXNvqUI6ugxN_UclxM58WH99aQSs2PgwQm_IEZLG5IJdpoCNG6tdfYkdXLAS1NrTyZrHeGUdW-I8hl2hJeUEyc2Fzx2mlMHVPdGgeB5BPm2LXiJy-f7n2ZWUos4NCtH8N-yKcYelBi6H5m5avKY3McE1GqGKE_OLqwXvZvthim6LS8EokSDTw8eWLOD06cQEryvlqqckcgYWo3Ayqie_20dcKKOiLOn2WjHPzsow-x_WmxJ27K3oSF-w-zmexX6xY5nm0HdAukMfbQDXndIl6tuUFjGNEoCye_bFcEaqR2rtUhGgYfSj8jcBdDDpyzfPWF9Qk4rEnkbN1Grn5sCGFps8GjPA-2wnPcK55QD7VJ_kXiUtKXSF3VWs9B2eLQO6Td46vfok_CCwbWAfOI3Lh0wqTG0YsqS-QuU2eJ2X30hZrsIEqOALSTTPw-Xu0xvE7jCkrJFr_jYvYaO30ol3TJ4uRKCHA2onqwUNZDYustQy9zto_LPdjB2PbG-GBi513FfUwJ8cAwF1ZEvnPuKpXW3gsf3jWo4bZ4ifGpwI0n9ggBnfZXD5nW8rFaHO3x-5ddaHSY_fJdIuhEhJltzwWXMnGBHRA4Kr-rgwhFsvJC-WP10uSJB3JyUUzkLoBdWh1aO-VsJ0I8xyrKIhL26iyoVL3lEEnihwqyySekktMVGry674Nj2DnIfgF4SeN4au-ROxEBNN2kQzWCDQ_8n0D9g162vyCUJW-B8wvo8yoOEaXHtG-hsT-JmrTC2hex8YnzGQmiro381PNNpcl5W8wDVw0-AM6_erp3rTIgowyDPSWesb_IH98stLqbFAip6TBbeenF1pQ4LerJQPqoHoTWA0PXUCbZjUkUatI06OdGKMdlmpgEMmOoBp4yB-AiDxLcHk5VsKWb-n9Ib5cGfQNga?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH-_WjgjCb3-cvFF3NkjdNyPiOFaapKhNEwtvjmc73Oy6pYv8UTnthZq_LkAmgNwhMMsJaIP9r7BxaiFnym6VFsV-cnv4_Gl8Q6nOM3k8XKK-5x646IzoXNfQeAsgfIxNoSSUWQI9u5r_EnAwT42p33EniF7sZuRseVf95UGn774CfMbrisiWB06VgIMbeaurSYiDzHN08BiZSiabv6V91suz0TmEYT_PfmDUJXtXbMIHAtNwkrTzbxfw1ZPdk32s5tQKDX4cFWTI10qBm-EfFlo&b64e=1&sign=44f14c33c890982ccae90f50496c780e&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZuqWx324Jzb26DGkrxgcCnF3nJlv37V4DNGX-4-zYDQqsc9TebjMrByhpG9CFdzD0S2crY1DQW9V6faGaQsrlpXNvqUI6ugxN_UclxM58WH99aQSs2PgwQm_IEZLG5IJdpoCNG6tdfYkdXLAS1NrTyZrHeGUdW-I8hl2hJeUEyc2Fzx2mlMHVPdGgeB5BPm2LXiJy-f7n2ZWUos4NCtH8N-yKcYelBi6H5m5avKY3McE1GqGKE_OLqwXvZvthim6LS8EokSDTw8eWLOD06cQEryvlqqckcgYWo3Ayqie_20dcKKOiLOn2WjHPzsow-x_WmxJ27K3oSF-w-zmexX6xY5nm0HdAukMfbQDXndIl6tuUFjGNEoCye_bFcEaqR2rtUhGgYfSj8jcBdDDpyzfPWF9Qk4rEnkbN1Grn5sCGFps8GjPA-2wnPcK55QD7VJ_kXiUtKXSF3VWs9B2eLQO6Td46vfok_CCwbWAfOI3Lh0wqTG0YsqS-QuU2eJ2X30hZrsIEqOALSTTPw-Xu0xvE7jCkrJFr_jYvYaO30ol3TJ4uRKCHA2onqwUNZDYustQy9zto_LPdjB2PbG-GBi513FfUwJ8cAwF1ZEvnPuKpXW3gsf3jWo4bZ4ifGpwI0n9ggBnfZXD5nW8rFaHO3x-5ddaHSY_fJdIuhEhJltzwWXMnGBHRA4Kr-rgwhFsvJC-WP10uSJB3JyUUzkLoBdWh1aO-VsJ0I8xyrKIhL26iyoVL3lEEnihwqyySekktMVGry674Nj2DnIfgF4SeN4au-ROxEBNN2kQzWCDQ_8n0D9g162vyCUJW-B8wvo8yoOEaXHtG-hsT-JmrTC2hex8YnzGQmiro381PNNpcl5W8wDVw0-AM6_erp3rTIgowyDPSWesb_IH98stLqbFAip6TBbeenF1pQ4LerJQPqoHoTWA0PXUCbZjUkUatI06OdGKMdlmpgEMmOoBp4yB-AiDxLcHk5VsKWb-n9Ib5cGfQNga?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH-_WjgjCb3-cvFF3NkjdNyPiOFaapKhNEwtvjmc73Oy6pYv8UTnthZq_LkAmgNwhMMsJaIP9r7BxaiFnym6VFsV-cnv4_Gl8Q6nOM3k8XKK-5x646IzoXNfQeAsgfIxNoSSUWQI9u5r_EnAwT42p33EniF7sZuRseVf95UGn774CfMbrisiWB06VgIMbeaurSYiDzHN08BiZSiabv6V91suz0TmEYT_PfmDUJXtXbMIHAtNwkrTzbxfw1ZPdk32s5tQKDX4cFWTI10qBm-EfFlo&b64e=1&sign=44f14c33c890982ccae90f50496c780e&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZuqWx324Jzb26DGkrxgcCnF3nJlv37V4DNGX-4-zYDQqsc9TebjMrByhpG9CFdzD0S2crY1DQW9V6faGaQsrlpXNvqUI6ugxN_UclxM58WH99aQSs2PgwQm_IEZLG5IJdpoCNG6tdfYkdXLAS1NrTyZrHeGUdW-I8hl2hJeUEyc2Fzx2mlMHVPdGgeB5BPm2LXiJy-f7n2ZWUos4NCtH8N-yKcYelBi6H5m5avKY3McE1GqGKE_OLqwXvZvthim6LS8EokSDTw8etMxpe4WwNIZSZBlVtJgxpRzrW5y5n9ufEpLH-9tj46RFJqonzqgcSdaO3mM4RiJpAVoudcebWGbFSeVyMqg8yWahS2xdAgV2jWtRRFj68GhUCkgleHq4EpcjzOatWnaZXLE6OF4QXzt_zT8EgiLlfFKE-Q0yBAP71ug1fMmcKlK2kmMIiTzB2enIf0rGt3wBVU8m0jDVSrQ75EGsakdGag1f7R8eo_tu7Dp0CCqshzaGBT7Ue872I92HD4CpcApkenSneT_4waqAHNc86jD9XgECNiGvXQmi9dGb6gkZwLfvJGD2ziarMl4Tf215_AaoAezRKBGbwpf8v4HTzJ0UjuE77NwpJtNgwnFXACL1i4NHs3aOqzMlB4ThZ_Kv-YYV_nzCI0Z7hs4dqith_Mdshh3bLunI51lFtC5jB1pZbACWEmYhjaqrO6bzW6EmQV8nIDt10AcLtDfC-xnMD-3A10u8KN7wToEHLNd8jpwcx2G3-N8pxMRNDY1cUSebYiMOLY5WiRWnU9YJw0_R0bBVfCVcOD7yRUvTvVWRWJ-qIkvec0Oms4wXdHw95a26gTsX3kkznGb5n-1VASMByl8txCprU7LlhHwSQ7vOVrimkWaIS9m3S_vctVH963V88T_Z_wmS1DkjL_xVo7ZoHNTSsvocD08KwFZpRqBWgR5eRuEfjrTHpzW1GNSq9E0Y4ckSEusvQf5EWmjqhrP1zuzoB0A48CqFzS5j?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d_btmhvY0DQ8KtwsCxOT57BkGqIw7BEgH-_WjgjCb3-cvFF3NkjdNyPiOFaapKhNEwtvjmc73Oy6pYv8UTnthZq_LkAmgNwhMMsJaIP9r7BxaiFnym6VFsV-cnv4_Gl8Q6nOM3k8XKK-5x646IzoXNfQeAsgfIxNoSSUWQI9u5r_EnAwT42p33EniF7sZuRseVf95UGn774CfMbrisiWB06VgIMbeaurSYiDzHN08BiZSiabv6V91suz0TmEYT_PfmDUJXtXbMIHAtNwkrTzbxfw1ZPdk32s5tQKDX4cFWTI10qBm-EfFlo&b64e=1&sign=9883a4989f012d22b380965ae8fdcc4d&keyno=1',
            },
            directUrl:
                'https://elementx.shop/product/derzhatel-s-besprovodnoy-zaryadkoy-baseus-smart-vehicle-bracket-sucker-style-chernyy?utm_source=yandex_market&utm_term=%7Bproduct.id%7D',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvTcsH4HJSOi4XSAGEU0OcIOKHB7dYx23pMrUFRMMfzOcJkDpynZkx7ymdezelV_ZGAcxRKoK-ZgZHC2Fy7xuxwODlW6D8xeKj749xpAs8m0ilnuwUA8gq11mGkKBW6sucyQjOwjeeoX1DnSf8om4tZvRVK0HhxRfpWLB3Wc5itSOSCLdm1vak9c7ztPlZaI7-rhBxnPJNzWUfc7Ms9obw1bGBNzfrqMkHSZ3vj-SRAqBXoWSVE9XzZMCAWWk6QtwdlkGp6KU9weR_a2ONtH14keltubrFpnfAURH-eiLZqbHFGUAF_sJtBO5-ywP1hzVlUKWmSpmCR8gELg3-qbhPO3HI_pEzgUHNkGfJakUEIRkPbZAWZMW-FPhN-uF6KigdproTRzaQ06bEAkro5EK9l6XY2GLhOch5b0k86XsYMBc_nBQueWOzrXr8KzyRX7kCzyqHdXqWYXID6nEBFymv0OkLSF-8FROjm-Xm-ljwvuBQq4F7kYt3JBm5o7ndqinE5NbxQXgO8vaVUjl5saIH2pL-d12eDJMMSlpfCivUpUWfaxbphX_0GXqtwjHUHj_QtyV0JHZMGC_8KgDbT9wLN0eBoEn2FiXYoOh8Q9CBUeGsZsU4SE1Omng964Lbpy2V_Eiuy8JgRjEfFyELvnzKExyl3dsQcFkRp1-WGsAPEEPetoiadtyYoc9goI2DYx1w34o59LPKDxfrFbUVwh1zpf_fzeO8wRgNkzZxcsLNpN6UjLE-Ec_j-Cq9wfVTNHnmO3iciLPw2H-RA_5MODd2fGNI_MahFVSxTndcrHXKJipCT0ugaUEu-dQ7YyduM4SYdx3WipMMBjZS03CtsrwIuHzTkv2gu-ynpeGsgQ7AcbkYdfwy-AzmL8GSFPOUWGv3FEpI1HRF-0p66jozlH2jxuGZOyGIQs6UOf_NK2LsVO-5tdMSxWKuAm8VG3qpDhGVhGer98yT48WPYmINsV-zz4ZAX4HErkexiZfLHfWqxx5H9lNsFKnCS?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjUlBkVRWtEB63POvuEokOQQJJ6dCNyixeLtG41DMmErXWo5CAK-7rS4nACshQn9qIxEGudltQ-YI6rpi18h4gxs,&b64e=1&sign=4be2804451b8ee01723646fff7f2d026&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRvTcsH4HJSOi4XSAGEU0OcIOKHB7dYx23pMrUFRMMfzOcJkDpynZkx7ymdezelV_ZGAcxRKoK-ZgZHC2Fy7xuxwODlW6D8xeKj749xpAs8m0ilnuwUA8gq11mGkKBW6sucyQjOwjeeoX1DnSf8om4tZvRVK0HhxRfpWLB3Wc5itSOSCLdm1vak9c7ztPlZaI7-rhBxnPJNzWUfc7Ms9obw18j975Jax1E6oXByB4C42HgcWfCx1bvmjPajoss7ebZalfBsawYooEkm1R9V_ogZ3m2CjHhm_ccU8Ax-ax8bPw-MnKpcPxVpV9xBXBEso1glFcKMsjQ0HyDOUcM_i0uZJwXGREYWxCF3cj9xjWtjzto0YFRi7mRTOiksvDDHW9wUwIfAPJbB8FA-uV6Jsyf4HnwrA73ab0jE-Ug7ZbMbCEpnpcwVGGdvmqlt-96AhR1mmGaUQiZp22XuxGSnO21gYlenhb-ai2TaP2-XEFxLTjE224onw0FgUl6It789WUYVEvgCTypdILusFDBunhmBWzHbvvNIDn8arOkHrty0u2MA7ui_eXLR507uFr2gK-Ujy8ZNqlmGn6jhwfFoAEZcf4MzIeVEMUX5pI2nFzI_VdaC72s6nYy1PhTZJp4R8Va9OorBmVe8QLZBSZJ0f1GEngmcB3Z2zR-XtJsdmBD2BrDRtMdI9UbWpnskd_gRHXN05l1ud2KLEFuxuJ-ab08SbeWhD4lXmhwMxiF9Lw5XTxxszydPRVBOqDnOQNHeLcw-oIawkAZG_XwkPiW2VxZ5Kje1A-DmE_NmqTowzhXwZefOGhnWCTFUEZdVUJ45V7KaMIw2KYwwTV6uTvxGfw4deNQdI7dUxnTNY2DGM543wvKqGTKLxRUG8L5W6Tp-2qGeJ8u9uZ3P8iQTPtB0TTk7DLcS-PTNxCHDYCKwNY8lQko-kDzIW48zljRERSC-d8DmBvyolT_A384hhEiGvdTqJr8qUwONsrcBaeQdOiSneS6YH59YCdZnC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjXzjs1c_itgASnnWQ4tNfEwTEqkERFSnpCJhlWepuJNXomRCBIdgmh6RuopYVqN1EybuhHJCK7bM8zIaPzoEzgI,&b64e=1&sign=b2ff89b955f76cb2cbe5c4fcac81051d&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.3,
                    count: 51,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 13,
                            percent: 81,
                        },
                    ],
                },
                id: 546830,
                name: 'ELEMENTX',
                domain: 'elementx.shop',
                registered: '2018-11-28',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--elementx/546830/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 394273009,
            },
            onStock: true,
            phone: {
                number: '+7 4996771466',
                sanitized: '+74996771466',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZuqWx324Jzb26DGkrxgcCnF3nJlv37V4DNGX-4-zYDQqsc9TebjMrByhpG9CFdzD0S2crY1DQW9V6faGaQsrlpXNvqUI6ugxN_UclxM58WH99aQSs2PgwQm_IEZLG5IJdpoCNG6tdfYkdXLAS1NrTyZrHeGUdW-I8hl2hJeUEyc2Fzx2mlMHVPdGgeB5BPm2LXiJy-f7n2ZW1Tcxy5K1QZp_JrZju_byyOtfGAjmNuvj16Ef47DKXFKKlP4RS3XyGc7BOP9KePoOIH-GHmMgpLyaCXZFlbOg8XAjFqwetXuG2zaSuXmdaNzQBLGbFHJ2L9cUGVx3_kA0M8MPI_OM54UtyEx5IxWOoVkCZtwUsTXZuKYv9JvIxMoPsKMi7O6K5bzFlZSNXhL2GMeN8tn2VnQAfArRk_D8P9Katt-sKr1l3coCxKUoMYsEAhxwnbWjx4yumzRPNfQpBGnaybpM6SHXbNamoM4P_k33fRlW1J9z3Y_uSQAmcSQZrD4TUlEXR50I3p8H0-eC36r1iM0eiKcz-GLzyvk84U5ZWw1z_AQrsHzPqaMvrdb1mWgTA7zooESv3-6pXfMpitmi92MR1dZ29gp0gnmRQxiLEHOqGx52p6GJBSyJobvzoK0lIkXBy6G7yxg7QEz_G_pBGWLUavd_I2PPJFGOXi5bTo2BI42V2_qups0CkFQ-d2RbzelnhM4TVn3NMGpSYOFFthPLfdD77RVnNMarEYNkmEsui0G_2QJ0f45eiVsBRQGmFEaAUwInSdU1exuqK48EIJh2OCAxkESCiWjEdx9q7mEKAC3mb3PETFFzkfE__lJ81oVVtrjZMSEJI0lJtPXEa9t6GPc8dfFqWcNvVY-edNMPZmYN4eru2iqGSwieLdXY5py_KqhjrgxozPzDZF98moefZ5pJSm83iWq09iHdyBlpLfLAtluZKAKxxcNMvIgpocMnB2cHWHVWyoHMlu3OiYQZKlmmtPCK_D9B9Gvxo6WWlTOI?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Q-RRR5PUnpjPqJXKXqpTDv9IPxKQE_Y7CwODSsiaaaL9FRtZpX98jHco_VWl_t_jv5vV1qOjYGmz1wAWV-CTR7nd9uchPVIZkSreJjp-H-H0i8pN4KCGwkVUJ4_OJXz_pEGwarqF-B2gFpGMQpIo4rfa8LcFr32YzV2xMD6UhkQ,,&b64e=1&sign=9de854972449a633c7b62a6a0224787a&keyno=1',
            },
            photo: {
                width: 350,
                height: 350,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1610546/market_6F26ugeA3NxPM-o_VGK0Jg/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 21,
                        },
                        brief: 'завтра при заказе до 21:00',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 15,
                        },
                        brief: 'сегодня при заказе до 15:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/zJCarmWtdMXI-FWZyGJsgQ?model_id=394273009&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=sepruGgFdkSi8Dg_w1MGQK-mUxR7V4tvjbpkkipX-0CdiKzuWHNmj2wEtQfmU45qB0FZMhwo7esMEau0TWfRxKK-AjUNnQozPfA9KPmdaztUPCaPusXD6p5E4r7IhUjtcKFiciThISHT9yD_-2EmoHxVDaepRlL7&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.005095730536,
                    NUMBER_OFFERS: 42,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.501623',
                    },
                ],
            },
            photos: [
                {
                    width: 350,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1610546/market_6F26ugeA3NxPM-o_VGK0Jg/orig',
                },
                {
                    width: 522,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_rrwfX5HpaoxOQeZmDqLhBQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1610546/market_6F26ugeA3NxPM-o_VGK0Jg/120x160',
                },
                {
                    width: 186,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250283/market_rrwfX5HpaoxOQeZmDqLhBQ/190x250',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZGkplB5wMTD0zYhfCornm0HyUKv46qdyy1tFr_UsHhW6w',
            wareMd5: '63YAek3H27ZiQ841uWJwqw',
            skuType: 'market',
            name: 'Держатель с беспроводной зарядкой Baseus Gravity Car Mount Osculum Type Черный',
            description: '',
            price: {
                value: '1339',
                discount: '5',
                base: '1409',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvBSsf529_wVFZzCY0dIZ2e5TRThvONJtYtXBhTcAfo7cAa4h9uy9jY3o2wkbcsxD_VD6xJS2KR17rOxi1MpOj3r89Z0-26IsXuo4gT1Kfvd4isumN6jL4-j_UdqRi4U4fyrvfl7YMbEkOLcGGS6en8fkI84zx232tgftVV1V7M9W2YWI4rMP38REDmyuPD54eD9jkBlF38Xunpi8XIjSJP2JSccWOYOtV_NoOV3vRChLiZEVAO3rMO0CFIgcO5r8a9uslZ0KeG7te6TRjXqFerw7OafRBEQXPz1WZskMJZNLNfiNYrcpYpAkXuy4GktWZmPiQaINSWrzGmvq9rBREuyXNkBgJJNcBUq8-_IzUx_ooQQqUxKqfRRu46Z6aZspVk6auunWT7P35lksuTChZQ0I9-Gt-bhmKQGdyQhGm1bKLok5HT4YNDrPYorTKvZ1J_c9pJBcJ2UsncqYL3rXlge8demkvTWMXNNWEf_ynkFIpYwouxShiR27GWlfhdoEvHc99DBrukwkpDv9U3MLjbd9P8Ds-beHqskUXJBuPTkj6PNYUx_wvOB0ktga09iDCMwxUJg0a7_UoDatyCnUcYrS0naPdq4XkzxSTDDaAv61XEqlazH38i9sOi-fhSYYYXt0vgguYQNqZYNsSg_YtHURGWJb3_L7sp0v-zQlxnA8cUOlEOp1SSueP_r0XDECvxuSENgRdikVT_nCNYfkg15NL3x3S75XIJElaZogwThHNwXtjdRQ91sMNZy79EuahxmDwvn2T2baGInPUWzRFztH4z12R3KIhD4qNXrWaoet4mlNWc4boAWQwE4u8WbsDUhcTLbq-hiSatrQ-xfQJ7o4Z4G1AcsuU_WqQpUjGZaYpwfOCGCH53PsaR8Pp40IXUVYZlq1s0nKxwohKVKxf2B5uXJRY2YTFqkuraLCj54L9Jo3GiYrma5xDPBftIIfYl5ebbG_HCuNzkWKEgbqAwOLhCg_HmIe0seRyQ_EOE2?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d-KRWTwWLT-VnIQLQcaEeYViIpuTS-jHrPZi1vuOSMtVgTCMqfDP_1nbxDxtoFg4s8Z5fDITWWH4Hrdk0WwyxdYcUH5Cqpoa3HgHeaiu5u9j0yrBUJz8S-HcAd-D8DZBrBZrGltrPXFXwtVzo0zSJzbOhICo3iMbaDm879PzihFwwYnengtE4llzzU5ZDKE9eu2YYfv4eqTeolxsDXv9KmNqwitBF6Ir87_SNs-7SoJi5HXkkgwwvKKZwk8YwefT7amt95TvjvrU_A8usB-F7UZb2L7BE8_jJA1tOyoRDvv3SAlB0-MZ0GP&b64e=1&sign=49eaf3dfb9befb39c515af7b95b25dcd&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvBSsf529_wVFZzCY0dIZ2e5TRThvONJtYtXBhTcAfo7cAa4h9uy9jY3o2wkbcsxD_VD6xJS2KR17rOxi1MpOj3r89Z0-26IsXuo4gT1Kfvd4isumN6jL4-j_UdqRi4U4fyrvfl7YMbEkOLcGGS6en8fkI84zx232tgftVV1V7M9W2YWI4rMP38REDmyuPD54eD9jkBlF38Xunpi8XIjSJP2JSccWOYOtV_NoOV3vRChLiZEVAO3rMO0CFIgcO5r8a9uslZ0KeG7te6TRjXqFerw7OafRBEQXPz1WZskMJZNLNfiNYrcpYpAkXuy4GktWZmPiQaINSWrzGmvq9rBREuyXNkBgJJNcBUq8-_IzUx_ooQQqUxKqfRRu46Z6aZspVk6auunWT7P35lksuTChZQ0I9-Gt-bhmKQGdyQhGm1bKLok5HT4YNDrPYorTKvZ1J_c9pJBcJ2UsncqYL3rXlge8demkvTWMXNNWEf_ynkFIpYwouxShiR27GWlfhdoEvHc99DBrukwkpDv9U3MLjbd9P8Ds-beHqskUXJBuPTkj6PNYUx_wvOB0ktga09iDCMwxUJg0a7_UoDatyCnUcYrS0naPdq4XkzxSTDDaAv61XEqlazH38i9sOi-fhSYYYXt0vgguYQNqZYNsSg_YtHURGWJb3_L7sp0v-zQlxnA8cUOlEOp1SSueP_r0XDECvxuSENgRdikVT_nCNYfkg15NL3x3S75XIJElaZogwThHNwXtjdRQ91sMNZy79EuahxmDwvn2T2baGInPUWzRFztH4z12R3KIhD4qNXrWaoet4mlNWc4boAWQwE4u8WbsDUhcTLbq-hiSatrQ-xfQJ7o4Z4G1AcsuU_WqQpUjGZaYpwfOCGCH53PsaR8Pp40IXUVYZlq1s0nKxwohKVKxf2B5uXJRY2YTFqkuraLCj54L9Jo3GiYrma5xDPBftIIfYl5ebbG_HCuNzkWKEgbqAwOLhCg_HmIe0seRyQ_EOE2?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d-KRWTwWLT-VnIQLQcaEeYViIpuTS-jHrPZi1vuOSMtVgTCMqfDP_1nbxDxtoFg4s8Z5fDITWWH4Hrdk0WwyxdYcUH5Cqpoa3HgHeaiu5u9j0yrBUJz8S-HcAd-D8DZBrBZrGltrPXFXwtVzo0zSJzbOhICo3iMbaDm879PzihFwwYnengtE4llzzU5ZDKE9eu2YYfv4eqTeolxsDXv9KmNqwitBF6Ir87_SNs-7SoJi5HXkkgwwvKKZwk8YwefT7amt95TvjvrU_A8usB-F7UZb2L7BE8_jJA1tOyoRDvv3SAlB0-MZ0GP&b64e=1&sign=49eaf3dfb9befb39c515af7b95b25dcd&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvBSsf529_wVFZzCY0dIZ2e5TRThvONJtYtXBhTcAfo7cAa4h9uy9jY3o2wkbcsxD_VD6xJS2KR17rOxi1MpOj3r89Z0-26IsXuo4gT1Kfvd4isumN6jL4-j_UdqRi4U4fyrvfl7YMbEkOLcGGS6en8fkI84zx232tgftVV1V7M9W2YWI4rMP38REDmyuPD54eD9jkBlF38Xunpi8XIjSJP2JSccWOYOtV_NoOV3vRChLiZEVAO3rMO0CFIgcO5r8a9uslZ0KeG7UsKZBRsFZDToUre60cykz4QH41FJ6CXtFhvyRX5Mq9PdEJDuRDY9accolVe1AUtP9rkliBmjrt5wDXXPLTbj_TpbBj5oR39NLXh7WtI8vgBhWAkV31R1PO6HRzSeNXZBJgwLxpJF-Bfxp9kzGjk5vsF8tdtzMUFN8FfDhvqdSN59FkR4NN4iwQo0awpjh72GZfP43TYbHs0WRAkaSXlpqURhrMQb4AjCQDdGrtQWaPiTIFysUKUmNG-tSfzJOsMk0ihw3c7ESvr7rqpqk0jp0wOZZIhe77JNLSSM3qeVKrmjrjr7WlU-k0RpDStDqaVbbm2VTZ7fqg1dkqNPBy8u4gBhH8roxGhUjFDJcJqgwiPqQmqWV5E7SKcBZVmnvahPuOhx9iKdutL_YzXmILcUSiqdNuIY4nzOaManbihRMTYbX8evvd3qgxEtEzqKQDqvpO28VpnhxW4XZgJ-4_EYlM5ffHnMlILPRAy7yTwjiFOSg87oDCIXgpk-lhg-qNZpXa52wRDtE5NfPJg4FndZDq-pNURqBf7FWlEbO4Lyp6iKU1MxFPdO35yPgHDP4zdNalqpv9sjx9NBFgmPKVvkw_UxtOPA4EuWiHl4_tgi_Me3_QhaVxD28SNVvvntzBsMxeJ-ozGl5Pjzy-_GnKWdwCoWf8PAd-5Yge77YfuKhGDiRa_E6gEQdTOY6-4ocWWsgm5mbnrQtVW5q04dBivtQDvuwXJ8uHnz?data=QVyKqSPyGQwNvdoowNEPja3d1OwT5RebO4Zy-C2uHONd8GmFMkgPUnvPDJAlLf_81dJE055E-mZw3i-NUS4ZW2o9yIwSQzC3g5Hnlaaq9d-KRWTwWLT-VnIQLQcaEeYViIpuTS-jHrPZi1vuOSMtVgTCMqfDP_1nbxDxtoFg4s8Z5fDITWWH4Hrdk0WwyxdYcUH5Cqpoa3HgHeaiu5u9j0yrBUJz8S-HcAd-D8DZBrBZrGltrPXFXwtVzo0zSJzbOhICo3iMbaDm879PzihFwwYnengtE4llzzU5ZDKE9eu2YYfv4eqTeolxsDXv9KmNqwitBF6Ir87_SNs-7SoJi5HXkkgwwvKKZwk8YwefT7amt95TvjvrU_A8usB-F7UZb2L7BE8_jJA1tOyoRDvv3SAlB0-MZ0GP&b64e=1&sign=60a13bb234a27a7834f7ec15748ddee5&keyno=1',
            },
            directUrl:
                'https://elementx.shop/product/derzhatel-s-besprovodnoy-zaryadkoy-baseus-gravity-car-mount-osculum-type-chernyy?utm_source=yandex_market&utm_term=%7Bproduct.id%7D',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsP8n0LH1Gm8vakV8RnSkWE91PQJgME-mMYsFVvGNwB_moErVeHUToJWTLRMQTXPFq4wnRJJRezVQfBRChl4g2GInw0_n0b85L4d9yQWVrz00AuWA2xfBljUEfj4JtwWoxUUijoWcr8bwNZH_7KaRDaK1rGCK3UDCiqyMG8DPsEYTHUJQKk7F1X9HOckNqaGWUoYuhDqt1-_DfNThhvJjx0IXEI0AsXMe7tMFsXvE-ADttXdBttbqUsmJoZ6naxFlOZqqw64snEKDbPrXygJTyW5pmNFJC8329b3TCPxsPBpBxYHgkaWbHsrxPqe5E_8UQrDo7Pb2zWBiFVhub7Rf_FolU8zjOupeV9_Nkz5aqt4EXrnPtrscH2UepjmBrQrmi0ICSjoUFNetNo7ovRhH_oOkpIN65ruTEjAEp-BS7BiCnFCv6OtJHtqKc0U1FSOr8Ag4nmOqCAVazvxe38_wETUkgtuhtKiiqm62TCPml96aRnuvC131wxKGCPdW-QkRNhKT1UW2misiyIXsAogvx3C_47yB1OOyC092nUh0R9Slkq1G1RWtSVwU_vtljAmITBObM3sI_qzfTWpudPU2siJIa1ITeK1lzf60hB1xy6nMxUNE7ihiUsQl7H0a_EtGnPUKYVMz44OUMsZSctfPLmv2B146pY2uavnMzJjWNqWME7JzaZaHgYp3s1M5hsdbhCeOpVgNbWNwN2eseVLHd-4O6YcqdaA_aTCCpdWy4Uk6FED6NU0kCScYCCvB0RM-SqS66jS9tZQfgz1J5FnUWQbgN0iF-Xe8mpdQij49W0K4xSAGmW-nbKE4kHQ0nIoZgpKgVi9BbH4khEPhg_JlxBEPSjLlmCI2r_pabkHiue-pVVJqrpJNVryGVCRF3MQJuKoLDHYKYTJeJnVAUfqbMoSbXxaNm9J6csrzMIJC0F8r8lvZTdZ-fS802fbWHNCMcqk9_eCcSdaJNjVERTqtTCSAdBtMtFAbGHnVkLSImfIGyRafKNCK-E?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjUlBkVRWtEB63POvuEokOQS-4zNOVghSYpw7oaxWLRMiU7u0gUKs7sLM4JE_MI3Qfc8o7IOgdtA2RAwcWBX4mYc,&b64e=1&sign=07f6f6c78bd7da56117e272e0816b927&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsP8n0LH1Gm8vakV8RnSkWE91PQJgME-mMYsFVvGNwB_moErVeHUToJWTLRMQTXPFq4wnRJJRezVQfBRChl4g2GInw0_n0b85L4d9yQWVrz00AuWA2xfBljUEfj4JtwWoxUUijoWcr8bwNZH_7KaRDaK1rGCK3UDCiqyMG8DPsEYTHUJQKk7F1X9HOckNqaGWUoYuhDqt1-_DfNThhvJjx0_e9_EkDDlZzj5sX7Bvtf0cVg4tNBoHOHLJgmk5VBB6HOhUeQZDtW7TrwZjo22rFE6GBnF5A5V_54c-V9WRZAzzGTXnP4cEmt2zCCP4ZrjVhjocQ7ZYwY9hum4jwUqUD6TH6ODvwk0D8SIyhSOWUsizyX-omEYppkO4AtdN0zi73PPdbFz7YZ4seKQVp727eTCFa_MDFruyK6fIlCX-GTUxX9L_RglEcCcK8EpqhgczSnrRhgWFyWn22gcXcKVcyVU_FSlChKwfktJIlZ69dEosuejIGaAus2COrWoVas2-QCqkR4twaohuBnzdPNBjhES33W2f9tmJTgclVqKbiTLb_eQNRwH-0woGRdYdosq5nOLnVkRJJv2bYXlrtVhtbkVBBeLr4bgNnXjsSSYhECEZ9_hddHDxmwtL2pBs6tg5HOM3HH55ZzZzzldhNxoNvIsxVDywkwdNCyhTM5lrCQo8Z4Y3uQDF_11xmpEUyr75HvB1pG_T9QjDtAyYy-wYw9NOjVqLLesX46FaT7FnwlDTE5GPPvNxftmPtsIIRfXws8cSX03aw2E_P55epwUsBH9lfY0Yf2Bd7GVnVAwwPbEAzr0b7HtF0nLTIAwiPDGvId-bpc2fZiW_vB2vH6vmEoTVXV_ZWoVdQkE0vfYdyylEUIjfuunggCp_RLg74w1d0LEWI-kB6BVDZKVvlN8x00BbSwIlLRwDISGX6hqBXZ79_KKSWFE5o54fQOFm_Pai8Yr0vTvxHnlIjakTVEluLH1lS3M-1fSPn2ktnk_onB40d5FQJTwtkd?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bRiACOQ5zZvdH8Zk8kGpff7Jr8jQYpzjXzjs1c_itgASnnWQ4tNfExXzRuQ04qilKSMSiOfYQywOyV4J22dQBkHVVwieHsJ8NAwKi9z1foM_C11nWScR7o,&b64e=1&sign=e6e6209b9ef8214f604c262026728bf0&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.3,
                    count: 51,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 2,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 1,
                            percent: 6,
                        },
                        {
                            value: 5,
                            count: 13,
                            percent: 81,
                        },
                    ],
                },
                id: 546830,
                name: 'ELEMENTX',
                domain: 'elementx.shop',
                registered: '2018-11-28',
                type: 'DEFAULT',
                opinionUrl: 'https://market.yandex.ru/shop--elementx/546830/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 339963360,
            },
            onStock: true,
            phone: {
                number: '+7 4996771466',
                sanitized: '+74996771466',
                call:
                    'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvBSsf529_wVFZzCY0dIZ2e5TRThvONJtYtXBhTcAfo7cAa4h9uy9jY3o2wkbcsxD_VD6xJS2KR17rOxi1MpOj3r89Z0-26IsXuo4gT1Kfvd4isumN6jL4-j_UdqRi4U4fyrvfl7YMbEkOLcGGS6en8fkI84zx232tgftVV1V7M9W2YWI4rMP38REDmyuPD54eD9jkBlF38X7b7ncJjmyruZISbDjwGwlbx2DQWLCWBkAWw5P4huf7gFU1x4C2SBwQsdsvBpR7bFIWWmi5H8D582KLkuvJerX7hU0zryHdG0RQewkjY8tu4fpOmw-jNTTOfSpmo3mc40P1w64I_B3SzBdAbSewRwHgBwra7fKBNLCH-FCs7RQVtj2QTp2_Sl0fAWh9rmc3_x41iRpZdd7NtUNqXs7RQVgW3GTlGAMuHp4YPY8n-sI6AugH8kbmwt8wN2JMbf6kARWw8bMkyN4Y3pWx0aUUeL74ejjjtw08rbYObxk70f0bPTOT9NFUoxS2osnLMm89PSdamWIeyMkwb5RF1jQx4OqEGSpuuYIhxVEzoiLZPrz44xNy_VOhgKzJQg4K58W2o1CQunXwh3A8PIv-k-mjAdJ-pmrZjTAThg2_bfVBW7y29ecT2sijR5JXGKtmzhEe6LFreT0dCSKKgiZx2_ibxYQXacJHn5tu_IQ_sMtYnv-5WE4OKHnxJTVlQ4H9-D3ENGznIbNpWFWAgiUHQPNaWXKfXsQGi-ebwntTCZQVNadXGSQ3Odag-Vfi5g41__aU_pcHCw7WNN1M7cL9KT4673rd3zUtKnLE7Ee7UNjztLQg3hwRH-d7_3l2C_IrWejTomyCtec5pk2kzMUUaytgVZ8FK3uHZsEuioesYPwwkUghCtAGVV9_qGtqOHtF3U8ah1ujh7pKldtjoG0_jLAUOynp1XIcpZQ4Pm8h6NBcDmc2jQ7WWH_NbBnfC08WkR6eYh49A4fkvj3lWiwD4-yg638BKvtL8MzxdH?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-FX6jY5Bfxgsn7JZP_nw2xwTAX_tDMJ-lMrVJCGgYNh8oxiZ4IF88l-F_0KmQrXDk5tJwX6qjOtiTOSvgMISXK2kBZKReQ-kBRFOvFRsLXL0-1GQD5hYX6XHioc0_w13actuBpv6Oxpy4LjMrgKYyfdAdVhFwcfMJorJc5MFW1ow,,&b64e=1&sign=55ad2ef9363ef7c71368869733473318&keyno=1',
            },
            photo: {
                width: 674,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1641365/market_OU8k3u2-PM9dXcmVWzVAHA/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 21,
                        },
                        brief: 'завтра при заказе до 21:00',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 15,
                        },
                        brief: 'сегодня при заказе до 15:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/63YAek3H27ZiQ841uWJwqw?model_id=339963360&hid=91502&pp=490&clid=2210590&distr_type=4&cpc=uk09sP8vE5lte84jN67LWxUH65SeBaBoyoXAQLoq5Bq3qrUXsOnkId7eJc-yECHAalLxsVMUzPsLEMuF1kMWLEHZJrx9ZOs9xv4d71N2E5D7HyoZwq9SzD84jy7npCBABKPZWKU1MDFUhBM3i65F7OW2E9rw1xCX&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.005095730536,
                    NUMBER_OFFERS: 30,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.479333',
                    },
                ],
            },
            photos: [
                {
                    width: 674,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1641365/market_OU8k3u2-PM9dXcmVWzVAHA/orig',
                },
                {
                    width: 701,
                    height: 652,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1857979/market_xunJJn-Qr7UQnr6I2nlJBA/orig',
                },
                {
                    width: 701,
                    height: 608,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_p9ehPosW9w89DPF0pNtPbA/orig',
                },
                {
                    width: 683,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1670981/market_oZzc6vIk79SAxYM4ZZ9EFA/orig',
                },
                {
                    width: 701,
                    height: 618,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779269/market_tKQJuUOq_e757Zjbunjo5A/orig',
                },
                {
                    width: 533,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_PYXZuS0Ehmnq9XeLgJE_PQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 153,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1641365/market_OU8k3u2-PM9dXcmVWzVAHA/120x160',
                },
                {
                    width: 190,
                    height: 176,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1857979/market_xunJJn-Qr7UQnr6I2nlJBA/190x250',
                },
                {
                    width: 190,
                    height: 164,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_p9ehPosW9w89DPF0pNtPbA/190x250',
                },
                {
                    width: 155,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1670981/market_oZzc6vIk79SAxYM4ZZ9EFA/120x160',
                },
                {
                    width: 190,
                    height: 167,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1779269/market_tKQJuUOq_e757Zjbunjo5A/190x250',
                },
                {
                    width: 190,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/217370/market_PYXZuS0Ehmnq9XeLgJE_PQ/190x250',
                },
            ],
        },
        {
            __type: 'model',
            id: 567008242,
            name: 'Держатель с беспроводной зарядкой Deppa Crab IQ',
            kind: '',
            type: 'MODEL',
            isNew: true,
            description:
                'держатель для автомобиля, место крепления: воздуховод, панель/лобовое стекло, способ крепления: присоска, зажим, подходит для смартфонов, макс. диагональ: 6.50 ", зарядное устройство',
            photo: {
                width: 458,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1687058/img_id8186289056368281002.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 458,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1687058/img_id8186289056368281002.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 575,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1930823/img_id687303085072371957.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1990',
                min: '1920',
                avg: '1990',
            },
            vendor: {
                id: 6516710,
                name: 'Deppa',
                site: 'https://www.deppa.ru',
                picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id2610281269771821442/orig',
                isFake: false,
            },
            rating: {
                value: -1,
                count: 0,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-deppa-crab-iq/567008242?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-deppa-crab-iq/567008242/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 3,
            opinionCount: 0,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZEiqcoBnZWiO37Me003q-B3hWa8uQqy7E6tz925JUwCZQ',
                wareMd5: 'Vuggc4eD_tAQwd-0Fj4r7Q',
                skuType: 'market',
                name: 'Автомобильный держатель Crab IQ для смартфонов 4"-6.5", Deppa',
                description:
                    'Достаточно поместить смартфон в автомобильный держатель, подключенный к гнезду прикуривателя, и зарядка начнётся автоматически. Вам не нужно беспокоиться о расходе заряда при использовании гаджета в качестве навигационного устройства. Держатель оснащен кронштейном, который позволяет удобно настраивать угол наклона и высоту для комфортного обзора.',
                price: {
                    value: '1920',
                },
                cpa: false,
                directUrl:
                    'https://kotofoto.ru/moskva/shop/uid_230761_avtomobilniy_derzhatel_crab_iq_dlya_smartfonov_4_6_5_deppa.html?roistat=Market_msk_307-smartfony-i-telefoniya&utm_source=market&utm_medium=cpc&utm_campaign=msk&utm_content=avtomobilniy_derzhatel_crab_iq_dlya_smartfonov_4_6_5_deppa&utm_term=230761',
                shop: {
                    organizations: [],
                    id: 281111,
                    outlets: [],
                },
                model: {
                    id: 567008242,
                },
                onStock: true,
                phone: {
                    number: '8 (800) 505-43-75',
                    sanitized: '88005054375',
                },
                photo: {
                    width: 1000,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1910582/market_yNq6gyY8gxC1KqrufaPX5Q/orig',
                },
                delivery: {
                    price: {
                        value: '250',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 250 руб., возможен самовывоз',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '250',
                                },
                                daysFrom: 2,
                                daysTo: 2,
                            },
                            brief: '2&nbsp;дня',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 6516710,
                    name: 'Deppa',
                    site: 'https://www.deppa.ru',
                    picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id2610281269771821442/orig',
                    isFake: false,
                },
                warranty: true,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.003042768221,
                        NUMBER_OFFERS: 10,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.949189',
                        },
                    ],
                },
                photos: [
                    {
                        width: 1000,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/1910582/market_yNq6gyY8gxC1KqrufaPX5Q/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/1910582/market_yNq6gyY8gxC1KqrufaPX5Q/120x160',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.466231',
                    },
                ],
            },
            showUid: '15717800175384431183316025',
            url:
                'https://market-click2.yandex.ru/redir/C-RMiAr-MEeCjhvu2MRw9uZFQ1WOGnjNw0nAvZuIa9-Ox1EbR8dWx6Irr0vnszLn90DiJYcnyDG9WJy7sqly_sgnaJBu6W133D4ak-0J--C793I99Au_Tb51IGc-KqTx_oGPEf2p4sXoxoXcssA1zZvC64Bl9HqS1aZktmDZeHAvOVHCEdWCs0ppTZzztyUW2TKAU0LMBWhk67PSJBNt5R7DzSFVIJAJskc5dFiF7xkHfwiM6npz7qgYDV1QZuFu8ySwzYYEV0r_uUQr_0724ObWxWKzys9t3pyMeKZiSEZrOc93JyyjhhO9Rya-zO16klceU0mrUJMn8lCp3JkgVp34Y6WCax5D02gG7sKOAgGHlZibdu6w5AxeoRSNnXjcxhERsMYmRfvUMe5MzyiOc25XdszqAbK49WvYG-L-8Sc7tDr0MNsMx3-IT_Eb_kdRx-6eEEnQV29kwlMSgeExgQjuoIDkgj-IFw9airFT9p0,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5ELMXbquG1iQx491349byMGnNlcoC57BRERI6hSAQvLMe98EWIorq7OHYavXM65RAHV79GYttspMnhDro94Uiopdq6IigMQr9dP&b64e=1&sign=7b6b7a3803b28e0523b27735286b8570&keyno=1',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-deppa-crab-iq/567008242/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZEbJup9DLmhrysgMqhHjwL7wvBultVc-6lW2cQiHa6p9Q',
            wareMd5: 'rAJ6Rpq-B3wuxVbVLTKPqA',
            skuType: 'market',
            name: 'Baseus Автомобильный держатель с беспроводной зарядкой Rock Solid Черный WXHW01-01',
            description:
                '→ Надежная фиксация смартфона. → Интеллектуальная беспроводная зарядка. → Максимальная мощность: 7.5W (Apple), 10W (Android). → Встроенный инфракрасный сенсор. → Место крепления: дефлектор.',
            price: {
                value: '1999',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvw65PTtoFhtXHeTTfD80PwqMKj79-KGN7M3EOuw8CxSbXmSJpftJ-pI6MkGMgqH_8hW163fqPBeTChrllenePJ5EGvj8C9Dgu8rXpyg8I_gOc7bDYoVLgxh7Feet41a-xvsSQLKK9UDMD2TTQfT2fE4DLhHZ5mVvuxqSEqQu_UGMDEKYialrjQVpNyOLNCAE7UqsJNWKeMWjaHKKT_ESEwytAjChxkTkO3YoBMeVs40HNYQevylCowkEN-Q5qHbYW49Z6pm2H0VdEzr57XoPthp2wKaYnt0Zq5x7aiZ85muncNNRzZOXJpsXyLu3g50BPqpAqOWPbLFmMbweDMB_d3lj-nlU9Sw6zHXXK-_JHvaABPFnyyawqu5Ry258S0KdwwYLkT--bgoSsZv6eEgghvUusoSjUthtwXfdtaHdOywHZmbklc0vZI4xrZRMZ6mStJZ26bQF338YzlEOyW2kYhCogQzB63hFClPUY9wBhokfYT33_cFThoQSn3jd23SQsDw3COSRMicSzlJ_88KTy8o1T6Bkhf9bFji18ZbFyUD8CI-xV1womG_qwkpHHovDU5fpvgQIHRH-qoSDTFt5wmPIF-8V2BBuXSftneDL9GaeGdxkFN9zJYAFk8VromukKauShBYWlxdcSkr6qT2XdpXiaQKaKyT9dwbQEoPrTLKGbFyFPcVhyl0U-ybyOWyAtPFFsWg8K5C6mX56JfQeraDCn8z0MwTqyOigvoT8MyYLzFcPxoWgwH7CXyJyvQXDBO4PU8ZAYHZS41BYwyjKcbhcvRnpoVE8DAz_MLTsLzP949td9BgSguBK8JuPy-PF-BkS2yjAD4ywdRpnh6SkJZPY1_fIVoQnqIVN8w1y3_1ZS2aDWYL3evkuRUFD2EIapdVs69XcAT8pIb8KE_yzj9XJnssP71jghVwQ2zPNSxY?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_MB000GSFlddTYJPhCYZEyzGDN-IgKA6GjDS07I9r0ZPQrHfVwHKMBYDrsixTi_tarKB_rqzDNXew5TJJMk3NR7kCertOsRZWHoP9cMPbT9rZpYlkRmXmKB-XYGSpLKynBqC4IHgXJGWnEeVSU7C8-dyU_aQ-gbE2T_jOnm3prfXGHQyT4HPESb9Sfqoug-85BDK1M_pcyXprfPBSqZI1YB-uRwUHWdWFZW6e5KB8imWaOCIHasg2qZbnHYZAGwStpiBJhr0faUEIhnN7-LfcWM7lWyuN8eWsCRsPfmgVRWcMrAvs4iaD6Kxm5O3dw1ELhuXNY6q8fH3MBdU56EgDkXFJvnFmaY6HUmM0lCYvCG0_Lmg6cTJK_eNWTYJL82l8YJw4bfdVwy1vupXCTf_4CKO1rWmQqCByip93mgfwPxZrAoiH5G950zhLzNs60Nbl73oIaTYNDPwg2vbv8j3Y-S4sg6bP42mvDueQuyk8LmPeRVM1f8Q7jHH-5rovTI_hhPA_2bwNa75a7JC-qCT2q0QW1Nx_K8fluBN7I4z4buOqwXtSUZsCHTolX5BH7-QF8v5OPPWigZPl53v6oSogD&b64e=1&sign=743fe154d92d8a3c9efadd243dbaa92a&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvw65PTtoFhtXHeTTfD80PwqMKj79-KGN7M3EOuw8CxSbXmSJpftJ-pI6MkGMgqH_8hW163fqPBeTChrllenePJ5EGvj8C9Dgu8rXpyg8I_gOc7bDYoVLgxh7Feet41a-xvsSQLKK9UDMD2TTQfT2fE4DLhHZ5mVvuxqSEqQu_UGMDEKYialrjQVpNyOLNCAE7UqsJNWKeMWjaHKKT_ESEwytAjChxkTkO3YoBMeVs40HNYQevylCowkEN-Q5qHbYW49Z6pm2H0VdEzr57XoPthp2wKaYnt0Zq5x7aiZ85muncNNRzZOXJpsXyLu3g50BPqpAqOWPbLFmMbweDMB_d3lj-nlU9Sw6zHXXK-_JHvaABPFnyyawqu5Ry258S0KdwwYLkT--bgoSsZv6eEgghvUusoSjUthtwXfdtaHdOywHZmbklc0vZI4xrZRMZ6mStJZ26bQF338YzlEOyW2kYhCogQzB63hFClPUY9wBhokfYT33_cFThoQSn3jd23SQsDw3COSRMicSzlJ_88KTy8o1T6Bkhf9bFji18ZbFyUD8CI-xV1womG_qwkpHHovDU5fpvgQIHRH-qoSDTFt5wmPIF-8V2BBuXSftneDL9GaeGdxkFN9zJYAFk8VromukKauShBYWlxdcSkr6qT2XdpXiaQKaKyT9dwbQEoPrTLKGbFyFPcVhyl0U-ybyOWyAtPFFsWg8K5C6mX56JfQeraDCn8z0MwTqyOigvoT8MyYLzFcPxoWgwH7CXyJyvQXDBO4PU8ZAYHZS41BYwyjKcbhcvRnpoVE8DAz_MLTsLzP949td9BgSguBK8JuPy-PF-BkS2yjAD4ywdRpnh6SkJZPY1_fIVoQnqIVN8w1y3_1ZS2aDWYL3evkuRUFD2EIapdVs69XcAT8pIb8KE_yzj9XJnssP71jghVwQ2zPNSxY?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_MB000GSFlddTYJPhCYZEyzGDN-IgKA6GjDS07I9r0ZPQrHfVwHKMBYDrsixTi_tarKB_rqzDNXew5TJJMk3NR7kCertOsRZWHoP9cMPbT9rZpYlkRmXmKB-XYGSpLKynBqC4IHgXJGWnEeVSU7C8-dyU_aQ-gbE2T_jOnm3prfXGHQyT4HPESb9Sfqoug-85BDK1M_pcyXprfPBSqZI1YB-uRwUHWdWFZW6e5KB8imWaOCIHasg2qZbnHYZAGwStpiBJhr0faUEIhnN7-LfcWM7lWyuN8eWsCRsPfmgVRWcMrAvs4iaD6Kxm5O3dw1ELhuXNY6q8fH3MBdU56EgDkXFJvnFmaY6HUmM0lCYvCG0_Lmg6cTJK_eNWTYJL82l8YJw4bfdVwy1vupXCTf_4CKO1rWmQqCByip93mgfwPxZrAoiH5G950zhLzNs60Nbl73oIaTYNDPwg2vbv8j3Y-S4sg6bP42mvDueQuyk8LmPeRVM1f8Q7jHH-5rovTI_hhPA_2bwNa75a7JC-qCT2q0QW1Nx_K8fluBN7I4z4buOqwXtSUZsCHTolX5BH7-QF8v5OPPWigZPl53v6oSogD&b64e=1&sign=743fe154d92d8a3c9efadd243dbaa92a&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZvw65PTtoFhtXHeTTfD80PwqMKj79-KGN7M3EOuw8CxSbXmSJpftJ-pI6MkGMgqH_8hW163fqPBeTChrllenePJ5EGvj8C9Dgu8rXpyg8I_gOc7bDYoVLgxh7Feet41a-xvsSQLKK9UDMD2TTQfT2fE4DLhHZ5mVvuxqSEqQu_UGMDEKYialrjQVpNyOLNCAE7UqsJNWKeMWjaHKKT_ESEwytAjChxkTkO3YoBMeVs40HNYQevylCowkEN-Q5qHbYW49Z6pm2H0VJ6KBn9AKDds6wmATsb-LL-iu1kAJCV5-KZj-AaLWSl5Iz2FnmCQNuckgNwQQ38X7FCJv5o4qWobp0GEyWHZwtpufEZuBdSKaj5-xZ9xA2Ft1vroCuXIEnhOQ19ve1kLBWqnqUhiO7TXKJA7WN7KbXQAA7hjbaItXVJ0AC0AclZB3h2oAkZPayw53nm36Om88F-Mhsaee_8RC80ER9BQEB9uYAUNNe-zD2C2EKeMP-chQ0xkRcpE4DhTPFj6j8i53LzLIIw1u8TclgFRnSjOXHzwyXEFUy56_6hF0xLGRJjODsIJF4srxFokF63csWTBWpEc9wUVTHMoFJXncEP7t8r6XQYa_SJXHbUNrMM6SaKDK_kE3o-L8BHvxhqN7ipvoryMSpt4u41Dva-dzFuFqPpAEBUwzj1sSdff5jHKKyWep6HVMcagyYbfgIhf8KwIEDaImbLKGxXhBRteVP5knp12PP5TxmHWnE9dz9XJcPuGLtTVkdOpZ_AMwx_jKlGif6su_YYvjdNo58vxSyxY7enCKmdvsVlHk6g8zOjoKhHOUDFymh08obPVmJCLIkJwpj_sjzNmyKk73Ep--EjSn7cakA1jgC0Lvc5jiz22cbR0B5Me_hHcZiBjNkf-FVY_6j8tToVuOefU9A6Qqs6qsUXMtWHQtrE0b?data=QVyKqSPyGQwNvdoowNEPjZpsIoD7trStp7HWTUyWiDA2If3cM-tUZ4SyTysr1u_AaAi0O-A910_MB000GSFlddTYJPhCYZEyzGDN-IgKA6GjDS07I9r0ZPQrHfVwHKMBYDrsixTi_tarKB_rqzDNXew5TJJMk3NR7kCertOsRZWHoP9cMPbT9rZpYlkRmXmKB-XYGSpLKynBqC4IHgXJGWnEeVSU7C8-dyU_aQ-gbE2T_jOnm3prfXGHQyT4HPESb9Sfqoug-85BDK1M_pcyXprfPBSqZI1YB-uRwUHWdWFZW6e5KB8imWaOCIHasg2qZbnHYZAGwStpiBJhr0faUEIhnN7-LfcWM7lWyuN8eWsCRsPfmgVRWcMrAvs4iaD6Kxm5O3dw1ELhuXNY6q8fH3MBdU56EgDkXFJvnFmaY6HUmM0lCYvCG0_Lmg6cTJK_eNWTYJL82l8YJw4bfdVwy1vupXCTf_4CKO1rWmQqCByip93mgfwPxZrAoiH5G950zhLzNs60Nbl73oIaTYNDPwg2vbv8j3Y-S4sg6bP42mvDueQuyk8LmPeRVM1f8Q7jHH-5rovTI_hhPA_2bwNa75a7JC-qCT2q0QW1Nx_K8fluBN7I4z4buOqwXtSUZsCHTolX5BH7-QF8v5OPPWigZPl53v6oSogD&b64e=1&sign=cd317e9b474a3dced12be712345b9e38&keyno=1',
            },
            directUrl:
                'https://www.lifeproof-store.ru/products/baseus-avtomobilnyij-derzhatel-s-besprovodnoj-zaryadkoj-rock-solid-chernyij-wxhw01-01',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.9,
                    count: 491,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 3,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 4,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 0,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 10,
                            percent: 2,
                        },
                        {
                            value: 5,
                            count: 388,
                            percent: 96,
                        },
                    ],
                },
                id: 239284,
                name: 'lifeproof-store.ru',
                domain: 'www.lifeproof-store.ru',
                registered: '2014-07-10',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Луговой проезд, дом 2, 109652',
                opinionUrl:
                    'https://market.yandex.ru/shop--lifeproof-store-ru/239284/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            onStock: true,
            photo: {
                width: 700,
                height: 700,
                url: 'https://avatars.mds.yandex.net/get-marketpic/936727/market_3czlxzFYgdHlDpvO45f0EA/orig',
            },
            delivery: {
                price: {
                    value: '300',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: false,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 300 руб.',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '300',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                        },
                        brief: 'завтра',
                    },
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '500',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 14,
                        },
                        brief: 'сегодня при заказе до 14:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/rAJ6Rpq-B3wuxVbVLTKPqA?hid=91502&pp=490&clid=2210590&distr_type=4&cpc=ryaKHLIkvEOHpNe3UoKMqVCevWZYZDZywR0NINDSeLNbWbBTcxSSSKOUcESJXhkuFInqo4CJrWIPkUqOHoFcO5MvH4RG34tDDKD6VCt2gqX9E8RCM6t90VqxnZdvx9EOP_FUmNYQpS_srT_3la6l8NEJqM0D4QFJ&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            promo: {
                type: 'promo-code',
                promoCode: 'Market19',
                discount: {
                    value: '5',
                },
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.004209954292,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.445984',
                    },
                ],
            },
            photos: [
                {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/936727/market_3czlxzFYgdHlDpvO45f0EA/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/936727/market_3czlxzFYgdHlDpvO45f0EA/120x160',
                },
            ],
        },
        {
            __type: 'model',
            id: 97199084,
            name: 'Держатель с беспроводной зарядкой Buro CWC-QC1',
            kind: '',
            type: 'MODEL',
            isNew: false,
            description:
                'держатель для автомобиля, место крепления: лобовое стекло, способ крепления: присоска, подходит для смартфонов, макс. диагональ: 6.20 ", зарядное устройство',
            photo: {
                width: 494,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1363658/img_id6022283428489778548.jpeg/orig',
                criteria: [
                    {
                        id: '13887626',
                        value: '13899071',
                    },
                    {
                        id: '14871214',
                        value: '14899090',
                    },
                ],
            },
            photos: [
                {
                    width: 494,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1363658/img_id6022283428489778548.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 449,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id3552734304733975235.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 369,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id3514947473063808104.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 564,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1365202/img_id3224109714710114236.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 445,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1363658/img_id1062944400924152346.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 547,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1363658/img_id2803291554263449101.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 701,
                    height: 283,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id3135576727477437144.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 427,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1360852/img_id6929398933652241694.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 539,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id757690764352123370.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 601,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id5928089084809008347.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 557,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1060343/img_id111410301074402129.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 463,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1220464/img_id6582545595423290025.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
                {
                    width: 437,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-mpic/1215212/img_id4284472586079358982.jpeg/orig',
                    criteria: [
                        {
                            id: '13887626',
                            value: '13899071',
                        },
                        {
                            id: '14871214',
                            value: '14899090',
                        },
                    ],
                },
            ],
            category: {
                id: 91502,
                name: 'Держатели для мобильных устройств',
                fullName: 'Держатели для телефонов, планшетов, навигаторов',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            price: {
                max: '1290',
                min: '1290',
                avg: '1290',
            },
            vendor: {
                id: 4978228,
                name: 'Buro',
                site: 'http://buro-tech.ru',
                isFake: false,
            },
            rating: {
                value: -1,
                count: 0,
                distribution: [
                    {
                        value: 1,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 2,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 3,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 4,
                        count: 0,
                        percent: 0,
                    },
                    {
                        value: 5,
                        count: 0,
                        percent: 0,
                    },
                ],
            },
            link:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-buro-cwc-qc1/97199084?hid=91502&pp=490&clid=2210590&distr_type=4',
            modelOpinionsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-buro-cwc-qc1/97199084/reviews?hid=91502&track=partner&pp=490&clid=2210590&distr_type=4',
            offerCount: 1,
            opinionCount: 0,
            reviewCount: 0,
            offer: {
                id: 'yDpJekrrgZEFCQfZhDL0uNj2I9vVgXdyEurC5pfEiKaLPN02Kr1pOQ',
                wareMd5: 'iH_PRXakD-wR-AHTwaJrTg',
                skuType: 'market',
                name: 'Беспроводное зарядное устройство Buro CWC-QC1 QC3.0',
                description:
                    'Беспроводное зарядное устройство Buro CWC-QC1 QC3.0 - описание: Беспроводное зарядное устройство предназначено для зарядки аккумулятора мобильного устройства, без подключения кабеля к разъему устройства. Используется только с устройствами, имеющими ресивер-приемник стандарта QI. Назначение: для мобильного телефона/плеера/gps-навигатора Разъем зарядного устройства (на конце кабеля): Microusb 2.0 Тип питания: от порта usb Сила выходного тока: 1 A Совместимость для бренда: универсальное Материал: пластик Вес',
                price: {
                    value: '1290',
                },
                cpa: false,
                directUrl:
                    'https://www.holodilnik.ru/digital_tech/digital_tech_rechargers/buro/cwc_qc1_qc3_0/?utm_medium=cpc&utm_campaign=yam_msk&utm_term=553813&utm_source=market.yandex.ru&aid=yam_msk',
                shop: {
                    organizations: [],
                    id: 632,
                    outlets: [],
                },
                model: {
                    id: 97199084,
                },
                onStock: true,
                photo: {
                    width: 705,
                    height: 1000,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/171655/market_f9g-ir-bw4CgwulpDnPuEQ/orig',
                },
                delivery: {
                    price: {
                        value: '300',
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: false,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                            nameAccusative: 'Россию',
                            nameGenitive: 'России',
                        },
                        nameAccusative: 'Москву',
                        nameGenitive: 'Москвы',
                    },
                    brief: 'в Москву — 300 руб.',
                    inStock: true,
                    global: false,
                    post: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки',
                            },
                            conditions: {
                                price: {
                                    value: '300',
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 13,
                            },
                            brief: 'завтра при заказе до 13:00',
                        },
                    ],
                    deliveryPartnerTypes: [],
                },
                vendor: {
                    id: 4978228,
                    name: 'Buro',
                    site: 'http://buro-tech.ru',
                    isFake: false,
                },
                warranty: true,
                recommended: false,
                isFulfillment: false,
                paymentOptions: {
                    canPayByCard: false,
                },
                isAdult: false,
                restrictedAge18: false,
                benefit: {
                    type: 'default',
                    description: 'Хорошая цена от надёжного магазина',
                    isPrimary: true,
                },
                trace: {
                    factors: {
                        CATEG_CLICKS: 1660,
                        SHOP_CTR: 0.003971779253,
                        NUMBER_OFFERS: 9,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpcBuy',
                            name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                            value: '0.142017',
                        },
                    ],
                },
                spasibo: {
                    receive: {
                        points: 25,
                        percent: 2,
                    },
                },
                photos: [
                    {
                        width: 705,
                        height: 1000,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/171655/market_f9g-ir-bw4CgwulpDnPuEQ/orig',
                    },
                ],
                previewPhotos: [
                    {
                        width: 176,
                        height: 250,
                        url:
                            'https://avatars.mds.yandex.net/get-marketpic/171655/market_f9g-ir-bw4CgwulpDnPuEQ/190x250',
                    },
                ],
            },
            trace: {
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.439105',
                    },
                ],
            },
            showUid: '15717800175384431183316027',
            modelSpecificationsLink:
                'https://market.yandex.ru/product--derzhatel-s-besprovodnoi-zariadkoi-buro-cwc-qc1/97199084/spec?hid=91502&pp=490&clid=2210590&distr_type=4',
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZE6knAWIn1wf-2_v48ICeiOgG1_IfchC_yUvP0KAbw5Bg',
            wareMd5: 'l1dqV37K1N-NwJXs8xOesQ',
            skuType: 'market',
            name: 'Держатель с беспроводной зарядкой Baseus Rock-solid Electric Holder Wireless charger черный',
            description:
                'Алюминиевый держатель премиум-класса, со встроенной беспроводной зарядкой, автоматическим зажимом и креплением на вентиляционную решетку.',
            price: {
                value: '1990',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj-VL46lT1quui8QQbfsw69M0qLNXOrF_r78sVrO0waSxzx5X32OgjKKDXQgnMoZYfceCXbcYH4xZbhmj1vFOnf19ukBVg8X7vjvZ4i5YKyw1gDEMlWLwlR3ueAt7DRZMc0HfLoWNIVlgx4J3ZLwX6XoPWyml3xF_XeQDcs0CRTXHAkf7TLS1Vv6NS8GcVovvF7Dn_QV1E4F6o4xA5Oz7hgQ32x114E3aDpcDvUfPnT1-MRetYAOKXfrOKLUh072Y9JDxcjbDPKaITJNbvrEqEPk8xDoaDGipT-C1cfgwYmSs9f6dVJb8eNa1y6wvoUXSv4HDA0XuiUlTF2iqGsva07195eVic98kSDMVEfC68KPyfV8OWau5Njn25IPmmT0-yP2f5hPW_gB_HHM-ADnnzI9gV7QkFTiGj7zmKFR8efiI2FXB4ZJau9adZg6TCGDdsUutwVO96cBgjFi6OChu_1jFuKiH9G5DkbkqZcJJm-ZWX6mDvnqOXIOLJ-PQgDKGhAVgSC0hhW42Cee0TxX7nq-E6XyzGqbWE9yf9Rae8F-ksqxz7QgG9T-EVCsHJ-fPgI2Jv15SYAzSWm69X3sjQVXdLQMn6CE9O1sAaESIWjttaO_p0a_cTAGGHlnhqHvQUJ-j4gyXnu-TD5_-PvsrPtGoC9F0BfWaxbe1pdDlaXBU8iAr0xDuiAWtJ4BHHH98f5RrS-sNg34xe1WKgI8raIwmvIs9BlM-NDADscvt4EVuKH2a2en3rFjz6M2Xkd8WBXx2GqmzicQRoQ1BVv_X0SaUrp6UvKaR12O0qqs6jiecQ3AD9pPeJHvs_d0-k2YCmaA-rDedV-55MFvAwZu2rS0JEvqV9QTMiEROn-rAGJOFntqCTcxgW4ZbJeNI3PcQE4_DhJv5KWZ5KwdpP4tcH3z4Ronp0SI6in1GwuMn9uZ?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5FJBoUHsYSgaEOQOJBmkIJgxapvD77bYE1tBXrVpLNij2I67fP7oqqp2wM-aMadKOnLEBCN0JSx8t9cvq_A6PXVcgNZO-pz55iLCq0uKY6-jKWbu-mC5DjeYhI3zGKImzfNKrHUHEC1AIIMfCHwVYO16lbb8-5x9hkKJXUPAVI5Di2aWFzhbA-maZB1fxmog9Y66HFbxFRzvnSjW6-cwnokjsQJ4kpwG9ZeNIGf4pW9BGU3G1WM6QPXJt555VBXf_Stam-6uKYZtXBNwshsWr1I_wBH_bzEXWx26xnM0w3J-ii6soc52oyg2W6K0e1cyxc0VJIHDDKzCfmiXOPDJJ8VP1IY7GcDIcqnhFVDeqCj2ko6I_59Eb8cwtj-DDJ-96AVFjdjly0doCUSdAcz1oskY8LkWHc4QUb0egCVeVUKOBxJW5zK3u7LxTojVdjmnd5Hzb6PN4iMmoi2UrHlxXWM,&b64e=1&sign=bc3268ee8cd27b2a5577a5c8131bdbab&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj-VL46lT1quui8QQbfsw69M0qLNXOrF_r78sVrO0waSxzx5X32OgjKKDXQgnMoZYfceCXbcYH4xZbhmj1vFOnf19ukBVg8X7vjvZ4i5YKyw1gDEMlWLwlR3ueAt7DRZMc0HfLoWNIVlgx4J3ZLwX6XoPWyml3xF_XeQDcs0CRTXHAkf7TLS1Vv6NS8GcVovvF7Dn_QV1E4F6o4xA5Oz7hgQ32x114E3aDpcDvUfPnT1-MRetYAOKXfrOKLUh072Y9JDxcjbDPKaITJNbvrEqEPk8xDoaDGipT-C1cfgwYmSs9f6dVJb8eNa1y6wvoUXSv4HDA0XuiUlTF2iqGsva07195eVic98kSDMVEfC68KPyfV8OWau5Njn25IPmmT0-yP2f5hPW_gB_HHM-ADnnzI9gV7QkFTiGj7zmKFR8efiI2FXB4ZJau9adZg6TCGDdsUutwVO96cBgjFi6OChu_1jFuKiH9G5DkbkqZcJJm-ZWX6mDvnqOXIOLJ-PQgDKGhAVgSC0hhW42Cee0TxX7nq-E6XyzGqbWE9yf9Rae8F-ksqxz7QgG9T-EVCsHJ-fPgI2Jv15SYAzSWm69X3sjQVXdLQMn6CE9O1sAaESIWjttaO_p0a_cTAGGHlnhqHvQUJ-j4gyXnu-TD5_-PvsrPtGoC9F0BfWaxbe1pdDlaXBU8iAr0xDuiAWtJ4BHHH98f5RrS-sNg34xe1WKgI8raIwmvIs9BlM-NDADscvt4EVuKH2a2en3rFjz6M2Xkd8WBXx2GqmzicQRoQ1BVv_X0SaUrp6UvKaR12O0qqs6jiecQ3AD9pPeJHvs_d0-k2YCmaA-rDedV-55MFvAwZu2rS0JEvqV9QTMiEROn-rAGJOFntqCTcxgW4ZbJeNI3PcQE4_DhJv5KWZ5KwdpP4tcH3z4Ronp0SI6in1GwuMn9uZ?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5FJBoUHsYSgaEOQOJBmkIJgxapvD77bYE1tBXrVpLNij2I67fP7oqqp2wM-aMadKOnLEBCN0JSx8t9cvq_A6PXVcgNZO-pz55iLCq0uKY6-jKWbu-mC5DjeYhI3zGKImzfNKrHUHEC1AIIMfCHwVYO16lbb8-5x9hkKJXUPAVI5Di2aWFzhbA-maZB1fxmog9Y66HFbxFRzvnSjW6-cwnokjsQJ4kpwG9ZeNIGf4pW9BGU3G1WM6QPXJt555VBXf_Stam-6uKYZtXBNwshsWr1I_wBH_bzEXWx26xnM0w3J-ii6soc52oyg2W6K0e1cyxc0VJIHDDKzCfmiXOPDJJ8VP1IY7GcDIcqnhFVDeqCj2ko6I_59Eb8cwtj-DDJ-96AVFjdjly0doCUSdAcz1oskY8LkWHc4QUb0egCVeVUKOBxJW5zK3u7LxTojVdjmnd5Hzb6PN4iMmoi2UrHlxXWM,&b64e=1&sign=bc3268ee8cd27b2a5577a5c8131bdbab&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZj-VL46lT1quui8QQbfsw69M0qLNXOrF_r78sVrO0waSxzx5X32OgjKKDXQgnMoZYfceCXbcYH4xZbhmj1vFOnf19ukBVg8X7vjvZ4i5YKyw1gDEMlWLwlR3ueAt7DRZMc0HfLoWNIVlgx4J3ZLwX6XoPWyml3xF_XeQDcs0CRTXHAkf7TLS1Vv6NS8GcVovvF7Dn_QV1E4F6o4xA5Oz7hgQ32x114E3aDpcDvUfPnT1-MRetYAOKXfrOKLUh072Y7AiVIfAnQjD6hT5mAAODVhv4AzH6JbeQAE4YKIOARnbuKMXf2e0igomWJHXQ2WEYCHcv0v0XzjhFIObVsVBmRC0v-tdk-bAiY93VbSSQKLbuR5UDk3EFmnjoA2Cxbw-M1v1shwSa0ZB-GR-sY7XRVKiESZIIiDPU1rTrlRoK78kTscphP5X-KNmYVNeMtGTuP0qpadV_CHDFd-1BJGIARfBuSsW8gnuHfdfUftMT26W0qLmPLcQK5LHZtxRIke6nr3baZGQ0g-wZjiJ_z9wMInnfx-IgBFVTPNKg0GTvS4rCFov6oj94xyq_GKEsU-uzKIfsYJjRyO_ZTBCUnLd9vZ8NKiP1m7Yc1aeugeIjXDaareC6Y1rWQCc4Fhdd48GZqzpJqYwzlWE8_cUQjA7XSnOjTgyHbgerfxzro5TncZPRba7uZ6AIXrHLfYupw3g2oiC-lWXlktRhxpxc0gsGxE3sw_eSI7L7Ke2TlwZMXETNveCohn7f0XeTIeS6clEcv1pgdzfoykC0ILEdn1UfZe7M534nYeeGxOodsT5WBmAyQsr4tT-3_zBIUS7oXPC-aWLnbj-85fbBo0tCCMeSXCwzIbBt5KyrO57-5r4QgS_E6LxeqnNwZRldYCZCb2D0YkTeE48ENbP0KPErBFh0rQRpitTwM1x9YloX9ffOffl?data=QVyKqSPyGQwNvdoowNEPjZb3cFB0dEVznRaC678oUjydNhHwqP2j5FJBoUHsYSgaEOQOJBmkIJgxapvD77bYE1tBXrVpLNij2I67fP7oqqp2wM-aMadKOnLEBCN0JSx8t9cvq_A6PXVcgNZO-pz55iLCq0uKY6-jKWbu-mC5DjeYhI3zGKImzfNKrHUHEC1AIIMfCHwVYO16lbb8-5x9hkKJXUPAVI5Di2aWFzhbA-maZB1fxmog9Y66HFbxFRzvnSjW6-cwnokjsQJ4kpwG9ZeNIGf4pW9BGU3G1WM6QPXJt555VBXf_Stam-6uKYZtXBNwshsWr1I_wBH_bzEXWx26xnM0w3J-ii6soc52oyg2W6K0e1cyxc0VJIHDDKzCfmiXOPDJJ8VP1IY7GcDIcqnhFVDeqCj2ko6I_59Eb8cwtj-DDJ-96AVFjdjly0doCUSdAcz1oskY8LkWHc4QUb0egCVeVUKOBxJW5zK3u7LxTojVdjmnd5Hzb6PN4iMmoi2UrHlxXWM,&b64e=1&sign=fe2c334b72e3fa3fba55543f016c0d7c&keyno=1',
            },
            directUrl:
                'https://topradar.ru/product/derzhatel-s-besprovodnoy-zaryadkoy-baseus-rock-solid-electric-holder-wireless-charger-chernyy/?utm_source=YD_market&utm_medium=cpc&utm_term=t28976&utm_content=Baseus&utm_city=yml_msk&utm_campaign=msk_',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DrvXVZ8ghia7tlPGcXiwFon7ijtDyTsku4lg_ThbYi6nLJ4PfEQMiWsBuhfLONEB8RvcZ-_M0WIE4WLrFv0xM7I6M_3BBc53w0JI8FJejSoyT2_gGUzBv5OmkLMBtThRu4qDgu_EWvOWFgSPWHRxfndF1yrSXASLw1bwREFM_YsTUx3Ig8SYtFLehh8YOCWzt6wAmjHnq0gq7w01wv4OpNQJwm1f5OUlpio8PTYlVDlZ6ljXlIyTA2uWNkXE7Xb-t2hoypKjqpuf-l6UW3STrQfaqU8fOQtTOJTGFmwZ3LjJovMzIfrC15haFv4VfKQawzPtzEb-4dY81-1e-RxImh5FkvFOnN59C3Mer2V0TdimUZMg23CHwl4o1dM97dvz0hxdhaGhEo2h3KGEMUHtTdVB7yZZRJzceH5co9UrYzkiY3DPwCkq7fDwG_rWxkV-E6-eaFvd6q1Pyt-yPyaZlCFh3w3o8XQ4lD3uhfaqs_BUbloawXfplSRY08bbY0wgQkbapQCcgA7gMiDJFoh2cl3A9A4W32YeIkfSvFecyQzoz24yb1BiOdTUC4j3xeWuBBL5yL6BzqW5g0DC7_YXw46EZkoA7L7dKt3OHAs6XaNqwHo8d-k2zuAEjyWRM8hK29OI1JLKdaddFlLMSWumGoYxpuQjL3Gc-vC7HDCRScrox2GQgPwRHADcePbjQkA0RM1iHkxRjFOirQLVa3TYU9OM7qsBIYWiMZ7SSMH_gIOi-Lft1nAMcf3Mvof6LW23LfHwYs52fer_329J4_SJp59deMTsy6rMU-0Hzx6gfKSHOVEuvidkJwjFrjeW4SnOzLwp-XXi3U776By3bgXMvOuIaqC6DNshMDwklgIr8ZnectK-4UG2a0elGzjnzpGRyiToGQnyMU5GrOIybiH8rXoq5RoFKF7oyw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e1tabMJuQ3HKSSFijUQl2jS0tazhAi8n2-imjHUT9MXAvZ-ApiflUu1spiyOOhmwy2wDXwqllfqMsiUpH-M1Nt-NJo8-d8XiQbcdrY4RfZQ770FphVjQag,&b64e=1&sign=a207c45ace9517017f93ad67df1ad613&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DrvXVZ8ghia7tlPGcXiwFon7ijtDyTsku4lg_ThbYi6nLJ4PfEQMiWsBuhfLONEB8RvcZ-_M0WIE4WLrFv0xM7I6M_3BBc53w0JI8FJejSoyT2_gGUzBv5OmkLMBtThRu4qDgu_EWvOWFgSPWHRxfndF1yrSXASLw1bwREFM_YsTUx3Ig8SYtFLehh8YOCWzt99NabBuavCHgIcZHCgltUhEPtJS5vV04_ZMbtmmxHOtbZFc55J_t6mgkrKcnqWhQUU2QmL4J4QFeOo3gxj84zv9otE8DQ0-2tr4xzLVUEIKJtMta5Z3vliiDBq1cS-mOsuD-iWNFnXEx4G76uem77oat1j3aHi2lEIZBEI4Pf1ZPS9VX_09_JDXzbpm77BwAML6f7aR_r2uh9LbWvUlFCydYM7ges0Us634VHANNvT9oLFJ3SIq6uVqRNiygY95RwuGLXU6fIQb-y8-v--g4pHB41AD86fnjPxZeDa4fCxRBhFPSYonOisbl0MI48RIjVo1_vLD510iE2wrex9mW9oM2Mfh5H-SlamQn-vEJmAcOiuOWk_vm_hY9hDVlBDHfOwil3oIAHIrcztoDhFjOVXhczfVwB5Zx0ABAuvudr9Z01VpNK4B4PMER1n0ak6WzGnOJ9Xklq3DG4b0D89UXnjer44IS34MZ93xLV-QBjE-dXPgSv50scJoiN_0MkkyMCThGtqBOH7Vd3xCyS5HPPxFBHvP3X1Jcq9fbZXye0xpu0Be6F4O0wjyEOvw3pzw_B4lZUazMbVSwBHZsRbm0CBDxToi--XMobCmcVmeijZwtyPNbwInzhMOza__NPqN8tx-x8jjP0vjO9lQicjCLtDS72mutVKTo-Zp74CsHCaAhtUQLxwMNrOaLMAknOzPLeF2v2Jyjy-sxpJ1qT23sFWTdMuqCeuEbg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2e1tabMJuQ3HKSSFijUQl2jS0tazhAi8n3JpsxLevdwunpXI9qx54lv6a65d3KjN221kU6XaBQ_kqE3pl3uYNwfhP4Exuf70w-aWTGGopEEoqPR5a3LtFxk,&b64e=1&sign=b5926f66ff0bd3f3536d52ed4c4a1c7e&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.8,
                    count: 5426,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 5,
                            percent: 0,
                        },
                        {
                            value: 2,
                            count: 4,
                            percent: 0,
                        },
                        {
                            value: 3,
                            count: 18,
                            percent: 0,
                        },
                        {
                            value: 4,
                            count: 148,
                            percent: 4,
                        },
                        {
                            value: 5,
                            count: 3870,
                            percent: 96,
                        },
                    ],
                },
                id: 65404,
                name: 'TopRadar.ru',
                domain: 'topradar.ru',
                registered: '2011-06-01',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Барклая, дом 8, ТЦ "Горбушка", павильон 253, 121087',
                opinionUrl: 'https://market.yandex.ru/shop--topradar-ru/65404/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            onStock: true,
            photo: {
                width: 840,
                height: 746,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1921388/market_sWNvELcTL-DlJjvHrVkr7A/orig',
            },
            delivery: {
                price: {
                    value: '350',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 350 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '350',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 17,
                        },
                        brief: 'сегодня при заказе до 17:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/l1dqV37K1N-NwJXs8xOesQ?hid=91502&pp=490&clid=2210590&distr_type=4&cpc=ryaKHLIkvENK8iJcJO1vgOpmPdh0u6vF7S2B-7yRvp8yP0ZtRMtc1Wgfj1NnhAQzOYU-CtB_cIypSKkQX_z_svfQoU1wEh6oJM7JWal9a14-9jJT2DqOQqckM_M-_wNsHiAFo3_RAdPCsQ_K4mhOWP-Yuj2I3vRQ&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.003030044725,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.433538',
                    },
                ],
            },
            photos: [
                {
                    width: 840,
                    height: 746,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1921388/market_sWNvELcTL-DlJjvHrVkr7A/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 190,
                    height: 168,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1921388/market_sWNvELcTL-DlJjvHrVkr7A/190x250',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZHtJEzbp0i55uN5vvqIyHle66TvkRSZoaFeqMbZjKk5hA',
            wareMd5: 'wayIwmpNLSXwpn9DE-jcjg',
            skuType: 'market',
            name:
                'Автомобильный держатель для телефона в дефлектор с беспроводной быстрой зарядкой Baseus Rock-solid Vehicle - Черный (WXHW01-01)',
            description:
                'Baseus Rock-solid Vehicle незаменимый аксессуар для любого автовладельца, который с легкостью выполняет две функции. Во-первых, это держатель для смартфона, который закрепляется в дефлекторе с помощью обычного крючка и двухточечной опоры. Во-вторых, он поддерживает технологию беспроводной зарядки QI и будет заряжать совместимые модели телефонов, пока удерживает их перед вашими глазами. Фиксация гаджета происходит автоматически с помощью встроенного инфракрасного датчика.',
            price: {
                value: '1990',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZs4_axZeguHV8rtU0t1qwgXiq8YuQGjUiFhDW6ArGx0dT8iS6FLgdGfvt8ZuHpM9y52vMEESJQOQcZCO17OA47GQgOMGe7ebOcaNN3SxdUqohfq8yNOOpa-j3gO8frd2GsUPsFB_-VkuRK9uoVRK_udluEQocnenlNIkhkeDwr_nTDfqEmUigJlhr5c-Y-wAoh8bs5_NmVYB5X2vjj0fpcWLn2Y1ikl7JYyMt4FvkSq4k1jMWVnozFw2Iulpu2o-6j8eU4Alup0NbJTfVdoYGJ3e2oLyKSuey2T3NiPvn30OEo5uKtf_vNbLo6_VffyXqshHAnLr1nsUokrh_arHDQeG2fFoqjCPJkthb4l-fEbBQX2TPYEuP2VnIaQh0yaMqMmJAjirmbyj24gtQDEes8zt8RayUfAXFw4yDeGPYTkvDEg8TfoK7IFnYCQuMkNp5Q5-C_Iz9MWg4V6V16KvOc_RUKK6F3iBUTFaPMZRGj7fg0CologjgZNfBJYYjZpNRtfPOz-LhJAC713zXIiDTk6ZKeOwaoMV528lD8MuVGjlCEykDhoXcxg1rUPmWqrbmlTgzdAcZWj7AwXE9SY5XpLGjnf1XsAq9_dqS6OR5tWFURG8PvGcX_kj2KPBewNQCQoA_eu77ay-BpwO9UAmiga9mV5KJmwZb1HV7T9yyow5c7k8zuwJrYJRaihOCSQ7w1PGAbNGbSJaZT62ucJAT721R-pe_SiIZw9hemdWQ67ubJYUpVBzu-kEGcSggPW3Qyyj11McsNQx-71d2YgsJpc3KKIELZ0kX1uyn2tI8z0plY3bSLCaqwviRIbHX4RCqJaPtsevgNSXgIpxB5AAGsUWSfzo7EORevPGQwvf6UF_h732QpcXinTM4cSsSCtyMrj7s460QIGykI-evrN--Hz6LCytDOCSbqWcGCrsbKLS?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaBpTlGAaUyC3z3ZqFwrq0IjCvoIsJO3BqcbZZZZ2ijst7bPsNNNyglnBpdprTdFQrV7uhGyrCgmZdNAGbFZKAOsS2dEiXuRdloHrLtZBbO7M5qi4lG-1v7EtHc1YHVXSZA5HPFWeWedvRRx-k9-NZW3f2VUv9Tg1F953SPcMEfQkTDzOunshHxaFiWLCBpmNcKAnMNj_pSVXaoCLYsHkqkGiSVgic0z1zK641QsTaU6Iv8ny-JEaK4MGbIWEw1YZsYFSDjXTkv5AY3H09ici24EoLTufUAQSJ7nAXzYqBtscYGbkn3gi-6TO4th3Meol2pXbYNPNe2S2115aBSL-Wx24AFivoRmiSBEF1_5PbExwenGowk8ccx6N82I8ZjjQy0tvBsbHUCYxciKB2qSHwHtUJ1T9YJOsnzHNziGdr9SHPuKVIyQFiWYTioSwf4jU2YURPnlGIA_v80fUBF6KNdgAeyVZBp1Z0JUmsUuMXVYFyhioRxmQDneG9TuBQeTDkjFuybdvnl8-Pxg8WGUq8n1q_c4ln1ADjQYUcdkOPw6owqswLDYmF9hiNj60FEihZhXNPg6r-I24XRygSC8HE-NBraOfGc-eRf2aXWKtVYR5tsMLJZqmCuQaOi3Zhahpg&b64e=1&sign=f671bc2b2e0ef703351d6574169766d1&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZs4_axZeguHV8rtU0t1qwgXiq8YuQGjUiFhDW6ArGx0dT8iS6FLgdGfvt8ZuHpM9y52vMEESJQOQcZCO17OA47GQgOMGe7ebOcaNN3SxdUqohfq8yNOOpa-j3gO8frd2GsUPsFB_-VkuRK9uoVRK_udluEQocnenlNIkhkeDwr_nTDfqEmUigJlhr5c-Y-wAoh8bs5_NmVYB5X2vjj0fpcWLn2Y1ikl7JYyMt4FvkSq4k1jMWVnozFw2Iulpu2o-6j8eU4Alup0NbJTfVdoYGJ3e2oLyKSuey2T3NiPvn30OEo5uKtf_vNbLo6_VffyXqshHAnLr1nsUokrh_arHDQeG2fFoqjCPJkthb4l-fEbBQX2TPYEuP2VnIaQh0yaMqMmJAjirmbyj24gtQDEes8zt8RayUfAXFw4yDeGPYTkvDEg8TfoK7IFnYCQuMkNp5Q5-C_Iz9MWg4V6V16KvOc_RUKK6F3iBUTFaPMZRGj7fg0CologjgZNfBJYYjZpNRtfPOz-LhJAC713zXIiDTk6ZKeOwaoMV528lD8MuVGjlCEykDhoXcxg1rUPmWqrbmlTgzdAcZWj7AwXE9SY5XpLGjnf1XsAq9_dqS6OR5tWFURG8PvGcX_kj2KPBewNQCQoA_eu77ay-BpwO9UAmiga9mV5KJmwZb1HV7T9yyow5c7k8zuwJrYJRaihOCSQ7w1PGAbNGbSJaZT62ucJAT721R-pe_SiIZw9hemdWQ67ubJYUpVBzu-kEGcSggPW3Qyyj11McsNQx-71d2YgsJpc3KKIELZ0kX1uyn2tI8z0plY3bSLCaqwviRIbHX4RCqJaPtsevgNSXgIpxB5AAGsUWSfzo7EORevPGQwvf6UF_h732QpcXinTM4cSsSCtyMrj7s460QIGykI-evrN--Hz6LCytDOCSbqWcGCrsbKLS?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaBpTlGAaUyC3z3ZqFwrq0IjCvoIsJO3BqcbZZZZ2ijst7bPsNNNyglnBpdprTdFQrV7uhGyrCgmZdNAGbFZKAOsS2dEiXuRdloHrLtZBbO7M5qi4lG-1v7EtHc1YHVXSZA5HPFWeWedvRRx-k9-NZW3f2VUv9Tg1F953SPcMEfQkTDzOunshHxaFiWLCBpmNcKAnMNj_pSVXaoCLYsHkqkGiSVgic0z1zK641QsTaU6Iv8ny-JEaK4MGbIWEw1YZsYFSDjXTkv5AY3H09ici24EoLTufUAQSJ7nAXzYqBtscYGbkn3gi-6TO4th3Meol2pXbYNPNe2S2115aBSL-Wx24AFivoRmiSBEF1_5PbExwenGowk8ccx6N82I8ZjjQy0tvBsbHUCYxciKB2qSHwHtUJ1T9YJOsnzHNziGdr9SHPuKVIyQFiWYTioSwf4jU2YURPnlGIA_v80fUBF6KNdgAeyVZBp1Z0JUmsUuMXVYFyhioRxmQDneG9TuBQeTDkjFuybdvnl8-Pxg8WGUq8n1q_c4ln1ADjQYUcdkOPw6owqswLDYmF9hiNj60FEihZhXNPg6r-I24XRygSC8HE-NBraOfGc-eRf2aXWKtVYR5tsMLJZqmCuQaOi3Zhahpg&b64e=1&sign=f671bc2b2e0ef703351d6574169766d1&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZs4_axZeguHV8rtU0t1qwgXiq8YuQGjUiFhDW6ArGx0dT8iS6FLgdGfvt8ZuHpM9y52vMEESJQOQcZCO17OA47GQgOMGe7ebOcaNN3SxdUqohfq8yNOOpa-j3gO8frd2GsUPsFB_-VkuRK9uoVRK_udluEQocnenlNIkhkeDwr_nTDfqEmUigJlhr5c-Y-wAoh8bs5_NmVYB5X2vjj0fpcWLn2Y1ikl7JYyMt4FvkSq4k1jMWVnozFw2Iulpu2o-6j8eU4Alup0NnYUPHdkSGa8unZkGTrxr5qCrhmnMalxMUyNcnqCS6EGmjKvoeB_aUHCfzbLYaV21QfCPdqDwIN4nZkWU6yFDDePitmUkPBn-oPBg6eiKeczX8bc8tvIEDIpu8GhTO74rvU83m8Vt7ZCevvL625tFesdnips9GQGuaIpu-EwYTjmppUYzRvIRgS3VmXYH2L5zHtNpvju2cm0SO38CBhB6M5G1feRafpZr3CMIKrsTGRDPQdAfElhUYi32evKF_aV-ovsofQLJNNaMSUIpy2O1-EfRW81CkXHRsJz1JCXvHc6KB67WdB4QqOHHfrTkf4QZaEXbglt6c2rAKZ3iczuywZhn59aQIqdJzXMFBPJ_xL24hTa1wE8HRba5sqjrNk4T7i008g5zPynuzD8Ai4aN0dKKdOoUJD7niqkB_lkqxdkbJkwEUgxSxgUTQ615pNCKFID8fRXfSKxMRuPPkOy9e7HUH0K9miz-Qx46Ggaiw8U4jrHQ6uFSGOci84vrJVHTU_bzRPsfSdhaQHZdHdUVS6rFsvaEJxrMl3hXmOcCLW35naSDj4Z88vJPOvKxO3ohcv6widFRT1jA5huUYtkr9uaUkY0HpxifxfZ7bWdAFtIYa0w9nZbUbpnyGKBZj4IY_zNARJemp3ldXzYhaBkeC7DzqFMJqWpw?data=QVyKqSPyGQwNvdoowNEPjeGSQjaG3CT64sVZK6cHb32wHzmH7yCbGutF1mWWoCqaBpTlGAaUyC3z3ZqFwrq0IjCvoIsJO3BqcbZZZZ2ijst7bPsNNNyglnBpdprTdFQrV7uhGyrCgmZdNAGbFZKAOsS2dEiXuRdloHrLtZBbO7M5qi4lG-1v7EtHc1YHVXSZA5HPFWeWedvRRx-k9-NZW3f2VUv9Tg1F953SPcMEfQkTDzOunshHxaFiWLCBpmNcKAnMNj_pSVXaoCLYsHkqkGiSVgic0z1zK641QsTaU6Iv8ny-JEaK4MGbIWEw1YZsYFSDjXTkv5AY3H09ici24EoLTufUAQSJ7nAXzYqBtscYGbkn3gi-6TO4th3Meol2pXbYNPNe2S2115aBSL-Wx24AFivoRmiSBEF1_5PbExwenGowk8ccx6N82I8ZjjQy0tvBsbHUCYxciKB2qSHwHtUJ1T9YJOsnzHNziGdr9SHPuKVIyQFiWYTioSwf4jU2YURPnlGIA_v80fUBF6KNdgAeyVZBp1Z0JUmsUuMXVYFyhioRxmQDneG9TuBQeTDkjFuybdvnl8-Pxg8WGUq8n1q_c4ln1ADjQYUcdkOPw6owqswLDYmF9hiNj60FEihZhXNPg6r-I24XRygSC8HE-NBraOfGc-eRf2aXWKtVYR5tsMLJZqmCuQaOi3Zhahpg&b64e=1&sign=8a181abffc01fbc9ac0f3908e0962c82&keyno=1',
            },
            directUrl: 'https://www.audio-drive.ru/catalog/235/19155/',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DpSudQfEJM4L5qyC-BZAWssiXSMo8i_EwqTZjsOQZ2Y0nvIT0GMqbxSk1bEdMc-pMPgU9emvs5ZUlpOWbU-KtuAzr_0gfTjCWxtJZxNxTi0xYzyo4MFyNAgXfHaGrUoYA4dfkemmvwARpwG3a3BO6ES7N5rY1vdrPKrTT1eLJU6VXKdY-MdZTgxhE67giPKEDV389Oypp2Eh2oS39XhgmOjnq0PoKbUMaFRdalNWNZMfHlKEv2FXZFQm-N9UG-D68mWUgW4EMbNeMy8voL4tRRNOFyqmurZVypYy7PDwAYkc6IvrHVaxnVeVwFBjrWkkyo2HbWCtjwchsKZCrPsU3sENsxzwkcPRxofSdK-nTpFoR1JkKwWKDslzdq5lRRUxpDnCMpXT597wzIyaHrmIeI71EMcB51O55raVgdPxM51s5pR36IWnN7wRe3tfSKzRPpuicGScv7SIvQkKmzXJbJoxL4p_xLGMlOlMbhBC3_JFG7iFzf2Fue2Nzhd0dlxwF23IkXlt0tv8jdadgDRate6RgEZUCug2nwf11jTP0p17Q5hE-JzvLc3dnCc0_iqaSejvQobqiHCBOuwAGzVKZdSplO544VVCx_1QgJJgXVxZ1A6TKw994e2jmG27s52n3GwV32QBf_W3hZPFkpAdzbrT7eMmn8c02QIQCmSmvcrSZKOrH3ewqFHEN3wmv3iX-q_PC0vwH2UhTQ8B5A0R0gXlKfeNBwTUB0j9uwIASHAQJ-kGujuuc1dHR10bY2LOfNvPQ9148-H2GCGpUtOc3qbRdrzIMwuBuLO6Kee_ZyUyV4lc7gE645atOx10Fp5bUXF5T7gyLi7yZ9t0AcsKY5TjjmdnEI1U6dFys9bNbtMu2WrU1NVTBt0MD7tcopudj72H3zG4DAMS0fKXG_vWNhWNTzAUwPrvIA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XkML0AvZJ_T66Fv79z32RtWCMa9u1iwcTxrzqSZw0HGHNBh1Hz2_VOUAynGjjZZREMd02rpYvmdGzwxaWIwBrPmLn-KIuk6sUj1IOOkjph2aG5dz3yOdnU,&b64e=1&sign=e382ff0ac81cef1712a854e619f87c1c&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DpSudQfEJM4L5qyC-BZAWssiXSMo8i_EwqTZjsOQZ2Y0nvIT0GMqbxSk1bEdMc-pMPgU9emvs5ZUlpOWbU-KtuAzr_0gfTjCWxtJZxNxTi0xYzyo4MFyNAgXfHaGrUoYA4dfkemmvwARpwG3a3BO6ES7N5rY1vdrPKrTT1eLJU6VXKdY-MdZTgxhE67giPKEDV389Oypp2Eh512rH20_utKmuKVGagAMBy7Ltr2ATkwlslGQfpAiULX1YCC_8hPuFFhTIsVNtYPtpdZgHARbofZYCgoz-g1WoVtFR72gB70rAoKZ1fNSRJc7_9hm5gmWBUEVo-W90s6niz5T-kVj4ayabavB4aEekv6C9W-B7EuM6jZfz3iKRlCTAqyL-5OfFuPmCWDQK8nVi0FNQ6e9K7q13fkZ728VYjvOkd62oS_PCmLF7aQRnjWy7Cg1Q-HBn7O45PGStx3Ovi-B9GDUNtPSuozCvlLLJAQeQwqHIvRxW7k0-JOAku2pUhC4NnJhHcXJqWzs5QxEg4CMYatWaXk6McPruUjFLTiF0MAwAJOE4AT4WEwjsFBYbQfge0EjCk73gouAp9T4A4673Q1fUw-N3DUNWu5j4pS88t5gsbwIP1Bgybswg1WveN3FdeQEw1jpPmEPryFKrYAaS_U22Mcz3ba8dIAVS0NVfgDF7ug8BbPggIpa2YCgljv7BQh_m_muNU84DpCOJKeH_fk3LliEDjrYkOiq6TfFkjR1M7N-J0FmlrKz_OgIVVkUtZly24EXvVzH2BEPaShQHJp0O-vP5vTUEvYS2KTUZpY3eXa8KDykqu1dJc363c5lUKVqFnFOibWnV7yMRzqdVD27zK_f_LJEj3uj-MroAQ5pVQiB12o3H13m38B1ddfZNlRx7PzL9x2OCYecSXCs5ReVAbrBJbmNRNL9Fg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XkML0AvZJ_T66Fv79z32RtWCMa9u1iwcT2VNNOS624qFbF1M4MtU0Rx05avbEp4rJA6z8UhEhRKqs-obaZ_RLZyPVR21dKqABUK1sPjGUqUW-W0Ox0I9ao,&b64e=1&sign=eb101e9284798f6a87e83f396ad4f637&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 930,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 11,
                            percent: 2,
                        },
                        {
                            value: 2,
                            count: 5,
                            percent: 1,
                        },
                        {
                            value: 3,
                            count: 7,
                            percent: 1,
                        },
                        {
                            value: 4,
                            count: 23,
                            percent: 5,
                        },
                        {
                            value: 5,
                            count: 444,
                            percent: 91,
                        },
                    ],
                },
                id: 141152,
                name: 'AUDIO-DRIVE.RU',
                domain: 'audio-drive.ru',
                registered: '2013-02-06',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Березовая аллея, дом 5а, строение 1-3, 127273',
                opinionUrl:
                    'https://market.yandex.ru/shop--audio-drive-ru/141152/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            onStock: true,
            photo: {
                width: 710,
                height: 710,
                url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-AAJJj5_Sn3xff6XCPW7DA/orig',
            },
            delivery: {
                price: {
                    value: '290',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: true,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — 290 руб., возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '290',
                            },
                            daysFrom: 0,
                            daysTo: 1,
                            orderBefore: 11,
                        },
                        brief: 'до&nbsp;завтра при заказе до 11:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/wayIwmpNLSXwpn9DE-jcjg?hid=91502&pp=490&clid=2210590&distr_type=4&cpc=ryaKHLIkvEPmG89Ro51FlRqpQQ94_Gnhm27qK4Os8Tw0ZUH4aQ29-PeJnTlSrM3U3hdBiZgt-CScEZP0bUwtZNOUMRRPDQV8gRXiNdn9-QooeItfpfeDig8ypXMd1czMj7iimCfKBnGAgUHaLtqXXmcDvqd6flM7&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.002563969698,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.429923',
                    },
                ],
            },
            photos: [
                {
                    width: 710,
                    height: 710,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-AAJJj5_Sn3xff6XCPW7DA/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 160,
                    height: 160,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_-AAJJj5_Sn3xff6XCPW7DA/120x160',
                },
            ],
        },
        {
            __type: 'offer',
            id: 'yDpJekrrgZEiZcHPnTGUmudpDzgto-OxkWCU2jpqDfHHsewlQ03IiQ',
            wareMd5: 'mCf-btT_UaPes5gymAn-Ew',
            skuType: 'market',
            name:
                'Держатель с функцией беспроводной зарядки Baseus Smart Vehicle Bracket Wireless Charger (Adsorption) Black (WBZX-B01)',
            description:
                'Простой и удобный автомобильный держатель Baseus Smart Vehicle Bracket Wireless Charge rлегко и надежно закрепит ваш смартфон на панеле или стекле автомобиля. Имеется функция беспроводной зарядки.',
            price: {
                value: '1990',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpROVSr1p9swWz3tPMvP2RFTrFfgyNguuQLY-2Qcps2YcCNOQY6s_jlUPO3tNr-ZBc6DfsadGS3gQtg1KpHVNYsSigYFyc-NbKJMCUTXXQ_AERL2G-DeoLJ_YJR5LiiXrP_4s3_9WXAzNzkbXZrRQ0DT38-_8y_jhNfg1POWoL1nG93VQHy3Xd77VbPBtWEyaJJlB25UExFUGhpzEaYJ_IIFt9LSC6fdAP6uD-UHt3FFrzfJqqV-pDw040JqE3OmeIiuQKkdicykWgTvct3loCt3glkGI5PDfp175QGEjYEaUEJ0Nydw3HyaOxM-wpZImuNv7_e9vM7QN2ODZFpr-fbcZbj7KWX9lQjH6T3neCE4AurxB91b3VxwkabhSHX2iDGbyNRfsD3rznldP4UKaE3vCUOeaDjG72qQQAJCSVxa1BgflLMX0X5DovkVPhxSoqnSt90nuUPfKpoZGCPDDGxaS3Wp7L4x8osi6ag5Cj6RoMsKOXSkc3kS6-iRu_IRq3i8KXb-IMVA6ml-ROGyociKIi2FmE79GgctRARvTcKKMBaPVQ6sZjk8oUv85KJOooNfwJZLTk0DsaigQg5SBnWlCj6SO94WNG8iGA7J6Kx1BoG3pkubNnJT2mILdaRV3Jg1uggbrLVghrKTArU471WjYM9ghqvcNO6VvETcah9m5UDwu5s64DYG826nvWbFL-E7FFZDf35G6XMSsqm5dEfFiCdf_WQhEfVXyZ3cJqjlXln5ZKEYVgUQS4Wsfak4fGcilm2wMy7ti1BSJsc_BSsALBidHj0CT4OELkDo9SaNzIvk68xmSFsTfKKljLVQefI9_CUkdrJQaiZfWxJH4XYJYBMhOwatFV0b8P1BP1OHP78E_VU7L6MxVuJRCCf-1W3DVNHhGNkNZuZNFWg1V1BiR-n1HRtgSVkrtXjJPbTa?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmR26gd-DPor8DZTjxNYtqmAhvVrVUll_kVgQSBgI0YMYPxUK0cX1bfdDEo_-mBrwmGnIusWfTUr95w3C4BRXRh3SE56vAHOwOo&b64e=1&sign=fc9c625d2344978426ff866128bebe29&keyno=1',
            urls: {
                490: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpROVSr1p9swWz3tPMvP2RFTrFfgyNguuQLY-2Qcps2YcCNOQY6s_jlUPO3tNr-ZBc6DfsadGS3gQtg1KpHVNYsSigYFyc-NbKJMCUTXXQ_AERL2G-DeoLJ_YJR5LiiXrP_4s3_9WXAzNzkbXZrRQ0DT38-_8y_jhNfg1POWoL1nG93VQHy3Xd77VbPBtWEyaJJlB25UExFUGhpzEaYJ_IIFt9LSC6fdAP6uD-UHt3FFrzfJqqV-pDw040JqE3OmeIiuQKkdicykWgTvct3loCt3glkGI5PDfp175QGEjYEaUEJ0Nydw3HyaOxM-wpZImuNv7_e9vM7QN2ODZFpr-fbcZbj7KWX9lQjH6T3neCE4AurxB91b3VxwkabhSHX2iDGbyNRfsD3rznldP4UKaE3vCUOeaDjG72qQQAJCSVxa1BgflLMX0X5DovkVPhxSoqnSt90nuUPfKpoZGCPDDGxaS3Wp7L4x8osi6ag5Cj6RoMsKOXSkc3kS6-iRu_IRq3i8KXb-IMVA6ml-ROGyociKIi2FmE79GgctRARvTcKKMBaPVQ6sZjk8oUv85KJOooNfwJZLTk0DsaigQg5SBnWlCj6SO94WNG8iGA7J6Kx1BoG3pkubNnJT2mILdaRV3Jg1uggbrLVghrKTArU471WjYM9ghqvcNO6VvETcah9m5UDwu5s64DYG826nvWbFL-E7FFZDf35G6XMSsqm5dEfFiCdf_WQhEfVXyZ3cJqjlXln5ZKEYVgUQS4Wsfak4fGcilm2wMy7ti1BSJsc_BSsALBidHj0CT4OELkDo9SaNzIvk68xmSFsTfKKljLVQefI9_CUkdrJQaiZfWxJH4XYJYBMhOwatFV0b8P1BP1OHP78E_VU7L6MxVuJRCCf-1W3DVNHhGNkNZuZNFWg1V1BiR-n1HRtgSVkrtXjJPbTa?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmR26gd-DPor8DZTjxNYtqmAhvVrVUll_kVgQSBgI0YMYPxUK0cX1bfdDEo_-mBrwmGnIusWfTUr95w3C4BRXRh3SE56vAHOwOo&b64e=1&sign=fc9c625d2344978426ff866128bebe29&keyno=1',
                491: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZpROVSr1p9swWz3tPMvP2RFTrFfgyNguuQLY-2Qcps2YcCNOQY6s_jlUPO3tNr-ZBc6DfsadGS3gQtg1KpHVNYsSigYFyc-NbKJMCUTXXQ_AERL2G-DeoLJ_YJR5LiiXrP_4s3_9WXAzNzkbXZrRQ0DT38-_8y_jhNfg1POWoL1nG93VQHy3Xd77VbPBtWEyaJJlB25UExFUGhpzEaYJ_IIFt9LSC6fdAP6uD-UHt3FFrzfJqqV-pDw040JqE3OmeIiuQKkdicyk9y57kN6dffbIdlm8D973-JQIdk0gZee2b1nZVwc0QEPy-1Wya04HmPQx-HgvFwg-QpwD20iq3IjxI9SI7TqpFY2DEYFxYdR9ShzLYctUIJKvNQRpGtk1USJL1kZOaQ91TwZSwHXNo8YURAUxDCadtzSKIJrB18TxneHAftEYKvdCTIlaIxWoEHdIIIJE5Zp5NjcBsBnJRG8G9CWnRmmcIvpe7roEJvVS4eCRecsNhB8ZFJMyurRvMebsTCN6csaWTmW8cQT-U5VJTGbNZe1qQaTlZXCVYh6uBWdhAQvi_JHG2doZw-441yBz3gX5TEULWyvAf0Ezj0SK1z9_EBJKhRfNgwZ_zjhi9tKpSBse6wVYHI44ozoHVXUanhif3n4-sa_9rFCBdPZH1l5xbajdzj_yshaL2YDCjWusaMlXileOG3DgwVBzL_RgNw3T-f1jxwv2b8DtvbTVIDWEI3nrhi03ocMlIuwBmo5lh7J0DnBhm0eVR2WNDjsCTcqWkKmP6Zkw1J6B9r2U4yNRY7GeL6K0C3FIQ6lj5EyINKm7RLV9R_4007v9-ZAupo7cfTgwGvexawIIEqIAHrW8JT-wUpgafJwjAk-XrqOTal8PA9s3zOr7XLWnVw-yuFU0HAMC43s-TjwK7_adtRbID0yrEV5Go4otc5s2?data=QVyKqSPyGQwNvdoowNEPjSka1QB2M7l93zBwYbzfBtPu7nYxDTYmtKUUudWRiGw8O7Fgv8hQsBCfB7wyqn03asjZO6D4gtmR26gd-DPor8DZTjxNYtqmAhvVrVUll_kVgQSBgI0YMYPxUK0cX1bfdDEo_-mBrwmGnIusWfTUr95w3C4BRXRh3SE56vAHOwOo&b64e=1&sign=e0bad3b3410b96bf0c8f288adcce11a1&keyno=1',
            },
            directUrl: 'https://deshevlevsego.ru/plansheti/derzateli_dla_plansheta/2-506',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DpGLMsssFlB3qZuP0IWOlBUHDY9WDkONW7JPMoUjxftDgy_OZLl49Uy_YcnXzFmrZZYsnBN_oXjvgOnOXyxxGL0Gl0Tbqp3UC59Xy4tovi_3e3ENDfrfwJ_eR4B69Lt1tiBcDvZbBb7VRDm7P5e4ZtPxp-HpkCqR0Qn5Xh2zKjcC_bKINjlsHEaUKlhWPghOSqOO354yVAFQ8Nb7uzVMI1CdQo5dQZeJ4AJTXxo_80G62BMlI9sO_H-RTHWx2HTKSMP2nhoSMWmIn0CbCYHJR2hI2IZHpdAgLxDGqSBlCdEa1Vh8ahLLP0RXuCALW8EpqQrCEW30AHz7vOd7dpxywXmCiV7m8-GG9W7Lv0g0eSkanWXpm16YC9iplxbn0Xs92mqH4Ek5u2TCHYyV48f0fPL4U_3VMu7HRkh7fdSrJsMIKssZljTe80jPmjtNo-PeLped9nS8w2BQl_zq_dHWw5gfQn0J_QVIxeOn366UPj_MpYeIttAvWVQnP7r8pgTkATf0AZzws3qXVBTSqHD9YVmm0jH-GaaPac4c3ekySZUGvC_vZN2R8Fc-M8babKdrLwmdNJO4Vj7HWKGWywnVeZ-Uy8cYALgDrH-4rtDU0LGtutoxZm2Dc0_hr1sotVaI_V9HHICtHioBAt05XXxMCDpYyi0pPFDTypUbi3KU9ccTvaMyGmLJwDMJ-INOQWINWK_Gg8FlFxfDXVeOXoe6CAvfoJRv2gapCaFQzPpkj5oETZjck0Cw4dv9Wag61tVR1a8VWHu7mmQct6OFqqMDoXEp7rs8F_DFugJKKHUxILIGmcbIrwVhkaRaMf3F1XzkfbeEe3kMK9fTFTSZtnef9v7HlLhwJ9XAb8DGvx265zDehLmodfvpjQCTgCaSuO07-m7_I6rNqQUGoRl5SojQ72tEi8U_oMT8xg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S9F-JKUREI92qgTT9QKtPS7IsY7xJlFOo4J8GqeuZdhU34FiQRqyIaSiv2_YeJnhW-OciWdD_-8ZnhA1_LpKix7XPY7-Tf9yG-KgzKH_kHSx2_hJiZ5G1M,&b64e=1&sign=b138d518fe336b2b6fb58f41326966ee&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRuTmOq-JMY0DpGLMsssFlB3qZuP0IWOlBUHDY9WDkONW7JPMoUjxftDgy_OZLl49Uy_YcnXzFmrZZYsnBN_oXjvgOnOXyxxGL0Gl0Tbqp3UC59Xy4tovi_3e3ENDfrfwJ_eR4B69Lt1tiBcDvZbBb7VRDm7P5e4ZtPxp-HpkCqR0Qn5Xh2zKjcC_bKINjlsHEaUKlhWPghOSqOO354yVAFQKF3_45AIgB8dyCN7TA8xjq3CctmXTOzRlHtXs_I-SeLJ_Kskf4bGay6sutBRTF3ZmV1pYRdYYRKDMPYGitJHEmT_4jxAAkffEl3Itb8zOI_HhD0kuPDrvPtyKrUeWmWte_HRii_VpXDPaeP1XQJM4uGpPwn_aHw191OE-JwaESFZBnDjcNHZeIO9_V4apwb3zFsGF5kKVPKyyKDL-98TWT6fCUC_2PkIA4QsCKAqZQNriaUqSB5OpFGc4pXiQMbl3_6gfmLHYCTy2mjUJXnZBTr6qSqz9GSgrUYSfXkNUkp4x5yB1Oovcv5j3kuQomLVkzQVShRhRq8KJaZU-w5n9c1FQ9aYrjxy0a2RRd463qF26SiJwusYtFBU1lKNHFi0V5GGnnGuFeRy9ESL-9BiB60G1uR-9Rx7N_-ticf42o5S5-jXUWnVPS-CJUgO4W9yClo1P281knOdV4lP-RkFJRedWX9Ft1EqYYF-8Zfi-oLA79MaBsYLs9y8oF428fjuj93Z7LnNKtybyMdaNQBudIZpA6bW6--CYi89G493dXtvugo9jXhjyuZl4cjNhEnlBGKvKn-URDDipnBJHsbTRc4AlcXosbV9XTyFkHhyl0XuMs3_DIDPc-UBV0UsNs8QZlrouDNcQcx0_2IYY_IZx0PVhtv4yK8yRxrS5DlS04cigk0RIJEcBYr6iFBfWdCQM_VLHk3Sinl1-z_dTERBtA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2S9F-JKUREI92qgTT9QKtPS7IsY7xJlFOvSMWfhFrtJ8GcQaindNVl1i2pia3CmU2JLwqSJngjq4U0RqsFbroANXsi-g9fJ0w8fITBJItQdhAFvIc67YXqA,&b64e=1&sign=d2855e5ada5752686c1607fdaf1a7d66&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 227,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 2,
                            percent: 1,
                        },
                        {
                            value: 2,
                            count: 3,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 3,
                            percent: 2,
                        },
                        {
                            value: 4,
                            count: 7,
                            percent: 5,
                        },
                        {
                            value: 5,
                            count: 127,
                            percent: 89,
                        },
                    ],
                },
                id: 133318,
                name: 'ДешевлеВсего.ru',
                domain: 'deshevlevsego.ru',
                registered: '2012-12-13',
                type: 'DEFAULT',
                returnDeliveryAddress: 'Москва, Смоленский Бульвар, дом 24, строение 2, 119002',
                opinionUrl:
                    'https://market.yandex.ru/shop--deshevlevsego-ru/133318/reviews?pp=490&clid=2210590&distr_type=4',
                outlets: [],
            },
            onStock: true,
            photo: {
                width: 100,
                height: 100,
                url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_zPsvh7d_IvXEc0s8uaKpiQ/orig',
            },
            delivery: {
                price: {
                    value: '0',
                },
                free: true,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameAccusative: 'Россию',
                        nameGenitive: 'России',
                    },
                    nameAccusative: 'Москву',
                    nameGenitive: 'Москвы',
                },
                brief: 'в Москву — бесплатно, возможен самовывоз',
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 0,
                            daysTo: 0,
                            orderBefore: 12,
                        },
                        brief: 'сегодня при заказе до 12:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            vendor: {
                id: 10785469,
                name: 'Baseus',
                site: 'http://www.baseus.com/',
                isFake: false,
            },
            warranty: false,
            recommended: false,
            isFulfillment: false,
            link:
                'https://market.yandex.ru/offer/mCf-btT_UaPes5gymAn-Ew?hid=91502&pp=490&clid=2210590&distr_type=4&cpc=ryaKHLIkvEMryKJGj_OOBpKc60S1nDUPqoIo-zx9lXppbAuG6bkMnIdF75hoPxCiQMcenNl7SDAV-geniHdca84RJd4zszbbSRzqcyLliM7gx_D0iamcV9IUmsCR9gkpqTCyEjv86uCbzHXTlNA6Pz1RnL-7cIrV&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            trace: {
                factors: {
                    CATEG_CLICKS: 1660,
                    SHOP_CTR: 0.004513686523,
                },
                fullFormulaInfo: [
                    {
                        tag: 'Default',
                        name: 'MNA_CommonThreshold_binary_prod_356576_040',
                        value: '0.425176',
                    },
                ],
            },
            photos: [
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_zPsvh7d_IvXEc0s8uaKpiQ/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/228937/market_zPsvh7d_IvXEc0s8uaKpiQ/74x100',
                },
            ],
        },
    ],
    categories: [],
    sorts: [
        {
            text: 'по популярности',
            options: [],
        },
        {
            text: 'по цене',
            field: 'PRICE',
            options: [
                {
                    id: 'aprice',
                    how: 'ASC',
                    text: 'Сначала дешёвые',
                },
                {
                    id: 'dprice',
                    how: 'DESC',
                    text: 'Сначала дорогие',
                },
            ],
        },
        {
            text: 'по рейтингу',
            field: 'QUALITY',
            options: [
                {
                    id: 'quality',
                    how: 'DESC',
                    text: 'По рейтингу',
                },
            ],
        },
        {
            text: 'по отзывам',
            field: 'OPINIONS',
            options: [
                {
                    id: 'opinions',
                    how: 'DESC',
                    text: 'По отзывам',
                },
            ],
        },
        {
            text: 'по размеру скидки',
            field: 'DISCOUNT',
            options: [
                {
                    id: 'discount_p',
                    how: 'DESC',
                    text: 'По размеру скидки',
                },
            ],
        },
        {
            text: 'по новизне',
            field: 'DATE',
            options: [
                {
                    id: 'ddate',
                    how: 'DESC',
                    text: 'Сначала новые',
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
