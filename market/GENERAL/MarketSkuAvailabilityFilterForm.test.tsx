import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { MarketSkuAvailabilityFilterForm, MarketSkuAvailabilityFilterFormProps } from '.';
import { ExtendedMskuFilter, MappingExistence, SupplierType, ExtendedMskuStatusValue } from 'src/java/definitions';
import {
  CategoryManagerControl,
  DeepmindCategoriesControl,
  MappingExistenceControl,
  SupplierTypeControl,
  SuppliersControl,
  MarketSkuStatusControl,
  SeasonControl,
} from 'src/containers';
import { DatePicker, TextInput } from 'src/components';
import { MskuPricebandLabelControl } from 'src/components/MarketSku/MskuPricebandLabelControl';
import { MskuPricebandIdControl } from 'src/components/MarketSku/MskuPricebandIdControl';
import { MskuInTargetAssortmentControl } from 'src/components/MarketSku/MskuInTargetAssortmentControl';
import { MskuSeasonalSelect } from 'src/deepmind/components';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('MskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  function setupChangeFilter(
    initialFilter: ExtendedMskuFilter,
    props: Partial<MarketSkuAvailabilityFilterFormProps> = {}
  ) {
    let result: ExtendedMskuFilter = initialFilter;
    const handleChangeFilter = (filter: ExtendedMskuFilter) => {
      result = filter;
    };

    wrapper = mount(
      <Wrapper>
        <MarketSkuAvailabilityFilterForm expandFilter {...props} filter={result} onUpdateFilter={handleChangeFilter} />
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

    it('should be change mappingExistence', () => {
      const getFilter = setupChangeFilter({ mappingExistence: MappingExistence.HAS_MAPPING });

      wrapper!.find(MappingExistenceControl).prop('onChange')(MappingExistence.HAS_MAPPING_ON_STOCK);

      expect(getFilter()).toHaveProperty('mappingExistence', MappingExistence.HAS_MAPPING_ON_STOCK);
    });

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

    it('should be change mskuStatusStartDate', () => {
      const getFilter = setupChangeFilter({ mskuStatusStartDate: '2020-01-01' });

      wrapper!
        .find(DatePicker)
        .filterWhere(node => node.key() === 'msku_status_start_date')
        .prop('onChange')(new Date(2020, 1, 3));

      expect(getFilter()).toHaveProperty('mskuStatusStartDate', '2020-02-03');
    });

    it('should be change mskuStatusFinishDate', () => {
      const getFilter = setupChangeFilter({ mskuStatusFinishDate: '2020-01-01' });

      wrapper!
        .find(DatePicker)
        .filterWhere(node => node.key() === 'msku_status_finish_date')
        .prop('onChange')(new Date(2020, 1, 3));

      expect(getFilter()).toHaveProperty('mskuStatusFinishDate', '2020-02-03');
    });

    it('should be change searchText', () => {
      const getFilter = setupChangeFilter({ searchText: 'foo' });

      wrapper!
        .find(TextInput)
        .filterWhere(node => node.key() === 'msku_search_text')
        .prop('onChange')({ target: { value: 'bar' } });

      expect(getFilter()).toHaveProperty('searchText', 'bar');
    });

    it('should be change marketSkuIdsString', () => {
      const getFilter = setupChangeFilter({ marketSkuIdsString: 'foo' });

      wrapper!
        .find(TextInput)
        .filterWhere(node => node.key() === 'msku_id_search_text')
        .prop('onChange')({ target: { value: 'bar' } });

      expect(getFilter()).toHaveProperty('marketSkuIdsString', 'bar');
    });

    it('should be change marketSkuIdsString', () => {
      const getFilter = setupChangeFilter(
        { mskuStatusValue: ExtendedMskuStatusValue.REGULAR },
        { showMskuStatuses: true }
      );

      wrapper!.find(MarketSkuStatusControl).prop('onChange')(ExtendedMskuStatusValue.IN_OUT);

      expect(getFilter()).toHaveProperty('mskuStatusValue', ExtendedMskuStatusValue.IN_OUT);
    });

    it('should be change shopSkuSearchText', () => {
      const getFilter = setupChangeFilter({ shopSkuSearchText: 'foo' });

      wrapper!
        .find(TextInput)
        .filterWhere(node => {
          return node.key() === 'shop_sku_search_text';
        })
        .prop('onChange')({ target: { value: 'bar' } });

      expect(getFilter()).toHaveProperty('shopSkuSearchText', 'bar');
    });

    it('should be change seasonIds', () => {
      const getFilter = setupChangeFilter(
        { mskuStatusValue: ExtendedMskuStatusValue.SEASONAL, seasonIds: [505] },
        { showMskuStatuses: true }
      );

      wrapper!.find(SeasonControl).prop('onChange')(403);

      expect(getFilter()).toHaveProperty('seasonIds', [403]);
    });

    it('should be hidden MarketSkuStatusControl', () => {
      wrapper = mount(
        <Wrapper>
          <MarketSkuAvailabilityFilterForm expandFilter filter={{}} onUpdateFilter={jest.fn()} />
        </Wrapper>
      );

      expect(wrapper!.find(MarketSkuStatusControl)).toHaveLength(0);
    });

    it('should be hidden SeasonControl', () => {
      wrapper = mount(
        <Wrapper>
          <MarketSkuAvailabilityFilterForm expandFilter showMskuStatuses filter={{}} onUpdateFilter={jest.fn()} />
        </Wrapper>
      );

      expect(wrapper!.find(SeasonControl)).toHaveLength(0);
    });

    it('should be change fromPriceInclusive', () => {
      const getFilter = setupChangeFilter({ mskuInfoFeatures: { fromPriceInclusive: 10 } });

      wrapper!
        .find(MskuPricebandLabelControl)
        .filterWhere(node => {
          return node.prop('feature') === 'fromPriceInclusive';
        })
        .prop('onChange')({ fromPriceInclusive: 100 });

      expect(getFilter()).toEqual({ mskuInfoFeatures: { fromPriceInclusive: 100 } });
    });

    it('should be change pricebandId', () => {
      const getFilter = setupChangeFilter({ mskuInfoFeatures: { pricebandId: 5 } });

      wrapper!.find(MskuPricebandIdControl).prop('onChange')({ pricebandId: 8 });

      expect(getFilter()).toEqual({ mskuInfoFeatures: { pricebandId: 8 } });
    });

    it('should be change inTargetAssortment', () => {
      const getFilter = setupChangeFilter({ mskuInfoFeatures: { inTargetAssortment: false } });

      wrapper!.find(MskuInTargetAssortmentControl).prop('onChange')({ inTargetAssortment: true });

      expect(getFilter()).toEqual({ mskuInfoFeatures: { inTargetAssortment: true } });
    });

    it('should be change MskuSeasonalSelect', () => {
      const getFilter = setupChangeFilter({ seasonalIds: [2, 3] });
      wrapper!.find(MskuSeasonalSelect).prop('onChange')([1]);
      expect(getFilter()).toEqual({ seasonalIds: [1] });
    });
  });
});
