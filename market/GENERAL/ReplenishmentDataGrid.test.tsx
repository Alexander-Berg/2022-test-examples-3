import { Table } from 'fixed-data-table-2';
import React, { useEffect } from 'react';
import { mount, shallow } from 'enzyme';
import { times } from 'ramda';

import { ReplenishmentDataGrid } from './ReplenishmentDataGrid';
import { InputCell, SelectCell } from './components';
import {
  ReplenishmentDataGridColumnsObject,
  ReplenishmentDataGridColumnType,
  ReplenishmentDataGridExpandCellProps,
  SelectCellOptionProps,
} from './ReplenishmentDataGrid.types';

interface RowWithoutId {
  index?: number;
  group: string;
  name: string;
  select: { id: number | string; name: string };
}
interface Row extends RowWithoutId {
  id?: number;
}

const c: ReplenishmentDataGridColumnsObject<Row> = {
  id: {
    name: 'ID',
    width: 100,
    fixed: true,
    order: 1,
    sortable: true,
    type: ReplenishmentDataGridColumnType.EXPAND,
  },
  group: {
    name: 'GROUP',
    width: 100,
    fixed: false,
    order: 3,
    sort: 1,
    sortable: undefined,
    type: ReplenishmentDataGridColumnType.TREE,
    params: {
      title: 'title group',
    },
  },
  name: {
    name: <div>NAME</div>,
    width: 100,
    fixed: false,
    order: 2,
    sort: -1,
    sortable: false,
    type: ReplenishmentDataGridColumnType.INPUT,
    formatter: ({ value }) => <div>{value as string}</div>,
  },
  select: {
    name: 'SELECT',
    width: 100,
    fixed: false,
    order: 4,
    sortable: true,
    params: {
      list: [
        { id: 1, name: 'item1' },
        { id: 2, name: 'item2' },
        { id: 3, name: 'item3' },
      ],
    },
    type: ReplenishmentDataGridColumnType.SELECT2,
    formatter: ({ value }) => {
      const { name } = value as SelectCellOptionProps;
      return <div>{name}</div>;
    },
  },
};

const r: Row[] = times(
  index => ({
    id: index + 1,
    index: index + 1,
    group: `group ${index}`,
    name: `name ${index}`,
    select: { id: 1, name: 'item1' },
  }),
  10
);

r[0].id! *= -1;
r[3].id! *= -1;

const children = new Set<number>();
children.add(2); // index 1
children.add(3); // index 2
children.add(5); // index 4

