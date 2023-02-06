import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { AvailabilityState } from 'src/components';
import { twoTestWarehouses } from 'src/test/data';
import { CategoryAvailabilityContext, CategoryAvailabilityContextValue } from '../../contexts';
import { WarehouseCell } from './WarehouseCell';
import { CategoryRow } from './CategoryRow';
import { CategoryNameCell } from './CategoryNameCell';
import { SeasonCell } from './SeasonCell';
import { CategoryAvailabilityTable } from '.';

const store = createStore(rootReducer, {
  catDeepmind: {
    byId: {
      100: {
        id: 100,
        children: [],
        fullName: `full name #100`,
        name: `name #100`,
        parentId: 0,
        path: [100],
        published: true,
        cargoTypesOverride: {},
      },
      200: {
        id: 200,
        children: [],
        fullName: `full name #200`,
        name: `name #200`,
        parentId: 0,
        path: [200],
        published: true,
        cargoTypesOverride: {},
      },
    },
  },
});
const Wrapper: FC<{ context: CategoryAvailabilityContextValue }> = ({ children, context }) => {
  return (
    <Provider store={store}>
      <CategoryAvailabilityContext.Provider value={context}>{children}</CategoryAvailabilityContext.Provider>
    </Provider>
  );
};

function getDefaultAvailabilityContext(overridded: Partial<CategoryAvailabilityContextValue> = {}) {
  const context: CategoryAvailabilityContextValue = {
    makeGetCategoryAvailability: () => () => ({
      choosed: false,
      childrenInfo: { total: 10, availableCount: 1, unavailableCount: 1 },
      state: AvailabilityState.AvailableInherit,
      mskuInfo: {
        availableCount: 1,
        unavailableCount: 1,
      },
    }),
    makeCheckWarehouseAvailabilitiesAreChoosedSelector: () => () => false,
    handleWarehouseMatrixAudit: () => ({}),
    getBatchChangesSelector: () => ({}),
    getChangedAvailabilitiesSelector: () => [],
    getChoosedAvailabilitiesSelector: () => ({}),
    getMskuUrl: jest.fn(id => `msku-link-${id}`),
    ...overridded,
  };

  return context;
}

describe('CategoryAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<CategoryAvailabilityTable />', () => {
    it('should be contains CategoryRow', () => {
      const context = getDefaultAvailabilityContext();
      const categoryIds = [100, 200];

      wrapper = mount(
        <Wrapper context={context}>
          <CategoryAvailabilityTable
            categoryIds={categoryIds}
            filteredIds={categoryIds}
            selectedIds={categoryIds}
            selectedIdsFromPath={categoryIds}
            onChangeExpand={jest.fn()}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(CategoryRow)).toHaveLength(2);
    });

    it('should be contains WarehouseCell', () => {
      const context = getDefaultAvailabilityContext();
      const categoryIds = [100, 200];

      wrapper = mount(
        <Wrapper context={context}>
          <CategoryAvailabilityTable
            categoryIds={categoryIds}
            filteredIds={categoryIds}
            selectedIds={categoryIds}
            selectedIdsFromPath={categoryIds}
            onChangeExpand={jest.fn()}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(WarehouseCell)).toHaveLength(4);
    });

    it('should be contains CategoryNameCell', () => {
      const context = getDefaultAvailabilityContext();
      const categoryIds = [100, 200];

      wrapper = mount(
        <Wrapper context={context}>
          <CategoryAvailabilityTable
            categoryIds={categoryIds}
            filteredIds={categoryIds}
            selectedIds={categoryIds}
            selectedIdsFromPath={categoryIds}
            onChangeExpand={jest.fn()}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(CategoryNameCell)).toHaveLength(2);
    });

    it('should be hidden SeasonCell', () => {
      const context = getDefaultAvailabilityContext();
      const categoryIds = [100];

      wrapper = mount(
        <Wrapper context={context}>
          <CategoryAvailabilityTable
            categoryIds={categoryIds}
            filteredIds={categoryIds}
            selectedIds={categoryIds}
            selectedIdsFromPath={categoryIds}
            onChangeExpand={jest.fn()}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(SeasonCell)).toHaveLength(0);
    });

    it('should be contains SeasonCell', () => {
      const context = getDefaultAvailabilityContext();
      const categoryIds = [100];

      wrapper = mount(
        <Wrapper context={context}>
          <CategoryAvailabilityTable
            showSeasonColumn
            categoryIds={categoryIds}
            filteredIds={categoryIds}
            selectedIds={categoryIds}
            selectedIdsFromPath={categoryIds}
            onChangeExpand={jest.fn()}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(SeasonCell)).toHaveLength(1);
    });
  });
});
