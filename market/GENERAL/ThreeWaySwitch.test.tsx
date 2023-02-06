import React from 'react';
import { mount } from 'enzyme';

import { ThreeWaySwitch } from './ThreeWaySwitch';

describe('<ThreeWaySwitch />', () => {
  it('renders without errors', () => {
    mount(<ThreeWaySwitch onChange={jest.fn()} />);
  });
});
