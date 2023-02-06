import React from 'react';
import { mount } from 'enzyme';

import { WordEditor, WordEditorProps } from 'src/shared/common-logs/components/WordEditor/WordEditor';

function setup(rest: Partial<WordEditorProps> = {}) {
  const props: WordEditorProps = {
    multiValue: true,
    value: [{ name: 'test1' }, { name: 'test2' }, { name: 'test3' }],
    onChangeValue: jest.fn(),
    ...rest,
  };
  return mount(<WordEditor {...props} />);
}
describe('<WordEditor />', () => {
  it('should render', () => {
    const wrapper = setup();
    expect(wrapper.exists()).toBeTrue();
  });

  it('should show add alias button by default props', () => {
    const wrapper = setup();
    expect(wrapper.text()).toContain('Добавить значение');
  });

  it('should hide add alias button', () => {
    const wrapper = setup({ addable: false });
    expect(wrapper.text()).not.toContain('Добавить значение');
  });
});
