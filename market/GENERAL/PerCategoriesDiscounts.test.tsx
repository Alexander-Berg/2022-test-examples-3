import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';

import { rootReducer } from 'src/store/root/reducer';
import * as selectors from 'src/pages/promo/store/selectors';
import { WrapperWithStore } from 'src/pages/promo/tests/WrapperWithStore';
import {
  DIRECT_DISCOUNT_PROMO,
  CATEGORIES_COLLECTION_MOCK,
  FIRST_CATEGORY_MOCK,
  SECOND_CATEGORY_MOCK,
} from 'src/pages/promo/tests/mocks/promo';
import { PerCategoryDiscounts } from './PerCategoriesDiscounts';

const store = createStore(rootReducer);

describe('<PerCategoryDiscounts />', () => {
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
      <WrapperWithStore store={store}>
        <PerCategoryDiscounts promo={DIRECT_DISCOUNT_PROMO} />
      </WrapperWithStore>
    );
  };

  function getPerCategoryDiscountsComponent() {
    return wrapper!.find(PerCategoryDiscounts);
  }

  beforeEach(() => {
    jest.spyOn(selectors, 'selectCategoriesCollection').mockImplementation(() => CATEGORIES_COLLECTION_MOCK);
    renderComponent();
  });

  it('should be render without errors', () => {
    expect(getPerCategoryDiscountsComponent()).toHaveLength(1);
  });

  it('should contain list of promo categories with Discounts', () => {
    const categoryItems = getPerCategoryDiscountsComponent().find('.Categories > div');

    expect(categoryItems).toHaveLength(2);
    // eslint-disable-next-line no-irregular-whitespace
    expect(categoryItems.at(0).text()).toEqual(`${FIRST_CATEGORY_MOCK.name} — ${FIRST_CATEGORY_MOCK.discount}%`);
    // eslint-disable-next-line no-irregular-whitespace
    expect(categoryItems.at(1).text()).toEqual(`${SECOND_CATEGORY_MOCK.name} — ${SECOND_CATEGORY_MOCK.discount}%`);
  });
});
