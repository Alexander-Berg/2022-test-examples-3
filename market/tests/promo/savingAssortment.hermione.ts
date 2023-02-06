import { expect } from 'chai';

import { getPromoPageUrl } from './utils/routes'
import { commonBeforeEach, commonAfterEach } from './utils/commonTestHooks';
import { makeQueryPartWithSsku } from './utils/routes';
import  { SAVING_PROMOS } from './data/promos';

const ConfirmationPopupPO = require('../../../src/components/ConfirmationPopup/__pageObject');
const ParticipationCellPO = require('../../../src/pages/promo/PromoDetailsPage/components/ParticipationCell/__pageObject');
const OfferErrorsPO = require('../../../src/pages/promo/PromoDetailsPage/components/OfferErrors/__pageObject');
const ChangesActionPanelPO = require('../../../src/pages/promo/PromoDetailsPage/components/ChangesActionPanel/__pageObject');
const DirectDiscountFixedBasePriceInputPO = require(
  '../../../src/pages/promo/PromoDetailsPage/components/mechanics/DirectDiscount/DirectDiscountFixedBasePriceInput/__pageObject'
);
const DirectDiscountFixedPriceInputPO = require(
  '../../../src/pages/promo/PromoDetailsPage/components/mechanics/DirectDiscount/DirectDiscountFixedPriceInput/__pageObject'
);

process.env.HERMIONE_RUN_PROMO_SAVING !== 'true' && hermione.skip.in(/.+/);
describe('Cохранение ассортимента.', function () {
  beforeEach(async function () {
    await commonBeforeEach.call(this);
  });

  afterEach(async function () {
    await commonAfterEach.call(this);
  });

  describe('Валидация параметров участия.', function () {
    Object.keys(SAVING_PROMOS).forEach(promoName => {
      const promo = SAVING_PROMOS[promoName];
      promo.cases.forEach(({name, offer}) => {
        it(name, async function () {
          const changesActionPanel = new ChangesActionPanelPO(this.browser);

          await this.browser.url(getPromoPageUrl(
            this.browser,
            promo.promoId,
            makeQueryPartWithSsku([offer.ssku])
          ));

          await changesActionPanel.waitForVisible();
          await changesActionPanel.waitForSpinVisible(10 * 1000, true);

          await resetToPublishIfNeed(this.browser, changesActionPanel);

          await checkOffer.call(this, offer.base, true);

          await inputOfferData.call(this, offer.changed);
          await checkOffer.call(this, offer.changed, false);

          await saveAssortment(this.browser, changesActionPanel);

          await checkOffer.call(this, offer.changed, true);
        });
      });
    });
  });
});

async function resetToPublishIfNeed(browser, changesActionPanel) {
  const hasNotPublishChanges = await changesActionPanel.getResetToPublishButtonAttribute('disabled')
    .then(result => result !== 'true');

  if (hasNotPublishChanges) {
    const confirmationPopup = new ConfirmationPopupPO(browser);
    await changesActionPanel.getResetToPublishButton().click();
    await confirmationPopup.waitForVisible();
    await confirmationPopup.getOkButton().click();
    await changesActionPanel.waitForSpinVisible();
    await changesActionPanel.waitForSpinVisible(10 * 1000, true);
  }
}

async function saveAssortment(browser, changesActionPanel) {
  await changesActionPanel.getSaveButton().click();
  await changesActionPanel.waitForSpinVisible();
  await changesActionPanel.waitForSpinVisible(10 * 1000, true);
}

async function inputOfferData(offer) {
  const participationCell = new ParticipationCellPO(this.browser);
  await participationCell.waitUntilVisible(10 * 1000);
  const fixedBasePriceInput = new DirectDiscountFixedBasePriceInputPO(this.browser);
  const fixedPriceInput = new DirectDiscountFixedPriceInputPO(this.browser);

  if (await participationCell.isChecked() !== offer.participation) {
    await participationCell.getCheckbox().click();
  }

  await this.browser.yaSetValue(fixedBasePriceInput.getInput(), offer.fixedBasePrice);
  await fixedBasePriceInput.blur();

  await  this.browser.yaSetValue(fixedPriceInput.getInput(), offer.fixedPrice);
  await fixedPriceInput.blur();
}

async function checkOffer(offer, isOfferFromBackend) {
  const participationCell = new ParticipationCellPO(this.browser);
  const offerErrors = new OfferErrorsPO(this.browser);
  const fixedBasePriceInput = new DirectDiscountFixedBasePriceInputPO(this.browser);
  const directDiscountFixedPriceInput = new DirectDiscountFixedPriceInputPO(this.browser);
  const changesActionPanel = new ChangesActionPanelPO(this.browser);

  const offerType = isOfferFromBackend ? 'Оффер с бекенда': 'Оффер с фронтенда';

  const isParticipate = offer.participation;
  expect(await participationCell.isChecked())
    .equal(isParticipate, `Должно быть ${isParticipate ? 'включено' : 'выключено'} участие (${offerType})`);

  expect(await fixedBasePriceInput.getInputValue()).equal(offer.fixedBasePrice);

  expect(await directDiscountFixedPriceInput.getInputValue()).equal(offer.fixedPrice);

  const errorsCount = offer.errors.length;
  expect(await offerErrors.getErrorsCount())
    .equal(errorsCount,  `Количество ошибок должно быть (${offerType})`);
  for (const error of offer.errors) {
    expect(await offerErrors.hasErrorText(error))
      .equal(true, `В ячейке ошибок должен содержаться текст: "${error}" (${offerType})`);
  }


  if (isOfferFromBackend) {
    const { publishButtonEnabledByBackend } = offer;
    const errorMessage =
      `Кнопка публикации должна быть ${publishButtonEnabledByBackend ? 'включена' : 'отключена'}`;
    const expectedDisabledAttribute = publishButtonEnabledByBackend ? null : 'true';

    expect(await changesActionPanel.getPublishButtonAttribute('disabled'))
      .equal(expectedDisabledAttribute, errorMessage);
  }

}
