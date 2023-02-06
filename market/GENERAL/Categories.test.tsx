import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import React from 'react';

import { rootReducer } from 'src/store/root/reducer';
import * as selectors from 'src/pages/promo/store/selectors';
import {
  DIRECT_DISCOUNT_PROMO,
  CATEGORIES_COLLECTION_MOCK,
  FIRST_CATEGORY_MOCK,
  SECOND_CATEGORY_MOCK,
} from 'src/pages/promo/tests/mocks/promo';
import { Categories } from './Categories';
import { WrapperWithStore } from '../../../tests/WrapperWithStore';

const store = createStore(rootReducer);

describe('<Categories />', () => {
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
        <Categories promo={DIRECT_DISCOUNT_PROMO} />
      </WrapperWithStore>
    );
  };

  function getCategoriesComponent() {
    return wrapper!.find(Categories);
  }

  beforeEach(() => {
    jest.spyOn(selectors, 'selectCategoriesCollection').mockImplementation(() => CATEGORIES_COLLECTION_MOCK);
    renderComponent();
  });

  it('should be render without errors', () => {
    expect(getCategoriesComponent()).toHaveLength(1);
  });

  it('should contain list of promo categories', () => {
    const categoryItems = getCategoriesComponent().find('.Categories > div');

    expect(categoryItems).toHaveLength(2);
    expect(categoryItems.at(0).text()).toEqual(`${FIRST_CATEGORY_MOCK.name},`);
    expect(categoryItems.at(1).text()).toEqual(SECOND_CATEGORY_MOCK.name);
  });
});
