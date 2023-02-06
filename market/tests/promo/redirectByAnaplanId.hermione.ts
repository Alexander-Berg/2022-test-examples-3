import { expect } from 'chai';

import { getRedirectByAnaplanIdUrl, getPromoPageUrl, getPromosPageUrl } from './utils/routes';
import { commonBeforeEach, commonAfterEach } from './utils/commonTestHooks';

const PromoDetailsPagePO = require('../../../src/pages/promo/PromoDetailsPage/components/Page/__pageObject');
const PromoListPagePO = require('../../../src/pages/promo/PromoListPage/components/Page/__pageObject');
const AnaplanIdFilterPO = require('../../../src/pages/promo/PromoListPage/components/FiltersForm/AnaplanIdFilter/__pageObject');

describe('Редирект по anaplanId.', function () {
  beforeEach(async function () {
    await commonBeforeEach.call(this);
  });

  afterEach(async function () {
    await commonAfterEach.call(this);
  });

  describe('При существующем промо', function () {
    it('Происходит переход на страницу промо', async function () {
      const EXIST_PROMO = {
        anaplanId: '#6327',
        promoId: 'cheapest-as-gift$8383f5666cbe2be24376e499ae1bf624-IzYzMjc=',
      };
      const promoDetailsPage = new PromoDetailsPagePO(this.browser);
      const redirectPageURL = getRedirectByAnaplanIdUrl(this.browser, EXIST_PROMO.anaplanId);

      await this.browser.url(redirectPageURL);
      await promoDetailsPage.waitForVisible();
      const newURL = (await this.browser.getUrl()).split('?')[0];

      expect(newURL).equal(getPromoPageUrl(this.browser, EXIST_PROMO.promoId));
    });
  });

  describe('При несуществующем промо', function () {
    it('Происходит переход на страницу списка промо с фильтром по anaplanId', async function () {
      const NOT_EXIST_PROMO = {
        anaplanId: '#123123123',
      };
      const detailsPage = new PromoListPagePO(this.browser);
      const anaplanIdFilter = new AnaplanIdFilterPO(this.browser);

      const redirectPageURL = getRedirectByAnaplanIdUrl(this.browser, NOT_EXIST_PROMO.anaplanId);
      await this.browser.url(redirectPageURL);
      await detailsPage.waitForVisible();
      const newURL = await this.browser.getUrl();

      const expectedUrl = `${getPromosPageUrl(this.browser)}?anaplanIds=${encodeURIComponent(NOT_EXIST_PROMO.anaplanId)}`;
      expect(newURL).equal(expectedUrl, 'Неверный URL на старнице списка акций');

      await anaplanIdFilter.waitForVisible(10000);
      expect(await anaplanIdFilter.getInputValue())
        .equal(NOT_EXIST_PROMO.anaplanId, 'В поле фильтра неправильное значение');
    });
  });
});
