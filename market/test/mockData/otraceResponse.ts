export const otraceResponse = {
  offer: {
    color_info: { offer_color: 'blue', requested_market_color: 'green' },
    feed_id: '2691051',
    hits_count: 0,
    hits_count_ann: 0,
    offer_id: '867',
    type: 'OFFER_BY_WARE_MD5',
    ware_md5: 'y2iEEBaarUcMoINVf9KrmQ',
    warehouse_id: 157513,
    'Отображается репортом': 'Нет',
    'Причина фильтрации (accept_doc)': 'Товар закончился на складе (code=OFFER_DISABLED)',
    'Содержится в индексе': 'Да',
  },
  urls: {
    datacamp: 'http://datacamp.white.vs.market.yandex.net/shops/2831123/offers?offer_id=867&whid=157513&format=json',
    front: {
      market: 'https://market.yandex.ru/offer/y2iEEBaarUcMoINVf9KrmQ',
      pokupka: 'https://pokupki.market.yandex.ru/product/description/101419934467?offerId=y2iEEBaarUcMoINVf9KrmQ',
    },
    idxapi: {
      check_supplier: 'http://active.idxapi.vs.market.yandex.net:29334/v1/check_supplier/get?feed=2691051',
      dimensions:
        'http://active.idxapi.vs.market.yandex.net:29334/v1/dimensions?offer=2691051-867&shop_id=2831123&warehouse_id=157513',
      dukalis:
        'http://active.idxapi.vs.market.yandex.net:29334/v1/admin/dukalis/check_offer_status/offer?shop_id=2831123&whid=157513&rgb=white&feed_id=2691051&env=production&offer_id=867',
      feed: 'http://active.idxapi.vs.market.yandex.net:29334/v1/feeds/2691051',
      published_offer: 'http://active.idxapi.vs.market.yandex.net:29334/v1/feeds/2691051/sessions/published/offers/867',
      smart_offer: 'http://active.idxapi.vs.market.yandex.net:29334/v2/smart/offer?offer_id=867&feed_id=2691051',
      stocks: 'http://active.idxapi.vs.market.yandex.net:29334/v1/stocks?shop_id=2831123&offer=2691051-867',
    },
    index_trace: 'https://tsum.yandex-team.ru/trace/1639443600000/61df263690dbd600c8d3ebea926d857a',
    mbo: {
      model: 'https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=1416883174',
      msku: 'https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=101419934467',
    },
    report: {
      delivery:
        'http://rw.vs.market.yandex.net:80/yandsearch?place=actual_delivery&pp=18&pickup-options=grouped&regset=2&combinator=0&offers-list=y2iEEBaarUcMoINVf9KrmQ%3A1&pickup-options-extended-grouping=1&adult=1&debug=1&rids=213&show-filtered-buckets-and-carriers=1',
      'delivery with fake offer':
        'http://rw.vs.market.yandex.net:80/yandsearch?place=actual_delivery&pp=18&pickup-options=grouped&regset=2&combinator=0&offers-list=y2iEEBaarUcMoINVf9KrmQ%3A1%3Bw%3A0.3%3Bd%3A15x4x18%3Bwh%3A157513%3Bct%3A200%3Bp%3A12490&pickup-options-extended-grouping=1&adult=1&debug=1&rids=213&show-filtered-buckets-and-carriers=1',
      offer_info:
        'http://rw.vs.market.yandex.net:80/yandsearch?place=offerinfo&pp=18&adult=1&regset=2&rids=213&offerid=y2iEEBaarUcMoINVf9KrmQ&show-urls=direct',
      prime:
        'http://rw.vs.market.yandex.net:80/yandsearch?place=prime&debug=1&pp=18&adult=1&regset=2&rearr-factors=market_documents_search_trace%3Dy2iEEBaarUcMoINVf9KrmQ&rids=213&offerid=y2iEEBaarUcMoINVf9KrmQ&show-urls=direct',
      print_doc: 'http://rw.vs.market.yandex.net:80/yandsearch?place=print_doc&offerid=y2iEEBaarUcMoINVf9KrmQ',
    },
    saashub: 'http://stratocaster.saas-hub.vs.market.yandex.net:80/doc_state/2691051/867',
  },
  'Доступные тарифы от калькулятора доставки': {
    'COURIER.MMAP': [
      { 'bucket_id (delivery calc)': 10423502, 'bucket_id (report)': 1948, delivery_service_id: 56628 },
      { 'bucket_id (delivery calc)': 10658373, 'bucket_id (report)': 2111, delivery_service_id: 1005546 },
      { 'bucket_id (delivery calc)': 10796374, 'bucket_id (report)': 4026, delivery_service_id: 1003939 },
      { 'bucket_id (delivery calc)': 10565989, 'bucket_id (report)': 5191, delivery_service_id: 19 },
      { 'bucket_id (delivery calc)': 10493114, 'bucket_id (report)': 7857, delivery_service_id: 1003937 },
      { 'bucket_id (delivery calc)': 10826691, 'bucket_id (report)': 8484, delivery_service_id: 1006360 },
    ],
    'PICKUP.MMAP': [
      { 'bucket_id (delivery calc)': 10855127, 'bucket_id (report)': 1258, delivery_service_id: 56628 },
      { 'bucket_id (delivery calc)': 10855139, 'bucket_id (report)': 1344, delivery_service_id: 1005546 },
      { 'bucket_id (delivery calc)': 10625473, 'bucket_id (report)': 2150, delivery_service_id: 1003937 },
      { 'bucket_id (delivery calc)': 10628792, 'bucket_id (report)': 2467, delivery_service_id: 1003939 },
    ],
  },
};
