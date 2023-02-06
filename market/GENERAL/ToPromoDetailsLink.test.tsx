import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';

import { rootReducer } from 'src/store/root/reducer';
import { ToPromoDetailsLink } from './ToPromoDetailsLink';
import * as promoSelectors from '../store/selectors';
import { DIRECT_DISCOUNT_PROMO } from 'src/pages/promo/tests/mocks/promo';
import { WrapperWithStoreAndRouter } from 'src/pages/promo/tests/WrapperWithStoreAndRouter';

const store = createStore(rootReducer);
const PROMO_QUERY_PART = 'test=query&part=42';

describe('<ToPromoDetailsLink />', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
    jest.clearAllMocks();
  });

  const renderComponent = () => {
    wrapper = mount(
      <WrapperWithStoreAndRouter store={store}>
        <ToPromoDetailsLink promo={DIRECT_DISCOUNT_PROMO} />
      </WrapperWithStoreAndRouter>
    );
  };

  function getLink() {
    return wrapper!.find('a');
  }

  function getLinkHref() {
    return getLink().getDOMNode().getAttribute('href');
  }

  describe('without query part', () => {
    beforeEach(() => {
      renderComponent();
    });

    it('should be render without errors', () => {
      const toPromoDetailsLink = wrapper!.find(ToPromoDetailsLink);
      expect(toPromoDetailsLink).toHaveLength(1);
    });

    it('should contain link to promo details', () => {
      const promoLink = getLink();
      expect(promoLink).toHaveLength(1);
      expect(getLinkHref()).toEqual(`/promos/${DIRECT_DISCOUNT_PROMO.id}`);
      expect(promoLink.text()).toEqual(DIRECT_DISCOUNT_PROMO.humanReadableId);
    });
  });

  describe('with query part', () => {
    beforeEach(() => {
      jest.spyOn(promoSelectors, 'selectDetailsPageQueryPart').mockImplementation(() => PROMO_QUERY_PART);
      renderComponent();
    });

    it('should contain link with query part', () => {
      expect(getLinkHref()).toEqual(`/promos/${DIRECT_DISCOUNT_PROMO.id}?${PROMO_QUERY_PART}`);
    });
  });
});
