import {API_TYPE} from '@shared/types/api';

import tvmJsonConfig from './tvm.json';
import {getTvmUrl, getTvmAuth, getTvmClientId, getSelfTvmId} from '../_utils';

module.exports = {
  app: {
    port: process.env.PORT,
    selfhost: 'https://b2c-test.market.yandex.com',
    swagger: true,
    canReturnUserTicket: true,
  },
  http: {
    corsOrigin: ['https://grocery-authproxy.lavka.tst.yandex.net'],
  },
  blackbox: {
    api: 'blackbox-test.yandex.net',
  },
  tvm: {
    serverUrl: getTvmUrl('127.0.0.1', tvmJsonConfig),
    token: getTvmAuth(),
    clientId: getTvmClientId(tvmJsonConfig),
    selfId: getSelfTvmId(tvmJsonConfig),
    allowedTvmClients: [2021482],
  },
  api: {
    [API_TYPE.GLOBAL_MARKET_CHECKOUT]: {
      url: 'https://global-market-checkout.tst.vs.market.yandex.net',
    },
    [API_TYPE.GLOBAL_MARKET_PARTNER_ELASTIC]: {
      url: 'https://c-mdbb2sd3hrg2oo4dmhbb.rw.db.yandex.net:9200',
    },
    [API_TYPE.DELI]: {
      url: 'https://grocery-authproxy.lavka.tst.yandex.net/4.0/eda-superapp/lavka',
    },
    [API_TYPE.TRUST_PAYMENTS]: {
      url: 'https://trust-payments-test.paysys.yandex.net:8028',
    },
  },
  clientVersionDynamic: /^b2c-(.*?)\./,
};
