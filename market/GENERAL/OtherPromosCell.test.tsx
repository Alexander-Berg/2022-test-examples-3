import React from 'react';
import { createStore } from 'redux';
import { mount, ReactWrapper } from 'enzyme';

import { DIRECT_DISCOUNT_PROMO } from 'src/pages/promo/tests/mocks/promo';
import { rootReducer } from 'src/store/root/reducer';
import { WrapperWithStoreAndRouter } from 'src/pages/promo/tests/WrapperWithStoreAndRouter';
import { DIRECT_DISCOUNT_OFFER_MOCK } from 'src/pages/promo/tests/mocks/offer';
import { OfferItemPromo } from 'src/java/definitions-promo';
import { OtherPromosCell } from './OtherPromosCell';
import * as selectors from '../store/selectors';

const store = createStore(rootReducer);

function createActualPromo(
  id: OfferItemPromo['id'],
  promoId: OfferItemPromo['promoId'],
  name: OfferItemPromo['name']
): OfferItemPromo {
  return {
    id,
    promoId,
    name,
    local: false,
  };
}

describe('<DatePeriod />', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
    jest.clearAllMocks();
  });

  const renderComponentWithOffer = (offer = DIRECT_DISCOUNT_OFFER_MOCK) => {
    wrapper = mount(
      <WrapperWithStoreAndRouter store={store}>
        <OtherPromosCell offer={offer} />
      </WrapperWithStoreAndRouter>
    );
  };

  function getOtherPromosCellComponent() {
    return wrapper!.find(OtherPromosCell);
  }

  function getLink() {
    return wrapper!.find('a');
  }

  function getLinkHref() {
    return getLink().getDOMNode().getAttribute('href');
  }

  describe('without actualPromos', () => {
    beforeEach(() => {
      jest.spyOn(selectors, 'selectPromo').mockImplementation(() => DIRECT_DISCOUNT_PROMO);
      renderComponentWithOffer();
    });

    it('should be render without errors', () => {
      expect(getOtherPromosCellComponent()).toHaveLength(1);
    });

    it('should be empty', () => {
      expect(getOtherPromosCellComponent().text()).toEqual('');
    });
  });

  describe('with actualPromos', () => {
    it('should be empty with only this actual promo', () => {
      jest.spyOn(selectors, 'selectPromo').mockImplementation(() => DIRECT_DISCOUNT_PROMO);
      renderComponentWithOffer({
        ...DIRECT_DISCOUNT_OFFER_MOCK,
        actualPromos: [
          createActualPromo(
            DIRECT_DISCOUNT_PROMO.id,
            DIRECT_DISCOUNT_PROMO.humanReadableId,
            DIRECT_DISCOUNT_PROMO.name
          ),
        ],
      });

      expect(getOtherPromosCellComponent().text()).toEqual('');
    });

    it('should contain link to other promo', () => {
      jest.spyOn(selectors, 'selectPromo').mockImplementation(() => DIRECT_DISCOUNT_PROMO);
      renderComponentWithOffer({
        ...DIRECT_DISCOUNT_OFFER_MOCK,
        actualPromos: [
          createActualPromo(
            DIRECT_DISCOUNT_PROMO.id,
            DIRECT_DISCOUNT_PROMO.humanReadableId,
            DIRECT_DISCOUNT_PROMO.name
          ),
          createActualPromo('testId', '#4321', 'Promo name'),
        ],
      });

      expect(getLink().text()).toEqual('#4321 Promo name');
      expect(getLinkHref()).toEqual(`/promos/testId?sskuIds=${DIRECT_DISCOUNT_OFFER_MOCK.ssku}`);
    });
  });
});
