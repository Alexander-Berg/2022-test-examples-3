import { AutoSuggestInput } from 'src/components/AutoSuggestInput/AutoSuggestInput';
import { configure, mount, ReactWrapper } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import * as React from 'react';

configure({ adapter: new Adapter() });

let wrapper: ReactWrapper;
let focused: boolean;
const defaultOptions = {
  value: '',
  options: [
    {
      val: 1,
      text: '1',
    },
    {
      val: 2,
      text: '1-2',
    },
    {
      val: 3,
      text: '1-2-3',
    },
  ],
  onChange: (value: string) => {
    defaultOptions.value = value;
  },
  onSelect: () => {
    return undefined;
  },
  onFocus: () => {
    focused = true;
  },
  onBlur: () => {
    focused = false;
  },
};

beforeAll(() => {
  wrapper = mount(<AutoSuggestInput {...defaultOptions} />);
  wrapper.unmount();
});

describe('AutoSuggestInput', () => {
  it('input check', () => {
    wrapper.setProps(defaultOptions);
    wrapper.find('.textinput__control').simulate('change', { target: { value: 'Hello' } });
    expect(defaultOptions.value).toEqual('Hello');
  });

  it('show menu check', () => {
    wrapper.find('.textinput__control').simulate('focus');
    expect(wrapper.find('.menu__text').length).toEqual(3);
  });

  it('menu click check', () => {
    let selectedVal = 0;
    wrapper.setProps({
      onSelect: (val: number) => {
        selectedVal = val;
      },
    });
    wrapper
      .find('.menu__item')
      .first()
      .simulate('click');
    expect(selectedVal).toEqual(1);
  });

  it('focus/blur check', () => {
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('focus');
    expect(focused).toEqual(true);
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('blur');
    expect(focused).toEqual(false);
  });

  it('arrow navigation check', () => {
    let selectedVal = 0;
    wrapper.setProps({
      onSelect: (val: number) => {
        selectedVal = val;
      },
    });
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('focus');
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'ArrowDown' });
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'Enter' });
    expect(selectedVal).toEqual(2);
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('focus');
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'ArrowDown' });
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'ArrowDown' });
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'Enter' });
    expect(selectedVal).toEqual(1);
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('focus');
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'ArrowUp' });
    wrapper
      .find('.textinput__control')
      .first()
      .simulate('keydown', { key: 'Enter' });
    expect(selectedVal).toEqual(3);
  });
});
