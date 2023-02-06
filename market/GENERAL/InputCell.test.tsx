import React from 'react';
import { mount } from 'enzyme';

import { InputCell } from './InputCell';

describe('ReplenishmentDataGrid <InputCell />', () => {
  it('Should render', () => {
    const wrapper = mount(<InputCell value="testText" />);
    expect(wrapper.find(InputCell)).toBeDefined();
  });

  it('Should show value and title', () => {
    const text = 'testText';
    const wrapper = mount(<InputCell value={text} />);
    expect(wrapper.text()).toEqual(text);
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').props().title).toEqual(text);
  });

  it('Should input after click', () => {
    const text = 'testText';
    const wrapper = mount(<InputCell value={text} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    expect(wrapper.exists('input')).toBeTruthy();
    expect(wrapper.find('input').props().value).toEqual(text);
    expect(wrapper.find('input').props().autoFocus).toBeTruthy();
  });

  it('Input change localValue only', () => {
    const text = 'testText';
    const inputText = 'input test text';
    const wrapper = mount(<InputCell value={text} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    wrapper.find('input').simulate('change', { target: { value: inputText } });
    expect(wrapper.find('input').props().value).toEqual(inputText);
    expect(wrapper.props().value).toEqual(text);
  });

  it('Input blur setValue', () => {
    const text = 'testText';
    const inputText = 'input test text';
    const mockSetValue = jest.fn();
    const wrapper = mount(<InputCell value={text} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    wrapper.find('input').simulate('change', { target: { value: inputText } });
    wrapper.find('input').simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([[inputText]]);
  });

  it('Input key Enter setValue', () => {
    const text = 'testText';
    const inputText = 'input test text';
    const mockSetValue = jest.fn();
    const wrapper = mount(<InputCell value={text} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const input = wrapper.find('input');
    input.simulate('change', { target: { value: inputText } });
    input.simulate('keydown', { key: 'Enter' });
    input.simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([[inputText]]);
  });

  it('Input key Esc no setValue', () => {
    const text = 'testText';
    const inputText = 'input test text';
    const mockSetValue = jest.fn();
    const wrapper = mount(<InputCell value={text} setValue={mockSetValue} />);
    wrapper
      .find('[data-clicked]')
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    const input = wrapper.find('input');
    input.simulate('change', { target: { value: inputText } });
    input.simulate('keydown', { key: 'Escape' });
    input.simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([]);
    expect(wrapper.exists('input')).toBeFalsy();
  });

  it('Should show formatter value and title', () => {
    const text = 'testText';
    const formatter = <div>testFormatter</div>;
    const wrapper = mount(<InputCell value={text} formatterValue={formatter} />);
    expect(wrapper.find(formatter)).toBeDefined();
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual(text);
  });

  it('Input fast edit', () => {
    const text = 'testText';
    const mockSetValue = jest.fn();
    const wrapper = mount(<InputCell value={text} setValue={mockSetValue} />);
    wrapper.find('[data-clicked]').simulate('keydown', { key: '1' });
    let input = wrapper.find('input');
    input.simulate('keydown', { key: 'ArrowDown' });
    input.simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([['1']]);
    mockSetValue.mockReset();
    wrapper.find('[data-clicked]').simulate('keydown', { key: '2' });
    input = wrapper.find('input');
    input.simulate('keydown', { key: 'ArrowUp' });
    input.simulate('blur');
    expect(mockSetValue.mock.calls).toEqual([['2']]);
  });
});
