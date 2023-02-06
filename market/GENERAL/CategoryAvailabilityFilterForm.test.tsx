import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { CategoryAvailabilityFilterForm, CategoryAvailabilityFilterFormProps } from '.';
import { ExtendedMskuFilter } from 'src/java/definitions';
import { CategoryManagerControl, DeepmindCategoriesControl } from 'src/containers';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('CategoryAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  function setupChangeFilter(
    initialFilter: ExtendedMskuFilter,
    props: Partial<CategoryAvailabilityFilterFormProps> = {}
  ) {
    let result: ExtendedMskuFilter = initialFilter;
    const handleChangeFilter = (filter: ExtendedMskuFilter) => {
      result = filter;
    };

    wrapper = mount(
      <Wrapper>
        <CategoryAvailabilityFilterForm {...props} filter={result} onUpdateFilter={handleChangeFilter} />
      </Wrapper>
    );

    return () => result;
  }

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<CategoryAvailabilityFilterForm />', () => {
    it('should be change categoryManagerLogin', () => {
      const getFilter = setupChangeFilter({ categoryManagerLogin: 'batman' });

      wrapper!.find(CategoryManagerControl).prop('onChange')('superman');

      expect(getFilter()).toHaveProperty('categoryManagerLogin', 'superman');
    });

    it('should be change hierarchyCategoryIds', () => {
      const getFilter = setupChangeFilter({ hierarchyCategoryIds: [100] });

      wrapper!.find(DeepmindCategoriesControl).prop('onChange')([200]);

      expect(getFilter()).toHaveProperty('hierarchyCategoryIds', [200]);
    });
  });
});
