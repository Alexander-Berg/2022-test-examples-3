import {API_TYPE} from '@shared/types/api';

import tvmJsonConfig from './tvm.json';
import {getTvmUrl, getTvmAuth, getTvmClientId} from '../_utils';

module.exports = {
  app: {
    port: process.env.PORT,
    selfhost: 'https://b2b-test.market.yandex.com',
    canReturnUserTicket: true,
  },
  blackbox: {
    api: 'blackbox-test.yandex.net',
  },
  tvm: {
    serverUrl: getTvmUrl('127.0.0.1', tvmJsonConfig),
    token: getTvmAuth(),
    clientId: getTvmClientId(tvmJsonConfig),
  },
  api: {
    [API_TYPE.GLOBAL_MARKET_PARTNER]: {
      url: 'https://global-market-partner.tst.vs.market.yandex.net',
    },
    [API_TYPE.GLOBAL_MARKET_CHECKOUT]: {
      url: 'https://global-market-checkout.tst.vs.market.yandex.net',
    },
    [API_TYPE.DATACAMP]: {
      url: 'http://datacamp.white.tst.vs.market.yandex.net',
    },
    [API_TYPE.GLOBAL_MARKET_PARTNER_ELASTIC]: {
      url: 'https://c-mdbb2sd3hrg2oo4dmhbb.rw.db.yandex.net:9200',
    },
  },
  passport: {
    host: 'https://passport-test.yandex.com',
    apiHost: 'https://api.passport-test.yandex.ru',
  },
  support: {
    host: 'https://help-frontend.taxi.tst.yandex.ru',
    apiHost: 'http://api-python.taxi.tst.yandex.net',
  },
  clientVersionDynamic: /^b2b-(.*?)\./,
};
