import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { DataTableRow } from '@yandex-market/mbo-components/es/components/DataTable/Row/DataTableRow';
import { TableCell } from '@yandex-market/mbo-components';

import { rootReducer } from 'src/store/root/reducer';
import { DIRECT_DISCOUNT_OFFER_MOCK } from 'src/pages/promo/tests/mocks/offer';
import { DIRECT_DISCOUNT_PROMO } from 'src/pages/promo/tests/mocks/promo';
import { WrapperWithStoreAndRouter } from 'src/pages/promo/tests/WrapperWithStoreAndRouter';
import { OffersTable } from './OffersTable';
import * as promoSelectors from '../store/selectors';
import { ParticipationCell } from './ParticipationCell/ParticipationCell';
import { CategoryCell } from './CategoryCell';
import { WarehousesCell } from './WarehousesCell';
import { OtherPromosCell } from './OtherPromosCell';
import { SupplierCell } from './SupplierCell';

const store = createStore(rootReducer);

describe('<OffersTable />', () => {
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
        <OffersTable promo={DIRECT_DISCOUNT_PROMO} />
      </WrapperWithStoreAndRouter>
    );
  };

  const getTable = () => {
    return wrapper!.find(OffersTable);
  };

  const getFirstRowCellByIdx = (idx = 0) => {
    return getTable().find(DataTableRow).find(TableCell).at(idx);
  };

  beforeEach(() => {
    jest.spyOn(promoSelectors, 'selectOffersWasLoaded').mockImplementation(() => true);
  });

  describe('without offers', () => {
    beforeEach(() => {
      jest.spyOn(promoSelectors, 'selectVisibleOffers').mockImplementation(() => []);
      renderComponent();
    });

    it('should be render without errors', () => {
      const table = getTable();
      expect(table).toHaveLength(1);
    });

    it('should show no data info', () => {
      const table = getTable();
      const tableBody = table.find('tbody');
      expect(tableBody.text()).toEqual('Нет данных');
    });
  });

  describe('with DIRECT_DISCOUNT offer', () => {
    beforeEach(() => {
      jest.spyOn(promoSelectors, 'selectVisibleOffers').mockImplementation(() => [DIRECT_DISCOUNT_OFFER_MOCK]);
    });

    describe('common', () => {
      beforeEach(() => {
        renderComponent();
      });

      it('should be render without errors', () => {
        const table = getTable();
        expect(table).toHaveLength(1);
      });

      it('1st cell should has ParticipationCell', () => {
        const cell = getFirstRowCellByIdx();
        expect(cell.find(ParticipationCell)).toHaveLength(1);
      });

      it('2th cell should has current discount percent size', () => {
        const cell = getFirstRowCellByIdx(1);
        expect(cell.text()).toEqual('66.67');
      });

      it('3th cell should has minimalDiscountPercentSize', () => {
        const cell = getFirstRowCellByIdx(2);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.mechanics.minimalDiscountPercentSize.toString());
      });

      it('4th cell should has basePrice', () => {
        const cell = getFirstRowCellByIdx(3);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.basePrice!.toString());
      });

      it('5th cell should has simple price', () => {
        const cell = getFirstRowCellByIdx(4);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.price!.toString());
      });

      it('6th cell should has current discount percent size', () => {
        const cell = getFirstRowCellByIdx(5);
        expect(cell.text()).toEqual('54.95');
      });

      it('7th cell should has warnings', () => {
        const cell = getFirstRowCellByIdx(6);
        const warnings = cell.find('.Warnings > div');

        expect(warnings).toHaveLength(2);
        expect(warnings.at(0).text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.warnings[0].message);
        expect(warnings.at(1).text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.warnings[1].message);
      });

      it('8th cell should has msku', () => {
        const cell = getFirstRowCellByIdx(7);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.msku!.toString());
      });

      it('9th cell should has ssku', () => {
        const cell = getFirstRowCellByIdx(8);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.ssku);
      });

      it('10th cell should has offer name', () => {
        const cell = getFirstRowCellByIdx(9);
        expect(cell.text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.name);
      });

      it('11th cell should has <CategoryCell />', () => {
        const categoryCell = getFirstRowCellByIdx(10).find(CategoryCell);

        expect(categoryCell).toHaveLength(1);
        expect(categoryCell.prop('offer')).toEqual(DIRECT_DISCOUNT_OFFER_MOCK);
      });

      it('12th cell should has <OtherPromosCell />', () => {
        const otherPromosCell = getFirstRowCellByIdx(11).find(OtherPromosCell);

        expect(otherPromosCell).toHaveLength(1);
        expect(otherPromosCell.prop('offer')).toEqual(DIRECT_DISCOUNT_OFFER_MOCK);
      });

      it('13th cell should has <WarehousesCell />', () => {
        const warehouseCell = getFirstRowCellByIdx(12).find(WarehousesCell);

        expect(warehouseCell).toHaveLength(1);
        expect(warehouseCell.prop('offer')).toEqual(DIRECT_DISCOUNT_OFFER_MOCK);
      });

      it('14th cell should has disabled sources', () => {
        const cell = getFirstRowCellByIdx(13);

        const warnings = cell.find('.Warnings > div');

        expect(warnings).toHaveLength(2);
        expect(warnings.at(0).text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.disabledSources[0]);
        expect(warnings.at(1).text()).toEqual(DIRECT_DISCOUNT_OFFER_MOCK.disabledSources[1]);
      });

      it('15th cell should has <SupplierCell />', () => {
        const supplierCell = getFirstRowCellByIdx(14).find(SupplierCell);

        expect(supplierCell).toHaveLength(1);
        expect(supplierCell.prop('offer')).toEqual(DIRECT_DISCOUNT_OFFER_MOCK);
      });
    });
  });
});
