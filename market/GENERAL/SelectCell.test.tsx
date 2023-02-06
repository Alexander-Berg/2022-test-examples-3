import React from 'react';
import { mount, shallow } from 'enzyme';

import { SelectCell } from './SelectCell';
import { EMPTY_LIST_ITEM } from 'src/pages/replenishment/variables';
import { SelectCellOptionProps } from '../ReplenishmentDataGrid.types';

const getList = () =>
  [
    { id: 1, name: 'item 1' },
    { id: 2, name: 'item 2' },
    { id: 3, name: 'item 3' },
  ] as SelectCellOptionProps[];

describe('ReplenishmentDataGrid <SelectCell />', () => {
  it('Should render', () => {
    const wrapper = shallow(<SelectCell value={EMPTY_LIST_ITEM} list={[]} />);
    expect(wrapper.find(SelectCell)).toBeDefined();
  });

  it('Should show value and title', () => {
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={[]} />);
    expect(wrapper.text()).toEqual(EMPTY_LIST_ITEM.name);
    expect(wrapper.props().value).toEqual(EMPTY_LIST_ITEM);
    expect(wrapper.find('div').props().title).toEqual(EMPTY_LIST_ITEM.name);
  });

  it('Should show formatter value and title', () => {
    const formatter = <div>test formatter</div>;
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={[]} formatterValue={formatter} />);
    expect(wrapper.find(formatter)).toBeDefined();
    expect(wrapper.props().value).toEqual(EMPTY_LIST_ITEM);
    expect(wrapper.find('div').at(0).props().title).toEqual(EMPTY_LIST_ITEM.name);
  });

  it('Should select and option after click', () => {
    const list = getList();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    expect(wrapper.exists('select')).toBeTruthy();
    expect(wrapper.find('select').props().defaultValue).toEqual('');
    expect(wrapper.find('select').props().autoFocus).toBeTruthy();
    expect(wrapper.find('option').length).toEqual(list.length + 1);
    expect(wrapper.find('option').at(0).props().value).toEqual('');
    expect(wrapper.find('option').at(0).text()).toEqual(EMPTY_LIST_ITEM.name);
    list.forEach((item, index) => {
      const option = wrapper.find('option').at(index + 1);
      expect(option.props().value).toEqual(`${item.id}`);
      expect(option.props().title).toEqual(item.name);
      expect(option.text()).toEqual(item.name);
    });
  });

  it('Should select and option after dblclick', () => {
    const list = getList();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} doubleClick />);
    wrapper.find('[data-clicked]').simulate('click', {
      type: 'click',
      stopPropagation: () => undefined,
      preventDefault: () => undefined,
    });
    expect(wrapper.exists('select')).toBeFalsy();
    wrapper.find('[data-clicked]').simulate('dblclick', {
      type: 'dblclick',
      stopPropagation: () => undefined,
      preventDefault: () => undefined,
    });
    expect(wrapper.exists('select')).toBeTruthy();
  });

  it('Select change localValue only', () => {
    const list = getList();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const select = wrapper.find('select').getDOMNode() as HTMLSelectElement;
    select.value = `${list[1].id}`;
    wrapper.find('select').simulate('change', { target: select });
    expect(select.value).toEqual(`${list[1].id}`);
    expect(wrapper.props().value).toEqual(EMPTY_LIST_ITEM);
  });

  it('Select blur setValue', () => {
    const list = getList();
    const mockSetValue = jest.fn();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const select = wrapper.find('select').getDOMNode() as HTMLSelectElement;
    select.value = `${list[1].id}`;
    wrapper.find('select').simulate('change', { target: select });
    wrapper.find('select').simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([[list[1].id]]);
  });

  it('Select key Enter setValue', () => {
    const list = getList();
    const mockSetValue = jest.fn();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const select = wrapper.find('select').getDOMNode() as HTMLSelectElement;
    select.value = `${list[1].id}`;
    wrapper.find('select').simulate('change', { target: select });
    wrapper.find('select').simulate('keydown', { nativeEvent: { key: 'Enter' } });
    expect(mockSetValue.mock.calls).toEqual([[list[1].id]]);
  });

  it('Select key Esc no setValue', () => {
    const list = getList();
    const mockSetValue = jest.fn();
    const wrapper = mount(<SelectCell value={EMPTY_LIST_ITEM} list={list} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const select = wrapper.find('select').getDOMNode() as HTMLSelectElement;
    select.value = JSON.stringify(list[1]);
    wrapper.find('select').simulate('change', { target: select });
    wrapper.find('select').simulate('keydown', { nativeEvent: { key: 'Escape' } });
    expect(mockSetValue.mock.calls.length).toEqual(0);
    expect(wrapper.exists('select')).toBeFalsy();
  });

  it('Select null value', () => {
    const list = getList();
    const mockSetValue = jest.fn();
    const value = null as unknown as SelectCellOptionProps;
    const wrapper = mount(<SelectCell value={value} list={list} setValue={mockSetValue} />);
    expect(wrapper.text()).toBe('');
  });
});
