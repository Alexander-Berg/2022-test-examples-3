describe('b-phrase_type_phrase', function() {
    var data = {
        warnplace0: false,
        warnplace1: false,
        warnplace2: false
    },
    phrase = {
        "BannerID": "2408414287",
        "weighted": {
            "guarantee": [
                {
                    "amnesty_price": "15344891",
                    "bid_price": "37925104"
                }
            ],
            "premium": [
                {
                    "bid_price": "20350251",
                    "amnesty_price": "19144993"
                }
            ]
        },
        "md5": "9d835f4ac269291bd5630d4097e87ed4",
        "premium": [
            {
                "amnesty_price": "56900000",
                "bid_price": "190500000"
            }
        ],
        "guarantee": [
            {
                "amnesty_price": "31900000",
                "bid_price": "112400000"
            }
        ],
        "pid": "1892464676",
        "PhraseID": "1350758129",
        "larr": "3000000:257261",
        "price": "0.40",
        "id": "7031219493",
        "norm_phrase": "заказчик закупка",
        "phrase": "заказчиков закупок",
        "bs_url": "http://127.0.0.1:7088/rank/24?reg-id=225%0A977&#38;no-extended-geotargeting…%D0%B2%20%D0%B7%D0%B0%D0%BA%D1%83%D0%BF%D0%BE%D0%BA,G1892464676,1350758129",
        "errors": {
            "price": false,
            "price_context": false
        },
        "warnings": {
            "price": false,
            "price_context": false
        },
        "adgroup_id": 1892464676,
        "is_deleted": false,
        "is_phrase": 1,
        "minus_words": [],
        "key_words": "заказчиков закупок",
        "unglued": false,
        "can_edit_price": true,
        "type": "phrase",
        "modelName": "m-phrase-bidable",
        "modelId": "7031219493",
        "parentName": "m-group",
        "parentId": 1892464676
    },
        block,
        sandbox;

    function createBlock() {
        var ctx = {
            block: 'b-phrase',
            mods: { type: 'phrase' },
            phrase: phrase,
            js: { modelParams: {
                "id": "7031219492",
                "name": "m-phrase-bidable",
                "parentName": "m-group",
                "parentId": 1892464676
            } },
            group: {
                adgroup_id: "1892464676"
            },
            platform: "search",
            content: [
                {
                    elem: "content",
                    tag: "span",
                    content: [
                        {
                            elem: "key-words-wrapper",
                            tag: "span",
                            content: {
                                block: "b-phrase-key-words",
                                "phrase": phrase,
                                mods: { "fixation": "off" }
                            }
                        },
                        {
                            elem: "minus-words-wrapper",
                            tag: "span",
                            content: {
                                block: "b-phrase-minus-words",
                                mods: { "unglued": "off" },
                                "phrase": phrase
                            }
                        }
                    ]
                },
                {
                    elem: "edit-icon",
                    tag: "img",
                    mix: [
                        {
                            block: "b-icon",
                            mods: { "size-16": "edit" }
                        }
                    ]
                },
                {
                    elem: "minus-words-expander",
                    tag: "img",
                    mix: [
                        {
                            block: "b-icon",
                            mods: { "size-16": "threedots" }
                        }
                    ]
                },
                {
                    elem: "warnplace",
                    tag: "span",
                    elemMods: { "type": "" },
                    "phrase": phrase
                },
                {
                    elem: "warnings",
                    tag: "span",
                    "phrase": phrase,
                    "group": {
                        "build": null,
                        "phrases": [
                            phrase,
                            {
                                "phrase": "конфликта интересов между участником закупки и заказчиком",
                                "larr": "|300000",
                                "PhraseID": "1350758135",
                                "price": "0.40",
                                "norm_phrase": "заказчик закупка интерес конфликт между участник",
                                "context_stop_flag": 0,
                                "premium": [
                                    {
                                        "amnesty_price": "66000000",
                                        "bid_price": "75400000"
                                    }
                                ],
                                "pid": "1892464676",
                                "guarantee": [
                                    {
                                        "bid_price": "70200000",
                                        "amnesty_price": "26300000"
                                    }
                                ],
                                "PriorityID": "1",
                                "weighted": {
                                    "guarantee": [
                                        {
                                            "bid_price": "4908492",
                                            "amnesty_price": "3795950"
                                        }
                                    ],
                                    "premium": [
                                        {
                                            "amnesty_price": "63222361",
                                            "bid_price": "63297812"
                                        }
                                    ]
                                },
                                "min_price": 0.3,
                                "md5": "14583703a2d41a1894ee5dff03389b01",
                                "adgroup_id": 1892464676,
                                "is_phrase": 1,
                                "minus_words": [],
                                "key_words": "конфликта интересов между участником закупки и заказчиком",
                                "can_edit_price": true,
                                "type": "phrase",
                                "modelName": "m-phrase-bidable",
                                "modelId": "7031219492",
                                "state": "active",
                                "parentName": "m-group",
                                "parentId": 1892464676
                            }
                        ],
                        "ClientID": "1037420",
                        "pid": "1892464676",
                        "BannerID": "2408414287",
                        "current_status_bids": ["2787189323"],
                        "market_rating": -1,
                        "statusActive": "No",
                        "address_id": "2104843",
                        "banners": [
                            {
                                "build": null,
                                "phrases": [
                                    phrase,
                                    {
                                        "lmin": 0.3,
                                        "phrase": "конфликта интересов между участником закупки и заказчиком",
                                        "Ctr": "0.00",
                                        "showsForecast": "79",
                                        "param2": null,
                                        "price_context": "0.40",
                                        "real_pshows": "0",
                                        "real_rclicks": "0",
                                        "bs_data_exists": 1,
                                        "statusModerate": "Yes",
                                        "larr": "|300000",
                                        "PhraseID": "1350758135",
                                        "place": "0",
                                        "price": "0.40",
                                        "Clicks": 0,
                                        "id": "7031219492",
                                        "optimizeTry": 0,
                                        "shows": 0,
                                        "norm_phrase": "заказчик закупка интерес конфликт между участник",
                                        "context_stop_flag": 0,
                                        "premium": [
                                            {
                                                "amnesty_price": "66000000",
                                                "bid_price": "75400000"
                                            },
                                            {
                                                "bid_price": "63200000",
                                                "amnesty_price": "63200000"
                                            },
                                            {
                                                "amnesty_price": "63200000",
                                                "bid_price": "63200000"
                                            }
                                        ],
                                        "ectr": 0.00168,
                                        "is_suspended": false,
                                        "phraseIdHistory": null,
                                        "declined": 0,
                                        "pid": "1892464676",
                                        "cbroker": {
                                            "broker_place_without_coef": 0,
                                            "broker_truncated": 0,
                                            "broker_price": "0.00",
                                            "broker_place": 0,
                                            "broker_coverage": 0,
                                            "eff_strategy_min_price": ""
                                        },
                                        "rank": 10000,
                                        "phr": "конфликта интересов между участником закупки и заказчиком",
                                        "Shows": 0,
                                        "guarantee": [
                                            {
                                                "bid_price": "70200000",
                                                "amnesty_price": "26300000"
                                            },
                                            {
                                                "bid_price": "3200000",
                                                "amnesty_price": "3200000"
                                            },
                                            {
                                                "amnesty_price": "3200000",
                                                "bid_price": "3200000"
                                            },
                                            {
                                                "amnesty_price": "3200000",
                                                "bid_price": "3200000"
                                            }
                                        ],
                                        "param1": null,
                                        "broker": "0.00",
                                        "real_pclicks": "0",
                                        "PriorityID": "1",
                                        "weighted": {
                                            "guarantee": [
                                                {
                                                    "bid_price": "4908492",
                                                    "amnesty_price": "3795950"
                                                },
                                                {
                                                    "bid_price": "3200000",
                                                    "amnesty_price": "3200000"
                                                },
                                                {
                                                    "bid_price": "3200000",
                                                    "amnesty_price": "3200000"
                                                },
                                                {
                                                    "bid_price": "3200000",
                                                    "amnesty_price": "3200000"
                                                }
                                            ],
                                            "premium": [
                                                {
                                                    "amnesty_price": "63222361",
                                                    "bid_price": "63297812"
                                                },
                                                {
                                                    "bid_price": "63200000",
                                                    "amnesty_price": "63200000"
                                                },
                                                {
                                                    "bid_price": "63200000",
                                                    "amnesty_price": "63200000"
                                                }
                                            ]
                                        },
                                        "pectr": 0.021312,
                                        "real_rshows": "0",
                                        "BannerID": "2408414287",
                                        "clicks": 0,
                                        "quality_score": "6.4",
                                        "no_pokazometer_stat": 1,
                                        "min_price": 0.3,
                                        "md5": "14583703a2d41a1894ee5dff03389b01",
                                        "nobsdata": 0,
                                        "errors": {
                                            "price": false,
                                            "price_context": false
                                        },
                                        "warnings": {
                                            "price": false,
                                            "price_context": false
                                        },
                                        "adgroup_id": 1892464676,
                                        "ctr": "0.00",
                                        "ctx_clicks": 0,
                                        "disabled_tragic": 0,
                                        "is_deleted": false,
                                        "is_phrase": 1,
                                        "minus_words": [],
                                        "key_words": "конфликта интересов между участником закупки и заказчиком",
                                        "showAutoOptimizationImg": 0,
                                        "phrase_minus_words_length": 0,
                                        "unglued": false,
                                        "search_editable_price": true,
                                        "context_editable_price": false,
                                        "can_edit_price": true,
                                        "type": "phrase",
                                        "modelName": "m-phrase-bidable",
                                        "t_lowctr": false,
                                        "modelId": "7031219492",
                                        "state": "active",
                                        "phrase_minus_words_limit": 20,
                                        "parentName": "m-group",
                                        "parentId": 1892464676
                                    }
                                ],
                                "ClientID": "1037420",
                                "pid": "1892464676",
                                "statusPostModerate": "Rejected",
                                "BannerID": "2408414287",
                                "current_status_bids": ["2787189323"],
                                "bid": 2787189323,
                                "campaign_minus_words": [
                                    "1c"
                                ],
                                "cid": "21294141",
                                "modelId": "2787189323",
                                "url_protocol": "http://",
                                "dmParams": {
                                    "name": "m-banner",
                                    "id": "2787189323"
                                },
                                "isNewBanner": false,
                                "isVCardEmpty": false,
                                "is_vcard_open": true,
                                "is_vcard_collapsed": false,
                                "isStatusOptimized": null,
                                "isRejectByModerator": null,
                                "canToggleAge": 1
                            }
                        ],
                        "adgroup_id": 1892464676,
                        "OrderID": "11031879",
                        "modelId": 1892464676,
                        "dmParams": {
                            "name": "m-group",
                            "id": 1892464676
                        },
                        "firstBid": 2787189323,
                        "actions": [
                            {
                                "url": "https://8203.beta1.direct.yandex.ru/registered/main.pl?cmd=showCampMultiEdit&cid=21294141&banner_status=decline&bid=2787189323&adgroup_ids=1892464676&retpath=https%3A%2F%2F8203.beta1.direct.yandex.ru%2Fregistered%2Fmain.cgNzSGis-b8qDoKt.pl%2F%3Fcid%3D21294141%26ulogin%3Duseryd%26tab%3Ddecline%26cmd%3DshowCamp%26csrf_token%3DWWNKVugSkQkBaq7p%231892464676&ulogin=useryd&csrf_token=cgNzSGis-b8qDoKt",
                                "caption": "Редактировать группу…"
                            },
                            {
                                "url": "https://8203.beta1.direct.yandex.ru/registered/main.pl?cmd=showCampMultiEdit&cid=21294141&banner_status=decline&bid=2787189323&adgroup_ids=1892464676&is_groups_copy_action=1&ulogin=useryd&csrf_token=cgNzSGis-b8qDoKt",
                                "caption": "Копировать группу…",
                                "type": "copy-advert",
                                "js": {
                                    "limitReached": false,
                                    "limitReachedMessage": "Достигнуто максимальное количество групп в кампании - 1000"
                                }
                            },
                            {
                                "url": "https://8203.beta1.direct.yandex.ru/registered/main.pl?cmd=stopAdGroup&adgroup_ids=1892464676&cid=21294141&ulogin=useryd&csrf_token=cgNzSGis-b8qDoKt",
                                "caption": "Остановить"
                            },
                            {
                                "custom": {
                                    block: "b-minus-words-control",
                                    "switcherType": "link",
                                    mix: {
                                        block: "b-banner-actions-link",
                                        elem: "switcher"
                                    },
                                    "modelParams": {
                                        "name": "m-group",
                                        "id": 1892464676
                                    },
                                    "requestParams": {
                                        "for": "groups",
                                        "cid": "21294141",
                                        "adgroup_id": 1892464676,
                                        "ulogin": "useryd",
                                        "onSuccessSave": 1
                                    },
                                    "for": "groups",
                                    "switcherText": "Минус-слова на группу",
                                    "minusWords": []
                                }
                            },
                            {
                                "url": "https://8203.beta1.direct.yandex.ru/registered/main.pl?cmd=admSendBK&bid=2787189323&cid=21294141&adgroup_ids=1892464676&ulogin=useryd&csrf_token=cgNzSGis-b8qDoKt",
                                "caption": "Отправить в БК",
                                "type": "confirm",
                                "js": { "message": "Вы действительно хотите отправить группу в БК?" }
                            },
                            {
                                "url": "https://8203.beta1.direct.yandex.ru/registered/main.pl?cmd=admSendMD&bid=2787189323&cid=21294141&adgroup_ids=1892464676&ulogin=useryd&csrf_token=cgNzSGis-b8qDoKt",
                                "caption": "Перемодерация",
                                "type": "confirm",
                                "js": { "message": "Вы действительно хотите отправить группу заново на модерацию?" }
                            }
                        ]
                    }
                }
            ]
        };

        block = u.createBlock(ctx);
    }

    before(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        var campaign = BEM.MODEL.create({ id: "21294141", name: "m-campaign" }),
            group = BEM.MODEL.create({ "name": "m-group", "id": 1892464676 });
        group.set('cid', "21294141");
        createBlock();
    });

    after(function() {
        // должны отработать afterCurrentEvent
        sandbox.clock.tick(1);
        block.destruct();
        sandbox.restore();
    });

    it('При замене минус-слов обновляется текст', function() {
        block.phraseModel.set('minus_words', ['лес']);

        sandbox.clock.tick(100);

        expect(block.findBlockInside('minus-words-wrapper', 'b-phrase-minus-words').elem('minus-words').html())
            .to.equal('-лес');
    });

    it('При замене ключевых слов обновляется текст', function() {
        block.phraseModel.set('key_words', 'заказчиков закупок колбасы');

        sandbox.clock.tick(100);

        expect(block.findBlockInside('key-words-wrapper', 'b-phrase-key-words').domElem.html())
            .to.be.equal('заказчиков закупок колбасы ');
    });
});
