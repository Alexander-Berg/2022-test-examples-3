/* eslint-disable key-spacing */

describeBlock('i-entity-helpers__label', function(block) {
    let context;
    let baseInfo;

    beforeEach(function() {
        context = { tld: 'ru', expFlags: {} };
        baseInfo = { base_info: {} };
    });

    it('does not exist if both `vh_status` and `svod` are undefined', function() {
        assert.isUndefined(block(context, baseInfo));
    });

    it('`label-vh` if both `vh_status` and `svod` are defined', function() {
        baseInfo = {
            base_info: {
                vh_status: 'premiere',
                legal: { vh_licenses: { svod: true } }
            }
        };

        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: baseInfo.base_info.vh_status
        });
    });

    it('`label-vh` if only `vh_status` is defined', function() {
        baseInfo.base_info.vh_status = 'premiere';
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: baseInfo.base_info.vh_status
        });
    });

    it('`label-kpbasic` when user has subscription KP_BASIC and content by any subscriptions', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'KP_BASIC',
                svod: {
                    subscriptions: ['YA_PLUS', 'KP_BASIC']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'plus-color'
        });
    });

    it('`label-premium` when user has subscription YA_PREMIUM and content by any tags', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'KP_BASIC',
                svod: {
                    purchase_tags: ['plus', 'kp-amediateka']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'plus-color'
        });
    });

    it('`label-plus` when user has subscription YA_PLUS and content by any subscriptions', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'YA_PLUS',
                svod: {
                    subscriptions: ['YA_PLUS', 'KP_BASIC']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'plus-color'
        });
    });

    it('`label-premium` when user has subscription YA_PREMIUM and content by any subscriptions', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'YA_PREMIUM',
                svod: {
                    subscriptions: ['KP_BASIC', 'YA_PREMIUM']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'premium-long-color'
        });
    });

    it('`label-kpbasic` when user has subscription YA_PLUS and content by kp-basic+', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'YA_PLUS',
                svod: {
                    purchase_tags: ['kp-basic', 'kp-amediateka']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'plus-color'
        });
    });

    it('`label-premium` when user has subscription YA_PREMIUM and content by kp-basic+', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'YA_PREMIUM',
                svod: {
                    purchase_tags: ['kp-amediateka']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'premium-long-color'
        });
    });

    it('`label-kpbasic` when user has NO subscription and content by kp-basic+', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                svod: {
                    purchase_tags: ['kp-basic', 'kp-amediateka']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'plus-color'
        });
    });

    it('`label-price` when content available by TVOD', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                tvod: { discount_price: 99 }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'price'
        });
    });

    it('`label-price` when content available by EST with user_subscription', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                user_subscription: 'YA_PREMIUM',
                est: { discount_price: 99 }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'price'
        });
    });

    it('`label-price` when content available by EST', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                est: { discount_price: 99 }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'price'
        });
    });

    it('`label-free` when content available by AVOD', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                avod: true,
                svod: {
                    purchase_tags: ['kp-basic', 'kp-amediateka']
                }
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'free'
        });
    });

    it('`label-bought` if `est license` present and `vh_status` is not present', function() {
        baseInfo.base_info.legal = { vh_licenses: { est: { EST: true }, has_streams: true } };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'bought'
        });
    });

    it('`label-rented` if `tvod license` present and `vh_status` is not present', function() {
        baseInfo.base_info.legal = { vh_licenses: { tvod: { TVOD: true }, has_streams: true } };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'rented'
        });
    });

    it('`label-vh` if both `est license` and `vh_status` are present', function() {
        baseInfo.base_info.vh_status = 'premiere';
        baseInfo.base_info.legal = { vh_licenses: { est: { EST: true }, has_streams: true } };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: baseInfo.base_info.vh_status
        });
    });
});

describeBlock('i-entity-helpers__label-legal', function(block) {
    let context;
    let baseInfo;

    beforeEach(function() {
        context = { tld: 'ru', expFlags: {} };
        baseInfo = { base_info: {} };
    });

    it('does not exist if `tld` is not `ru`', function() {
        context = { expFlags: {} };
        assert.isUndefined(block(context, baseInfo));
    });

    it('does not exist if `svod` or other licenses are undefined', function() {
        assert.isUndefined(block(context, baseInfo));
    });

    it('`label-bought` if it has only `est license`', function() {
        baseInfo.base_info.legal = { vh_licenses: { est: { EST: true } } };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'bought'
        });
    });

    it('`label-rented` if it has only `tvod license`', function() {
        baseInfo.base_info.legal = { vh_licenses: { tvod: { TVOD: true }, has_streams: true } };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'rented'
        });
    });

    it('`label-bought` if it has `est license` and any other licenses', function() {
        baseInfo.base_info.legal = {
            vh_licenses: {
                est: { EST: true },
                tvod: { TVOD: true },
                svod: { subscriptions: ['YA_PREMIUM', 'YA_PLUS'] },
                has_streams: true
            }
        };
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: 'bought'
        });
    });

    it('`premium-short` if `svod` has `YA_PREMIUM` license only and `short` mod', function() {
        _.set(baseInfo, 'base_info.legal.vh_licenses.svod.purchase_tags', ['kp-amediateka']);
        assert.ownInclude(block(context, baseInfo, { mod: 'short' }), {
            block: 'label',
            theme: 'premium-short-color'
        });
    });
});

