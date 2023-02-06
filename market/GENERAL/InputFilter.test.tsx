import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';

import { delay } from 'src/pages/promo/utils';
import { InputFilter, Props, APPLY_FILTER_DEBOUNCE_TIME } from './InputFilter';

const defaultProps: Props = {
  filterName: 'test-filter-name',
  applyFilters: () => Promise.resolve(true),
  appliedValue: '',
};

describe('InputFilter', () => {
  let wrapper: ReactWrapper | null;

  const renderWithProps = (props: Props) => {
    wrapper = mount(<InputFilter {...props} />);
  };

  const getInputNode = () => {
    return wrapper!.find('input').first().getDOMNode() as HTMLInputElement;
  };

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('with default props', () => {
    beforeEach(() => {
      renderWithProps(defaultProps);
    });

    it('should be render', () => {
      expect(wrapper!.find(InputFilter)).toHaveLength(1);
    });

    it('should be empty by default', () => {
      const input = getInputNode();

      expect(input.value).toEqual('');
    });
  });

  describe('with not empty appliedValue', () => {
    const INPUT_VALUE = 'test input value';

    beforeEach(() => {
      renderWithProps({
        ...defaultProps,
        appliedValue: INPUT_VALUE,
      });
    });

    it('should contain appliedValue', () => {
      const input = getInputNode();

      expect(input.value).toEqual(INPUT_VALUE);
    });
  });

  describe('with changing of input value', () => {
    const VALUE_FOR_INPUT = '1234';
    const mockInputCallback = jest.fn();

    beforeEach(() => {
      renderWithProps({
        ...defaultProps,
        applyFilters: mockInputCallback,
        regex: /^\d+(\.\d+)?$/,
      });
    });

    afterEach(() => {
      mockInputCallback.mockClear();
    });

    it('should contain inputted value after change', () => {
      wrapper!.find('input').simulate('change', { target: { value: VALUE_FOR_INPUT } });
      const input = wrapper!.find('input').first().getDOMNode() as HTMLInputElement;

      expect(input.value).toEqual(VALUE_FOR_INPUT);
    });

    it('should empty after change if regex not match', () => {
      wrapper!.find('input').simulate('change', { target: { value: 'word' } });
      const input = wrapper!.find('input').first().getDOMNode() as HTMLInputElement;

      expect(input.value).toEqual('');
    });

    it('should be use callback by input with delay', async () => {
      wrapper!.find('input').simulate('change', { target: { value: VALUE_FOR_INPUT } });
      expect(mockInputCallback.mock.calls.length).toEqual(0);

      await act(() => delay(APPLY_FILTER_DEBOUNCE_TIME));
      expect(mockInputCallback.mock.calls.length).toEqual(1);
    });
  });
});
