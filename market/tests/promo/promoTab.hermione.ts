import { expect } from 'chai';

import { commonBeforeEach, commonAfterEach } from './utils/commonTestHooks';
import { getPromosPageUrl } from './utils/routes';

const HeaderPO = require('../../../src/containers/Header/__pageObject');
const PromoListPagePO = require('../../../src/pages/promo/PromoListPage/components/Page/__pageObject');

describe('Вкладка Промо.', function () {
  const PROMO_TAB_INDEX = 6;

  beforeEach(async function () {
    await commonBeforeEach.call(this);
  });

  afterEach(async function () {
    await commonAfterEach.call(this);
  });

  it('Вкладка промо отображается', async function () {
    const header = new HeaderPO(this.browser);

    expect(await header.tabByIndex(PROMO_TAB_INDEX).isVisible()).equal(true);
    expect(await header.tabByIndex(PROMO_TAB_INDEX).getText()).equal('Промо');
  });

  describe('По клику', function () {
    beforeEach(async function () {
      const header = new HeaderPO(this.browser);
      await header.tabByIndex(PROMO_TAB_INDEX).click();
    })

    it('происходит смена URL на страницу списка промо', async function () {
      const url = await this.browser.getUrl();
      const expectedUrl = getPromosPageUrl(this.browser);

      expect(url.split('?')[0]).equal(expectedUrl);
    })

    it('показывается страница списка промо', async function () {
      const detailsPage = new PromoListPagePO(this.browser);
      await detailsPage.waitForVisible()

      expect(await detailsPage.isVisible()).equal(true);
    })
  })
})