describeBlock('i-entity-helpers__label-vh', function(block) {
    let context;
    let baseInfo;

    beforeEach(function() {
        context = { tld: 'ru' };
        baseInfo = { base_info: {} };
    });

    it('does not exist if `tld` is not `ru`', function() {
        context = { expFlags: {} };
        assert.isUndefined(block(context, baseInfo));
    });

    it('does not exist if `vhStatus` is undefined', function() {
        assert.isUndefined(block(context, baseInfo));
    });

    it('does not exist if `vhStatus` is unknown', function() {
        baseInfo.base_info.vh_status = 'unknown';
        assert.isUndefined(block(context, baseInfo));
    });

    it('exists if `vhStatus` is known', function() {
        baseInfo.base_info.vh_status = 'premiere';
        assert.ownInclude(block(context, baseInfo), {
            block: 'label',
            theme: baseInfo.base_info.vh_status
        });
    });
});

describeBlock('i-entity-helpers__ott-normalize', function(block) {
    let context;
    let snpData;
    const ruid = 'test';
    const title = 'Славные парни';
    const contentTypeId = 20;
    const serviceName = 'ya-serp';
    const sid = '1abe8933f87847c8ae44bac8c0db8f09';
    const uuid = '4613ae01a3012b9cab8854f5be5385a8';
    const subscription = 'YA_PREMIUM';
    const options = {
        muted: true,
        autoplay: true
    };

    stubBlocks('counter-helpers__encode');

    beforeEach(function() {
        context = {
            pageUrl: {
                p: function() {}
            },
            reportData: {
                reqdata: {
                    ruid,
                    reqid: ruid
                }
            },
            expFlags: {
                'videoplayer-version': '1.1.1'
            }
        };
        snpData = {
            base_info: {
                legal:{
                    vh_licenses: {
                        avod: { is_avaliable: true },
                        has_streams: true,
                        user_subscription: subscription,
                        uuid
                    }
                }
            },
            rich_info : {
                vh_meta : {
                    content_groups : [
                        {
                            kinopoisk_id: 'kinopoisk_id',
                            licenses : [
                                {
                                    monetization_model : 'AVOD',
                                    page_id : 260229,
                                    subscription,
                                    streams : [{ drm_config : {} }]
                                }
                            ],
                            title,
                            trackings : {
                                contentTypeId,
                                serviceName,
                                sid,
                                uuid
                            },
                            uuid
                        }
                    ]
                }
            }
        };
    });

    it('returns correct OTT widget config', function() {
        const config = block(context, snpData, options);

        assert.isDefined(config);

        delete config.source.context.path;
        delete config.source.context.vars;
        delete config.source.ageRestriction;

        assert.deepOwnInclude(config, {
            id: config.id,
            muted: Boolean(options.muted),
            autoplay: options.autoplay,
            skinConfig: {
                primaryColor: '#ff6600'
            },
            serpCounterPath: '/snippet/entity_search/player/',
            source: {
                adConfig: {
                    partnerId: 260229,
                    category: 1012,
                    videoContentId: '4613ae01a3012b9cab8854f5be5385a8',
                    videoContentName: title
                },
                streams: [
                    {
                        drm_config: {},
                        drmConfig: {}
                    }
                ],
                preview: '',
                protectedFrame: true,
                withCredentials: true,
                context: {
                    contentTypeID: contentTypeId,
                    contentTypeId,
                    from: 'ottwidget_ya-serp',
                    fromBlock: 'block',
                    monetizationModel: 'AVOD',
                    position: 'none',
                    reqid: ruid,
                    serviceName,
                    sid,
                    subscriptionType: subscription,
                    uuid,
                    yandexuid: ruid
                },
                additionalParams: {
                    from_block: 'entity',
                    source: 'ottwidget_ya-serp',
                    stream_block: 'center.0.YA_PREMIUM'
                },
                logoConfig: false
            },
            version: '1.1.1',
            envName: 'production',
            urlWidget: '//yastatic.net/s3/kinopoisk-frontend/ott/player-wrapper/2.19.6/ott-player-wrapper.js',
            kpId: 'kinopoisk_id'
        });
    });
});

describeBlock('i-entity-helpers__get-thumb-link', block => {
    let context;
    let snippet;

    beforeEach(() => {
        context = { expFlags: {} };
        snippet = {};
        snippet.data = {
            base_info: { image: { mds_avatar_id: true } }
        };
        sinon.stub(blocks, 'i-entity-helpers__replace-url').returns('beautify_url');
        sinon.stub(blocks, 'i-entity-helpers__ott-image').returns('');
    });

    afterEach(() => {
        blocks['i-entity-helpers__replace-url'].restore();
        blocks['i-entity-helpers__ott-image'].restore();
    });

    it('should return vh image', () => {
        snippet.data.rich_info = {
            vh_meta: {
                content_groups: [{ import_thumbnail: true }]
            }
        };

        assert.equal(block(context, snippet.data).url, 'beautify_url');
    });
});
