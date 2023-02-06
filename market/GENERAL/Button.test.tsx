import React from 'react';
import { mount } from 'enzyme';

import { Button } from '.';

describe('<Button />', () => {
  it('renders without errors', () => {
    mount(<Button />);
  });
});
