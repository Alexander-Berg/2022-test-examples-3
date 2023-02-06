import React, { FC } from 'react';
import { Provider } from 'react-redux';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';

import { CategoriesControl, CategoryManagerControl } from 'src/containers';
import { CategoryRolesFilter } from 'src/pages/managers/types';
import { rootReducer } from 'src/store/root/reducer';
import { CategoryRolesFilterForm, CategoryRolesFilterFormProps } from '.';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('CategoryRolesPage', () => {
  let wrapper: ReactWrapper | null;

  function setupChangeFilter(initialFilter: CategoryRolesFilter, props: Partial<CategoryRolesFilterFormProps> = {}) {
    let result: CategoryRolesFilter = initialFilter;
    const handleChangeFilter = (filter: CategoryRolesFilter) => {
      result = filter;
    };

    wrapper = mount(
      <Wrapper>
        <CategoryRolesFilterForm {...props} filter={result} onUpdateFilter={handleChangeFilter} />
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

  describe('<CategoryRolesFilterForm />', () => {
    it('should be change categoryManagerLogin', () => {
      const getFilter = setupChangeFilter({ categoryManagerLogin: 'batman' });

      wrapper!.find(CategoryManagerControl).prop('onChange')('superman');

      expect(getFilter()).toHaveProperty('categoryManagerLogin', 'superman');
    });

    it('should be change hierarchyCategoryIds', () => {
      const getFilter = setupChangeFilter({ hierarchyCategoryIds: [100] });

      wrapper!.find(CategoriesControl).prop('onChange')([200]);

      expect(getFilter()).toHaveProperty('hierarchyCategoryIds', [200]);
    });
  });
});
