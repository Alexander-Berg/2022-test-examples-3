import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { ShopSkuAvailabilityFilterForm, ShopSkuAvailabilityFilterFormProps } from '.';
import { SupplierType, ShopSkuAvailabilityWebFilter } from 'src/java/definitions';
import { SupplierTypeControl, SuppliersControl } from 'src/containers';
import { TextInput } from 'src/components';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('SskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  function setupChangeFilter(
    initialFilter: ShopSkuAvailabilityWebFilter,
    props: Partial<ShopSkuAvailabilityFilterFormProps> = {}
  ) {
    let result: ShopSkuAvailabilityWebFilter = initialFilter;
    const handleChangeFilter = (filter: ShopSkuAvailabilityWebFilter) => {
      result = filter;
    };

    wrapper = mount(
      <Wrapper>
        <ShopSkuAvailabilityFilterForm expandFilter {...props} filter={result} onUpdateFilter={handleChangeFilter} />
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

  describe('<MarketSkuAvailabilityFilterForm />', () => {
    it('should be change supplierType', () => {
      const getFilter = setupChangeFilter({ supplierType: SupplierType.THIRD_PARTY });

      wrapper!.find(SupplierTypeControl).prop('onChange')(SupplierType.REAL_SUPPLIER);

      expect(getFilter()).toHaveProperty('supplierType', SupplierType.REAL_SUPPLIER);
    });

    it('should be change supplierIds', () => {
      const getFilter = setupChangeFilter({ supplierIds: [500] });

      wrapper!.find(SuppliersControl).prop('onChange')([404]);

      expect(getFilter()).toHaveProperty('supplierIds', [404]);
    });

    it('should be change sskuSearchText', () => {
      const getFilter = setupChangeFilter({ sskuSearchText: 'foo' });

      wrapper!
        .find(TextInput)
        .filterWhere(node => node.key() === 'ssku_search_text')
        .prop('onChange')({ target: { value: 'bar' } });

      expect(getFilter()).toHaveProperty('sskuSearchText', 'bar');
    });

    it('should be change shopSkuSearchText', () => {
      const getFilter = setupChangeFilter({ shopSkuSearchText: 'foo' });

      wrapper!
        .find(TextInput)
        .filterWhere(node => node.key() === 'shop_sku_search_text')
        .prop('onChange')({ target: { value: 'bar' } });

      expect(getFilter()).toHaveProperty('shopSkuSearchText', 'bar');
    });
  });
});