describe('ReplenishmentDataGrid <ReplenishmentDataGrid />', () => {
  it('Should simple render', () => {
    const wrapper = shallow(<ReplenishmentDataGrid columns={c} rows={r} />);
    expect(wrapper.find(ReplenishmentDataGrid)).toBeDefined();
    wrapper.unmount();
    expect(wrapper.find(ReplenishmentDataGrid).length).toBeFalsy();
  });

  it('Should column and column order', () => {
    const wrapper = mount(<ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} />);
    expect(wrapper.find('.header').map(i => i.text())).toEqual(['ID', 'NAME', 'GROUP', 'SELECT']);
    Object.keys(c).forEach(key => {
      expect(
        wrapper.find(
          `.fixedDataTableCellGroupLayout_cellGroupWrapper div[title="${c[key].params?.title || c[key].name}"]`
        )
      ).toBeDefined();
    });
  });

  it('Should rows', () => {
    const wrapper = mount(<ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} />);
    expect(
      wrapper
        .find('.fixedDataTableRowLayout_rowWrapper')
        .slice(1)
        .map(i => i.find('.cell').map(j => j.text()))
    ).toEqual(r.map(i => [`${i.id}`, i.name, i.group, i.select.name]));
  });

  it('Should rows without id', () => {
    const r2 = r.map(({ name, group, select }) => ({ name, group, select }));
    const wrapper = mount(<ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r2} />);
    expect(
      wrapper
        .find('.fixedDataTableRowLayout_rowWrapper')
        .slice(1)
        .map(i => i.find('.cell').map(j => j.text()))
    ).toEqual(r.map(i => [``, i.name, i.group, i.select.name]));
  });

  it('Click columns for sort', () => {
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} onUpdateColumnSort={fnMock} />
    );
    const keyClicked = [['id'], ['select']];
    wrapper
      .find(`.fixedDataTableCellGroupLayout_cellGroupWrapper div.header[title]`)
      .forEach(header => header.simulate('click'));
    expect(fnMock.mock.calls).toEqual(keyClicked);
  });

  it('Change column width', () => {
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} onUpdateColumnWidth={fnMock} />
    );
    const FDT2 = wrapper.find(Table).props();
    if (FDT2.onColumnResizeEndCallback) {
      FDT2.onColumnResizeEndCallback(50, 'id');
    }
    expect(fnMock.mock.calls).toEqual([['id', 50]]);
  });

  it('Change column order', () => {
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} onUpdateColumnOrder={fnMock} />
    );
    const FDT2 = wrapper.find(Table).props();
    if (FDT2.onColumnReorderEndCallback) {
      FDT2.onColumnReorderEndCallback({ reorderColumn: 'group', columnBefore: 'id', columnAfter: 'name' });
    }
    expect(fnMock.mock.calls).toEqual([['group', 'id', 'name']]);
  });

  it('Open and close expand', () => {
    const fnMock = jest.fn();

    const TestExpand: React.FC<ReplenishmentDataGridExpandCellProps<Row>> = ({
      row,
      onUpdateHeight,
      height,
      width,
    }) => {
      useEffect(() => {
        onUpdateHeight(200);
        fnMock('open', height, width);
        return () => fnMock('close');
      });

      return <div className="test-expand">{JSON.stringify(row)}</div>;
    };

    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} expandCell={TestExpand} />
    );

    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(wrapper.find('.test-expand').text()).toEqual(JSON.stringify(r[0]));
    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(fnMock.mock.calls).toEqual([
      ['open', 50, 1000],
      ['close'],
      ['open', 200, 1000],
      ['close'],
      ['open', 200, 1000],
      ['close'],
    ]);
  });

  it('Does not open and close expand on empty value', () => {
    const r2 = r.map(({ id: index, name, group, select }) => ({ index, name, group, select }));
    const fnMock = jest.fn();

    const TestExpand: React.FC<ReplenishmentDataGridExpandCellProps<RowWithoutId>> = ({
      row,
      onUpdateHeight,
      height,
      width,
    }) => {
      useEffect(() => {
        onUpdateHeight(200);
        fnMock('open', height, width);
        return () => fnMock('close');
      });

      return <div className="test-expand">{JSON.stringify(row)}</div>;
    };

    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r2} expandCell={TestExpand} />
    );

    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(fnMock).not.toBeCalled();
    expect(wrapper.find('.test-expand')).toHaveLength(0);

    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(fnMock).not.toBeCalled();
  });

  it('Open and close expand without id', () => {
    const r2 = r.map(({ id: index, name, group, select }) => ({ index, name, group, select }));
    const fnMock = jest.fn();
    const TestExpand: React.FC<ReplenishmentDataGridExpandCellProps<RowWithoutId>> = ({
      row,
      onUpdateHeight,
      height,
      width,
    }) => {
      useEffect(() => {
        onUpdateHeight(200);
        fnMock('open', height, width);
        return () => fnMock('close');
      });

      return <div className="test-expand">{JSON.stringify(row)}</div>;
    };

    const c2: ReplenishmentDataGridColumnsObject<Row> = { ...c, index: c.id };
    delete c2.id;

    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c2} rows={r2} expandCell={TestExpand} />
    );

    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(wrapper.find('.test-expand').text()).toEqual(JSON.stringify(r2[0]));
    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(0).simulate('click');
    expect(fnMock.mock.calls).toEqual([
      ['open', 50, 1000],
      ['close'],
      ['open', 200, 1000],
      ['close'],
      ['open', 200, 1000],
      ['close'],
    ]);
  });

  it('Edit cell', () => {
    const fnMock = jest.fn();
    const inputText = 'test text';
    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} onUpdateRow={fnMock} />
    );
    const inputCell = wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find(InputCell);
    inputCell.simulate('click');
    inputCell.props().setValue(inputText);
    expect(fnMock.mock.calls).toEqual([[r[0], 'name', inputText]]);
  });

  it('Edit select cell', () => {
    const fnMock = jest.fn();
    const testSelect = { id: 3, name: 'item3' };
    const wrapper = mount(
      <ReplenishmentDataGrid width={1000} height={1000} columns={c} rows={r} onUpdateRow={fnMock} />
    );
    const selectCell = wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find(SelectCell);
    selectCell.find('[data-clicked]').simulate('dblclick', {
      type: 'dblclick',
      stopPropagation: () => undefined,
      preventDefault: () => undefined,
    });

    const select = wrapper
      .find('.fixedDataTableRowLayout_rowWrapper')
      .at(1)
      .find('select')
      .getDOMNode() as HTMLSelectElement;
    select.value = `${testSelect.id}`;
    wrapper.find('select').simulate('change', { target: select });
    wrapper.find('select').simulate('keydown', { nativeEvent: { key: 'Enter' } });
    expect(fnMock.mock.calls).toEqual([[r[0], 'select', testSelect.id]]);
  });

  it('Select rows', () => {
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid
        width={1000}
        height={1000}
        columns={c}
        rows={r}
        onUpdateSelect={fnMock}
        getRowGroup={({ id = 0 }) => {
          if (id < 0 || children.has(id)) {
            return id;
          }
          return 0;
        }}
        onToggleGroup={() => undefined}
      />
    );
    const wrapperRows = wrapper.find('.fixedDataTableRowLayout_rowWrapper');
    const wrapperRowsClick = (
      index: number,
      keys: { shiftKey?: boolean; ctrlKey?: boolean; metaKey?: boolean } = {}
    ) => {
      wrapperRows
        .at(index + 1)
        .find('.cell')
        .at(2)
        .simulate('click', { nativeEvent: keys });
    };
    const getMock = (indexes: number[]) => {
      return [[r.filter((row, index) => indexes.indexOf(index) > -1)]];
    };
    // нельзя выделить потомка
    wrapperRowsClick(2);
    expect(fnMock.mock.calls).toEqual(getMock([]));
    fnMock.mockReset();

    // выделение родителя
    wrapperRowsClick(3);
    expect(fnMock.mock.calls).toEqual(getMock([3]));
    fnMock.mockReset();

    // выделение через шифт без потомков
    wrapperRowsClick(6, { shiftKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([3, 5, 6]));
    fnMock.mockReset();

    // выделение через шифт в противоположную от предыдущего сторону
    wrapperRowsClick(0, { shiftKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3]));
    fnMock.mockReset();

    // довыделение через контрол для windows систем
    wrapperRowsClick(5, { ctrlKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5]));
    fnMock.mockReset();

    // довыделение через команд для макос
    wrapperRowsClick(7, { metaKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5, 7]));
    fnMock.mockReset();

    // снятие выделения через команд для мак ос
    wrapperRowsClick(7, { metaKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5]));
    fnMock.mockReset();

    // снятие сброс выделения по одиносному клику
    wrapperRowsClick(9);
    expect(fnMock.mock.calls).toEqual(getMock([9]));
    fnMock.mockReset();

    // снятие сброс выделения по одиносному клику на уже выбранном элементе
    wrapperRowsClick(9);
    expect(fnMock.mock.calls).toEqual(getMock([]));
    fnMock.mockReset();
  });

  it('Select rows without id', () => {
    const r2 = r.map(({ id: index, name, group, select }) => ({ index, name, group, select }));
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid
        width={1000}
        height={1000}
        columns={c}
        rows={r2}
        onUpdateSelect={fnMock}
        getRowGroup={({ index = 0 }) => {
          if (index < 0 || children.has(index)) {
            return index;
          }
          return 0;
        }}
        onToggleGroup={() => undefined}
      />
    );
    const wrapperRows = wrapper.find('.fixedDataTableRowLayout_rowWrapper');
    const wrapperRowsClick = (
      index: number,
      keys: { shiftKey?: boolean; ctrlKey?: boolean; metaKey?: boolean } = {}
    ) => {
      wrapperRows
        .at(index + 1)
        .find('.cell')
        .at(2)
        .simulate('click', { nativeEvent: keys });
    };
    const getMock = (indexes: number[]) => {
      return [[r2.filter((row, index) => indexes.indexOf(index) > -1)]];
    };
    // нельзя выделить потомка
    wrapperRowsClick(2);
    expect(fnMock.mock.calls).toEqual(getMock([]));
    fnMock.mockReset();

    // выделение родителя
    wrapperRowsClick(3);
    expect(fnMock.mock.calls).toEqual(getMock([3]));
    fnMock.mockReset();

    // выделение через шифт без потомков
    wrapperRowsClick(6, { shiftKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([3, 5, 6]));
    fnMock.mockReset();

    // выделение через шифт в противоположную от предыдущего сторону
    wrapperRowsClick(0, { shiftKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3]));
    fnMock.mockReset();

    // довыделение через контрол для windows систем
    wrapperRowsClick(5, { ctrlKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5]));
    fnMock.mockReset();

    // довыделение через команд для макос
    wrapperRowsClick(7, { metaKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5, 7]));
    fnMock.mockReset();

    // снятие выделения через команд для мак ос
    wrapperRowsClick(7, { metaKey: true });
    expect(fnMock.mock.calls).toEqual(getMock([0, 3, 5]));
    fnMock.mockReset();

    // снятие сброс выделения по одиносному клику
    wrapperRowsClick(9);
    expect(fnMock.mock.calls).toEqual(getMock([9]));
    fnMock.mockReset();

    // снятие сброс выделения по одиносному клику на уже выбранном элементе
    wrapperRowsClick(9);
    expect(fnMock.mock.calls).toEqual(getMock([]));
    fnMock.mockReset();
  });

  it('Toggle group', () => {
    const fnMock = jest.fn();
    const wrapper = mount(
      <ReplenishmentDataGrid
        width={1000}
        height={1000}
        columns={c}
        rows={r}
        getRowGroup={row => {
          if (row.id < 0 || children.has(row.id)) {
            return row.id;
          }
          return 0;
        }}
        onToggleGroup={fnMock}
      />
    );
    // клик по родителю группы
    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(1).find('.cell').at(2).find('div').at(2).simulate('click');
    expect(fnMock.mock.calls).toEqual([[r[0]]]);
    fnMock.mockReset();

    // клик по потомку группы
    wrapper.find('.fixedDataTableRowLayout_rowWrapper').at(2).find('.cell').at(2).find('div').at(2).simulate('click');
    expect(fnMock.mock.calls).toEqual([]);
    fnMock.mockReset();
  });
});
