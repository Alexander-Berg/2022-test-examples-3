import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { DataTableRow } from '@yandex-market/mbo-components/es/components/DataTable/Row/DataTableRow';
import { TableCell } from '@yandex-market/mbo-components';

import { rootReducer } from 'src/store/root/reducer';
import { PromosTable } from './PromosTable';
import { ToPromoDetailsLink } from './ToPromoDetailsLink';
import * as promoSelectors from '../store/selectors';
import { DIRECT_DISCOUNT_PROMO } from 'src/pages/promo/tests/mocks/promo';
import { DatePeriod } from '../../components/DatePeriod';
import { Categories } from './Categories/Categories';
import { PerCategoryDiscounts } from './Categories/PerCategoriesDiscounts';
import { WrapperWithStoreAndRouter } from 'src/pages/promo/tests/WrapperWithStoreAndRouter';

const store = createStore(rootReducer);

jest.mock('src/utils/isFlagEnabled', () => {
  return {
    isFlagEnabled: (flag: string) => {
      const ENABLED_COOKIE_FLAGS = ['enableAllColumns'];
      return ENABLED_COOKIE_FLAGS.includes(flag);
    },
  };
});

describe('<PromosTable />', () => {
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
        <PromosTable />
      </WrapperWithStoreAndRouter>
    );
  };

  const getTable = () => {
    return wrapper!.find(PromosTable);
  };

  const getFirstRow = () => {
    return getTable().find(DataTableRow).at(0);
  };

  const getFirstRowCellByIdx = (idx = 0) => {
    return getFirstRow().find(TableCell).at(idx);
  };

  beforeEach(() => {
    jest.spyOn(promoSelectors, 'selectVisiblePromos').mockImplementation(() => [DIRECT_DISCOUNT_PROMO]);
    renderComponent();
  });

  it('should be render without errors', () => {
    const table = getTable();
    expect(table).toHaveLength(1);
  });

  it('should render one promo row', () => {
    const table = wrapper!.find(PromosTable);
    const rows = table.find(DataTableRow);
    expect(rows).toHaveLength(1);
  });

  it('1st cell should has link with url', () => {
    const toPromoDetailsLink = getFirstRowCellByIdx().find(ToPromoDetailsLink);
    expect(toPromoDetailsLink).toHaveLength(1);
    expect(toPromoDetailsLink.prop('promo')).toEqual(DIRECT_DISCOUNT_PROMO);
  });

  it('2nd cell should has promo name', () => {
    const cell = getFirstRowCellByIdx(1);
    expect(cell.text()).toEqual(DIRECT_DISCOUNT_PROMO.name);
  });

  it('3rd cell should has promo period component', () => {
    const promoPeriodComponent = getFirstRowCellByIdx(2).find(DatePeriod);

    expect(promoPeriodComponent).toHaveLength(1);
    expect(promoPeriodComponent.prop('promo')).toEqual(DIRECT_DISCOUNT_PROMO);
  });

  it('4th cell should has updated date', () => {
    const cell = getFirstRowCellByIdx(3);
    expect(cell.text()).toEqual(DIRECT_DISCOUNT_PROMO.updatedAt);
  });

  it('5th cell should has deadline date', () => {
    const cell = getFirstRowCellByIdx(4);
    expect(cell.text()).toEqual(DIRECT_DISCOUNT_PROMO.assortmentDeadline);
  });

  it('6th cell should has promo type', () => {
    const cell = getFirstRowCellByIdx(5);
    expect(cell.text()).toEqual('Прямая скидка');
  });

  it('7th cell should has promo status', () => {
    const cell = getFirstRowCellByIdx(6);
    expect(cell.text()).toEqual('Создана в Лоялти');
  });

  it('8th cell should warnings', () => {
    const cell = getFirstRowCellByIdx(7);
    const warnings = cell.find('.Warnings > div');
    expect(warnings).toHaveLength(2);
    expect(warnings.at(0).text()).toEqual(DIRECT_DISCOUNT_PROMO.warnings[0]);
    expect(warnings.at(1).text()).toEqual(DIRECT_DISCOUNT_PROMO.warnings[1]);
  });

  it('9th cell should contain offers count', () => {
    const cell = getFirstRowCellByIdx(8);
    expect(cell.text()).toEqual(DIRECT_DISCOUNT_PROMO.offersCount.toString());
  });

  it('10th cell should contain categories component', () => {
    const categoriesComponent = getFirstRowCellByIdx(9).find(Categories);

    expect(categoriesComponent).toHaveLength(1);
    expect(categoriesComponent.prop('promo')).toEqual(DIRECT_DISCOUNT_PROMO);
  });

  it('11th cell should contain supplierTypes', () => {
    const supplierTypeItems = getFirstRowCellByIdx(10).find('div > div');
    expect(supplierTypeItems).toHaveLength(2);
    expect(supplierTypeItems.at(0).text()).toEqual('1P');
    expect(supplierTypeItems.at(1).text()).toEqual('3P');
  });

  it('13th cell should be empty for Direct Discount', () => {
    expect(getFirstRowCellByIdx(12).text()).toEqual('');
  });

  it('14th cell should contain categories component', () => {
    const perCategoryDiscountsComponent = getFirstRowCellByIdx(13).find(PerCategoryDiscounts);

    expect(perCategoryDiscountsComponent).toHaveLength(1);
    expect(perCategoryDiscountsComponent.prop('promo')).toEqual(DIRECT_DISCOUNT_PROMO);
  });
});
