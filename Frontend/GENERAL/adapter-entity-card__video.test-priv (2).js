/* eslint-disable key-spacing */
describeBlock('adapter-entity-card__video-movie', function(block) {
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
        autoplay: true,
        thumb: {}
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
            },
            device: { BrowserName: '' }
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

    it('setup player version', function() {
        const config = block(context, snpData, options);

        assert.isDefined(config);
        assert.equal(config.version, context.expFlags['videoplayer-version']);
    });
});

describeBlock('adapter-entity-card__sideblock-video-trailer', function(block) {
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
        autoplay: true,
        thumb: {}
    };

    stubBlocks('counter-helpers__encode');
    stubBlocks('adapter-entity-card__trailer-data');

    beforeEach(function() {
        context = {
            pageUrl: {
                p: function() {},
                add: function() { return { url: function() {} } }
            },
            reportData: {
                reqdata: {
                    ruid,
                    reqid: ruid
                }
            },
            expFlags: {
                'videoplayer-version': '1.1.1'
            },
            device: { BrowserName: '' }
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

    it('setup player version', function() {
        blocks['adapter-entity-card__trailer-data'].returns({
            duration:1,
            url:'',
            uuid:uuid,
            title:'title'
        });

        const config = block(context, snpData, options);

        assert.isDefined(config);
        assert.equal(config.version, context.expFlags['videoplayer-version']);
    });
});
