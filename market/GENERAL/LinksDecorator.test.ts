import { OTraceResponse } from './types';
import { LinksDecorator } from './LinksDecorator';
import { DatacampOffersState } from '../../api/dataSources/DatacampDataSource';

describe('LinksDecorator', () => {
  it('generate links', () => {
    const linksDecorator = new LinksDecorator(getOtraceReport(), getDatacampInfo());
    const links = linksDecorator.makeItGood();
    expect(links).toHaveLength(2);
    expect(links[0].url).toBe(
      'http://active.idxapi.vs.market.yandex.net:29334/v1/dimensions?offer=2426453-fbs3546&shop_id=2484324&warehouse_id=150199'
    );
    expect(links[1].url).toBe(
      'http://active.idxapi.vs.market.yandex.net:29334/v1/stocks?shop_id=2484324&offer=2426453-fbs3546&warehouse_id=150199'
    );
  });
});

function getOtraceReport() {
  return {
    offer: {
      feed_id: '2426453',
      offer_id: 'fbs3546',
      ware_md5: 'voAkzoDvERC0njOJU4WuIA',
      warehouse_id: 150199,
    },
    urls: {
      datacamp:
        'http://datacamp.white.vs.market.yandex.net/shops/2484324/offers?offer_id=fbs3546&whid=150199&format=json',
      front: {
        market: 'https://market.yandex.ru/offer/voAkzoDvERC0njOJU4WuIA',
        pokupka: 'https://pokupki.market.yandex.ru/product/description/101481356587?offerId=voAkzoDvERC0njOJU4WuIA',
      },
      idxapi: {
        check_supplier: 'http://active.idxapi.vs.market.yandex.net:29334/v1/check_supplier/get?feed=2426453',
        dimensions:
          'http://active.idxapi.vs.market.yandex.net:29334/v1/dimensions?offer=2426453-fbs3546&shop_id=2484324',
        dukalis:
          'http://active.idxapi.vs.market.yandex.net:29334/v1/admin/dukalis/check_offer_status/offer?shop_id=2484324&whid=150199&rgb=white&feed_id=2426453&env=production&offer_id=fbs3546',
        feed: 'http://active.idxapi.vs.market.yandex.net:29334/v1/feeds/2426453',
        published_offer:
          'http://active.idxapi.vs.market.yandex.net:29334/v1/feeds/2426453/sessions/published/offers/fbs3546',
        smart_offer: 'http://active.idxapi.vs.market.yandex.net:29334/v2/smart/offer?offer_id=fbs3546&feed_id=2426453',
        stocks: 'http://active.idxapi.vs.market.yandex.net:29334/v1/stocks?shop_id=2484324&offer=2426453-fbs3546',
      },
      index_trace: 'https://tsum.yandex-team.ru/trace/1639011600000/8e5cde81c99dca70dca8ef620326e3c9',
      mbo: { msku: 'https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=101481356587' },
      report: {
        delivery:
          'http://rw.vs.market.yandex.net:80/yandsearch?place=actual_delivery&pp=18&pickup-options=grouped&regset=2&combinator=0&offers-list=voAkzoDvERC0njOJU4WuIA%3A1&pickup-options-extended-grouping=1&adult=1&debug=1&rids=213&show-filtered-buckets-and-carriers=1',
        'delivery with fake offer':
          'http://rw.vs.market.yandex.net:80/yandsearch?place=actual_delivery&pp=18&pickup-options=grouped&regset=2&combinator=0&offers-list=voAkzoDvERC0njOJU4WuIA%3A1%3Bw%3A0.02%3Bd%3A2x6x2%3Bwh%3A150199%3Bct%3A410%2F480%2F485%3Bp%3A788&pickup-options-extended-grouping=1&adult=1&debug=1&rids=213&show-filtered-buckets-and-carriers=1',
        offer_info:
          'http://rw.vs.market.yandex.net:80/yandsearch?place=offerinfo&pp=18&adult=1&regset=2&rids=213&offerid=voAkzoDvERC0njOJU4WuIA&show-urls=direct',
        prime:
          'http://rw.vs.market.yandex.net:80/yandsearch?place=prime&debug=1&pp=18&adult=1&regset=2&rearr-factors=market_documents_search_trace%3DvoAkzoDvERC0njOJU4WuIA&rids=213&offerid=voAkzoDvERC0njOJU4WuIA&show-urls=direct',
        print_doc: 'http://rw.vs.market.yandex.net:80/yandsearch?place=print_doc&offerid=voAkzoDvERC0njOJU4WuIA',
      },
      saashub: 'http://gibson.saas-hub.vs.market.yandex.net:80/doc_state/2426453/fbs3546',
    },
  } as OTraceResponse;
}

function getDatacampInfo() {
  return {
    offer: {
      actual: [
        {
          key: 1350501,
          value: {
            warehouse: [
              {
                key: 124718,
                value: {
                  identifiers: {
                    business_id: 1350502,
                    feed_id: 1468131,
                    offer_id: 'fbs3546',
                    shop_id: 1350501,
                    warehouse_id: 124718,
                  },
                },
              },
            ],
          },
        },
        {
          key: 2484324,
          value: {
            warehouse: [
              {
                key: 150199,
                value: {
                  identifiers: {
                    business_id: 1350502,
                    extra: {
                      classifier_good_id: '213ed212f4a840e660b44ca3aa010167',
                      classifier_magic_id2: 'e1563ea42685a1f98a5e3e63a87645f3',
                      market_sku_id: 101481356587,
                      offer_yabs_id: 152174423147123,
                      recent_business_id: 1350502,
                      recent_feed_id: 2426453,
                      recent_warehouse_id: 150199,
                      shop_sku: 'fbs3546',
                      ware_md5: 'voAkzoDvERC0njOJU4WuIA',
                    },
                    feed_id: 2426453,
                    offer_id: 'fbs3546',
                    shop_id: 2484324,
                    warehouse_id: 150199,
                  },
                },
              },
            ],
          },
        },
      ],
    },
  } as DatacampOffersState;
}
