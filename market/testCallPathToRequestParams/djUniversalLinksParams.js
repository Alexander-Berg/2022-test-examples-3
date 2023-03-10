import {CPA_VALUES} from '@self/root/src/resources/report/params';

const HANDLER_PARAMS = {
    djId: 'awdDjId',
    idfa: '1212',
    gaid: '1221112',
    hid: 2113243,
    topic: 'awdwda',
    range: '1:5',
    djPlace: 'quarantine',
    page: 2,
    numdoc: 15,
    billingZone: 'default',
    recomContext: 'awdawd_recom_context',
    useCartPriceDropMark: true,
    widgetPosition: 21312314214,
    cartSnapshot: [],
    cpa: CPA_VALUES.REAL,
    hyperid: 213124124422623,
    showPreorder: true,
};

export default {
    paramsForHandler: HANDLER_PARAMS,
    expectParamsForPrepareRequest: {
        cpa: HANDLER_PARAMS.cpa,
        idfa: HANDLER_PARAMS.idfa,
        gaid: HANDLER_PARAMS.gaid,
        'recom-context': HANDLER_PARAMS.recomContext,
        hid: HANDLER_PARAMS.hid,
        topic: HANDLER_PARAMS.topic,
        range: HANDLER_PARAMS.range,
        'widget-position': HANDLER_PARAMS.widgetPosition,
        hyperid: HANDLER_PARAMS.hyperid,
        'dj-place': HANDLER_PARAMS.djPlace,
        djid: HANDLER_PARAMS.djId,
        page: HANDLER_PARAMS.page,
        numdoc: HANDLER_PARAMS.numdoc,
        'show-preorder': HANDLER_PARAMS.showPreorder ? '1' : '0',

        'use-multi-navigation-trees': '1',
        adult: '0',
        base: 'market.yandex.ru',
        bsformat: '2',
        client: 'api',
        'cpa-pof': '',
        ip: '127.0.0.1',
        'ip-rids': '213',
        pg: '18',
        'pickup-options': undefined,
        pof: '',
        puid: '9876543210',
        rgb: 'green_with_blue',
        reqid: undefined,
        'show-min-quantity': '1',
        subreqid: '1',
        'show-credits': '1',
        'show-installments': '0',
        'show-sbp': '0',
        'disabled-promo-thresholds': undefined,
        'test-buckets': undefined,
        userAgent: undefined,
        'x-yandex-icookie': undefined,
        icookie: undefined,
        yandexuid: '1234567890',
        rids: '213',
        perks: 'perkawdawd_value',
        referer: 'https%3A%2F%2Fmarket.yandex.ru%2F',
        'with-rebuilt-model': 1,
        'enable-non-msku-dsbs-offers': true,
        'x-yandex-src-icookie': undefined,
        utm_source_service: undefined,
        baobab_event_id: undefined,
        tl: undefined,
        'view-unique-id': 'undefined',
        uuid: '',
        'promo-by-user-cart-hids': undefined,
        recom_subcontext: undefined,
        widget_type: undefined,
        place: 'dj_links',
        regset: '2',
        'new-picture-format': '1',
        'show-models': '1',
        'show-urls': 'cpa',
        'show-shops': 'top',
        'show-filter-mark': 'specifiedForOffer',
        row_width: '1',
        'require-geo-coords': '0',
        'regional-delivery': '1',
        show_explicit_content: 'medicine',
        'filter-express-delivery': undefined,
        pp: undefined,
        'compact-regions': '1',
        'enable-resale-goods': '0',
        hide_plus_subscriptions: 0,
    },
};
