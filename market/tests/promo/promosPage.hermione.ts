import { expect } from 'chai';

import { getPromosPageUrl } from './utils/routes';
import { commonBeforeEach, commonAfterEach } from './utils/commonTestHooks';

const PromoListPagePO = require('../../../src/pages/promo/PromoListPage/components/Page/__pageObject');
const DatePeriodFilterPO = require('../../../src/pages/promo/PromoListPage/components/FiltersForm/DatePeriodFilter/__pageObject');

describe('Страница списка промо.', function () {
  beforeEach(async function () {
    await commonBeforeEach.call(this);
    await this.browser.url(getPromosPageUrl(this.browser));
  });

  afterEach(async function () {
    await commonAfterEach.call(this);
  });

  it('При переходе по ссылке отображается страница списка промо', async function () {
    const DetailsPage = new PromoListPagePO(this.browser);

    await DetailsPage.waitForVisible()

    expect(await DetailsPage.isVisible()).equal(true);
  });

  describe('Фильтр по дате.', async function () {
    it('Отображается', async function () {
      const datePeriodFilter = new DatePeriodFilterPO(this.browser);

      await datePeriodFilter.waitForVisible();

      expect(await datePeriodFilter.isVisible()).equal(true);
      expect(await datePeriodFilter.getDateFromInput().isVisible()).equal(true);
      expect(await datePeriodFilter.getDateToInput().isVisible()).equal(true);
    });

    it('Дата начала вводится и проставляется в URL', async function () {
      const START_DATE = '2021-01-01';
      const datePeriodFilter = new DatePeriodFilterPO(this.browser);
      await datePeriodFilter.waitForVisible();
      await this.browser.yaSetValue(datePeriodFilter.getDateFromInput(), START_DATE);
      const newURL = await this.browser.getUrl();

      expect(newURL.includes(`startDate=${START_DATE}`)).equal(true);
    });

    it('Дата конца вводится и проставляется в URL', async function () {
      const END_DATE = '2021-01-25';
      const datePeriodFilter = new DatePeriodFilterPO(this.browser);
      await datePeriodFilter.waitForVisible();
      await this.browser.yaSetValue(datePeriodFilter.getDateToInput(), END_DATE);
      const newURL = await this.browser.getUrl();

      expect(newURL.includes(`endDate=${END_DATE}`)).equal(true);
    });
  });
});
