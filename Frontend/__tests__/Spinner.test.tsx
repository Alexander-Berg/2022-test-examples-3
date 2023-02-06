import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { Spinner } from '../Spinner';

Enzyme.configure({ adapter: new Adapter() });

describe('Spinner', () => {
  let SpinnerComponent: ReactWrapper;

  afterEach(() => {
    SpinnerComponent.unmount();
  });

  it('should render correctly', () => {
    SpinnerComponent = mount(<Spinner />);

    expect(SpinnerComponent.html()).toMatchSnapshot();
  });

  it('should mix className', () => {
    SpinnerComponent = mount(<Spinner className="className" />);

    expect(SpinnerComponent.html()).toMatchSnapshot();
  });
});
