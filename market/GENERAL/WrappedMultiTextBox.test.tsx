import React from 'react';
import { mount } from 'enzyme';
import { getParameterMock } from '@yandex-market/mbo-parameter-editor/es/__mocks__/parameter';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { MultiTextBox } from '@yandex-market/mbo-parameter-editor';

import { ALIASES_PARAM_ID } from 'src/shared/constants';
import { WrappedMultiTextBox, WrappedMultiTextBoxProps } from './WrappedMultiTextBox';

function setup(rest: Partial<WrappedMultiTextBoxProps> = {}) {
  const props: WrappedMultiTextBoxProps = {
    parameter: getParameterMock({ valueType: ValueType.STRING, isMultivalue: true }),
    parameterMeta: {},
    values: [],
    onChange: jest.fn(),
    Input: () => null,
    taskRestrictions: { aliasAdding: true },
    ...rest,
  };
  return mount(<WrappedMultiTextBox {...props} />);
}

describe('<WrappedMultiTextBox />', () => {
  it('should render', () => {
    const wrapper = setup();
    expect(wrapper.exists()).toBeTrue();
  });

  it('should show addable button by non alias parameter with false restriction', () => {
    const wrapper = setup({ taskRestrictions: { aliasAdding: false } });
    const component = wrapper.find(MultiTextBox);
    expect(component.prop('addable')).toBeTrue();
  });

  it('should show addable button by non alias parameter with true restriction', () => {
    const wrapper = setup({ taskRestrictions: { aliasAdding: true } });
    const component = wrapper.find(MultiTextBox);
    expect(component.prop('addable')).toBeTrue();
  });

  it('should show addable button by alias parameter with true restriction', () => {
    const wrapper = setup({
      taskRestrictions: { aliasAdding: true },
      parameter: getParameterMock({ valueType: ValueType.STRING, isMultivalue: true, id: ALIASES_PARAM_ID }),
    });
    const component = wrapper.find(MultiTextBox);
    expect(component.prop('addable')).toBeTrue();
  });

  it('should hide addable button by alias parameter with false restriction', () => {
    const wrapper = setup({
      taskRestrictions: { aliasAdding: false },
      parameter: getParameterMock({ valueType: ValueType.STRING, isMultivalue: true, id: ALIASES_PARAM_ID }),
    });
    const component = wrapper.find(MultiTextBox);
    expect(component.prop('addable')).toBeFalse();
  });
});
