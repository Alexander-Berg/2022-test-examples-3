import React from 'react';
import { shallow } from 'enzyme';

import { useColumns } from './useColumns';
import { ReplenishmentDataGridColumnsObject } from '../ReplenishmentDataGrid.types';
import { ColumnSort, SaveColumnsState, SetColumnOrder, SetColumnSort, SetColumnWidth } from './useColumns.types';

interface TestComponentProps {
  directColumns: ReplenishmentDataGridColumnsObject<unknown>;
}
interface TestPropComponentProps {
  columns: ReplenishmentDataGridColumnsObject<unknown>;
  setColumnWidth: SetColumnWidth;
  setColumnOrder: SetColumnOrder;
  columnSort: ColumnSort;
  setColumnSort: SetColumnSort;
}

const TestPropComponent: React.FC<TestPropComponentProps> = () => {
  return <div />;
};

const TestComponent: React.FC<TestComponentProps> = ({ directColumns }) => {
  const [columns, setColumnWidth, setColumnOrder, columnSort, setColumnSort] = useColumns(
    'ReplenishmentDetailPage',
    directColumns
  );
  return <TestPropComponent {...{ columns, setColumnWidth, setColumnOrder, columnSort, setColumnSort }} />;
};

const STORAGE_MOCK: SaveColumnsState = {
  index: {
    width: 150,
    order: 2,
  },
  status: {
    width: 132,
    order: 1,
  },
  groupId: {
    width: 150,
    order: 0,
  },
  categoryName: {
    width: 90,
    order: 3,
  },
  msku: {
    width: 110,
    order: 6,
  },
  ssku: {
    width: 200,
    order: 5,
  },
  title: {
    width: 400,
    order: 4,
  },
  supplier: {
    width: 300,
    order: 7,
  },
};

const getDirectColumns: () => ReplenishmentDataGridColumnsObject<unknown> = () => ({
  index: {
    name: '№',
    sortable: false,
    order: 0,
    width: 50,
  },
  status: {
    name: 'Статус',
    sortable: false,
    order: 1,
    width: 32,
  },
  groupId: {
    name: '№ Группы',
    width: 50,
    order: 2,
  },
  categoryName: {
    name: 'Категория',
    order: 3,
  },
  msku: {
    name: 'MSKU',
    order: 4,
  },
  ssku: {
    name: 'SSKU',
    width: 400,
    order: 5,
  },
  title: {
    name: 'Название',
    width: 300,
    order: 6,
  },
  supplier: {
    name: 'Поставщик',
    width: 300,
    order: 7,
  },
});
let LocalStorageValue: any;
const localStorageReset = () => {
  LocalStorageValue = JSON.stringify(STORAGE_MOCK);
};
const localStorage = {
  getItem: () => {
    return LocalStorageValue;
  },
  setItem: (name: string, value: any) => {
    LocalStorageValue = value;
  },
};

beforeAll(() => {
  Object.defineProperty(window, 'localStorage', {
    value: localStorage,
  });
  Object.defineProperty(global, 'localStorage', {
    value: localStorage,
  });
  localStorageReset();
});

describe('ReplenishmentDataGrid hook useColumns', () => {
  it('Hook init', () => {
    const directColumns = getDirectColumns();
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    expect(wrapper.props().columns).toBeDefined();
    expect(wrapper.props().setColumnWidth).toBeDefined();
    expect(wrapper.props().setColumnOrder).toBeDefined();
    expect(wrapper.props().columnSort).toBeDefined();
    expect(wrapper.props().setColumnSort).toBeDefined();
  });

  it('Check load localStorage', () => {
    const directColumns = getDirectColumns();
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { columns } = wrapper.props();
    Object.entries(STORAGE_MOCK).forEach(([key, column]) => {
      expect(columns[key].width).toEqual(column.width);
      expect(columns[key].order).toEqual(column.order);
    });
  });

  it('Check no data localStorage', () => {
    localStorage.setItem('', undefined);
    const directColumns = getDirectColumns();
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { columns } = wrapper.props();
    Object.entries(directColumns).forEach(([key, column]) => {
      expect(columns[key].width).toEqual(column.width || 100);
      expect(columns[key].order).toEqual(column.order);
    });
  });

  it('Check fail load localStorage', () => {
    localStorage.setItem('', '{error');
    const directColumns = getDirectColumns();
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { columns } = wrapper.props();
    Object.entries(directColumns).forEach(([key, column]) => {
      expect(columns[key].width).toEqual(column.width || 100);
      expect(columns[key].order).toEqual(column.order);
    });
  });

  it('Check sort select', () => {
    const directColumns = getDirectColumns();
    const testSort = { columnKey: 'msku', sort: 1 };
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnSort } = wrapper.props();
    setColumnSort(testSort);
    expect(wrapper.props().columnSort).toEqual(testSort);
    const { columns } = wrapper.props();
    expect(columns[testSort.columnKey].sort).toEqual(testSort.sort);
  });

  it('Check save width', () => {
    localStorageReset();
    const directColumns = getDirectColumns();
    const testColumn = 'msku';
    const testWidth = 999;
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnWidth } = wrapper.props();
    setColumnWidth(testColumn, testWidth);
    const { columns } = wrapper.props();
    expect(columns[testColumn].width).toEqual(testWidth);
  });

  it('Check save order no before', () => {
    localStorageReset();
    const directColumns = getDirectColumns();
    const testColumn = 'msku';
    const testBefore = undefined;
    const testAfter = 'groupId';
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnOrder } = wrapper.props();
    setColumnOrder(testColumn, testBefore, testAfter);
    const { columns } = wrapper.props();
    expect(columns[testColumn].order).toEqual(0);
    expect(columns[testAfter].order).toEqual(1);
  });

  it('Check save order no after', () => {
    localStorageReset();
    const directColumns = getDirectColumns();
    const testColumn = 'msku';
    const testBefore = 'groupId';
    const testAfter = undefined;
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnOrder } = wrapper.props();
    setColumnOrder(testColumn, testBefore, testAfter);
    const { columns } = wrapper.props();
    expect(columns[testColumn].order).toEqual(1);
    expect(columns[testBefore].order).toEqual(0);
  });

  it('Check save order unknow', () => {
    localStorageReset();
    const directColumns = getDirectColumns();
    const testColumn = 'msku';
    const testBefore = undefined;
    const testAfter = undefined;
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnOrder } = wrapper.props();
    setColumnOrder(testColumn, testBefore, testAfter);
    const { columns } = wrapper.props();
    expect(columns[testColumn].order).toEqual(0);
  });

  it('Check bad data test', () => {
    localStorageReset();
    const directColumns = getDirectColumns();
    const testColumn = 'msku';
    const testBefore = 'unknown';
    const testAfter = 'unknown';
    const wrapper = shallow(<TestComponent directColumns={directColumns} />);
    const { setColumnOrder } = wrapper.props();
    setColumnOrder(testColumn, testBefore, testAfter);
    const { columns } = wrapper.props();
    expect(columns[testColumn].order).toEqual(0);
  });
});
