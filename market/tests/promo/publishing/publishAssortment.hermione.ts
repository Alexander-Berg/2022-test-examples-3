import { expect } from 'chai';

import { getPromoPageUrl, makeQueryPartWithSsku } from '../utils/routes';
import { resolveAssetPath } from './assets/resolveAssetsPath';
import { commonBeforeEach, commonAfterEach } from '../utils/commonTestHooks';
import {PROMO_TYPES, PUBLISHING_PROMOS } from '../data/promos';
import { httpGetJSONRequest, createGetActivePromosUrl } from './httpRequest';

const MessagesViewerPO = require('../../../libraryPageObjects/MessagesViewer');
const ImportFromExcelControlPO = require('../../../../src/components/ImportFromExcelControl/__pageObject');
const ChangesActionPanelPO = require('../../../../src/pages/promo/PromoDetailsPage/components/ChangesActionPanel/__pageObject');

enum PublishTypes {
  BASE = 'base',
  CHANGED = 'changed',
}
describe('Импорт ассортимента из Excel.', function () {
  beforeEach(async function () {
    await commonBeforeEach.call(this);
  });

  afterEach(async function () {
    await commonAfterEach.call(this);
  });

  Object.keys(PUBLISHING_PROMOS).forEach(promoTestName => {
    const promo = PUBLISHING_PROMOS[promoTestName];
    const makeCase = makePublishCase.bind(this);

    makeCase(PublishTypes.BASE, promo);
    makeCase(PublishTypes.CHANGED, promo);
  });

});

function makePublishCase(publishType, promo) {
  const filename = promo.files[publishType];
  const caseTag = publishType === 'base' ? '[Публикация базового состояния]' : '[Публикация измененного состояния]';

  process.env.HERMIONE_RUN_PROMO_PUBLISH !== 'true' && hermione.skip.in(/.+/);
  describe(`${promo.anaplanId}.`, function () {
    this.timeout(0);
    it(`${caseTag} Ассортимент импортируется и публикуется`, async function () {
      const importFromExcelControl = new ImportFromExcelControlPO(this.browser);
      const changesActionPanel = new ChangesActionPanelPO(this.browser);
      const messagesViewer = new MessagesViewerPO(this.browser);

      await this.browser.url(getPromoPageUrl(
        this.browser, promo.promoId, makeQueryPartWithSsku(promo.offers.map(offer => offer.ssku))
      ));

      await importFromExcelControl.waitForVisible(1000 * 15);
      await importFromExcelControl.chooseFile(resolveAssetPath(filename));
      await importFromExcelControl.waitForUploaded();

      await changesActionPanel.waitForPublishButtonDisabledVisible(1000 * 15, true);
      await changesActionPanel.getPublishButton().click();
      await changesActionPanel.waitForSpinVisible();
      await changesActionPanel.waitForSpinVisible(1000 * 180, true);

      expect(await messagesViewer.isEmpty())
        .equal(true,  'Есть сообщения об ошибке в интерфейсе');

      /**
       * Проверка данных в Datacamp
       */
      for (const offer of promo.offers) {
        const promos = (await getActivePromosForOffer(offer))
          .filter(activePromo => activePromo.id === promo.anaplanId);
        const expectedData = offer.expectedData[publishType];
        if (expectedData.active) {
          expect(promos.length > 0)
            .equal(true, `В Datacamp нет активного промо: ${createGetActivePromosUrl(offer)}`);
          checkActivePromoData(promo.type, promos[0], expectedData);
        } else {
          expect(promos.length).equal(0, 'В Datacamp есть активное промо');
        }
      }
    });
  });
}

interface AnaplanActivePromo {
  id: string;
}

interface AnaplanActivePromos {
  promos?: AnaplanActivePromo[],
}

interface OfferFromDatacamp {
  offer: {
    promos: {
      anaplan_promos: {
        active_promos?: AnaplanActivePromos
      }
    }
  }
}

async function getActivePromosForOffer(offer): Promise<AnaplanActivePromo[]> {
  const offerInDatacamp = await httpGetJSONRequest<OfferFromDatacamp>(createGetActivePromosUrl(offer));
  const activePromosObject: AnaplanActivePromos = offerInDatacamp.offer.promos.anaplan_promos.active_promos || {};
  return  activePromosObject.promos || [];
}

function checkActivePromoData(promoType, promo, expectedData) {
  expect(promo.active).equal(true, `Промо в Datacamp должно быть активно`);
  switch (promoType) {
    case PROMO_TYPES.DIRECT_DISCOUNT:
      const PRICES_RATIO = 10000000;
      const BASE_PRICE_ERROR = `Базовая цена в Datacamp должна быть ${expectedData.basePrice}`
      expect(promo.direct_discount.base_price.price / PRICES_RATIO)
        .equal(expectedData.basePrice, BASE_PRICE_ERROR);
      expect(promo.discount_oldprice.price / PRICES_RATIO)
        .equal(expectedData.basePrice, BASE_PRICE_ERROR);

      const PRICE_ERROR = `Цена продажи в Datacamp должна быть ${expectedData.price}`
      expect(promo.direct_discount.price.price / PRICES_RATIO)
        .equal(expectedData.price, PRICE_ERROR);
      expect(promo.discount_price.price / PRICES_RATIO)
        .equal(expectedData.price, PRICE_ERROR);
      break;
    case PROMO_TYPES.CHEAPEST_AS_GIFT:
      break;
    default:
  }
}
