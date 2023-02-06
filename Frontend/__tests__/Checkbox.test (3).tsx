import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { Checkbox } from '../Checkbox';

Enzyme.configure({ adapter: new Adapter() });

describe.skip('Checkbox', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should render correctly', () => {
    const onChange = jest.fn();
    component = mount(<Checkbox label="Checkbox 1" checked onChange={onChange} />);

    expect(component.html()).toMatchSnapshot();
  });

  it('should calling onChange', () => {
    const onChange = jest.fn();
    component = mount(<Checkbox label="Checkbox 1" checked={false} onChange={onChange} />);

    component.find('input').simulate('change');

    expect(onChange).toBeCalledTimes(1);
  });
});
