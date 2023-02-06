import React from 'react';
import { mount } from 'enzyme';

import { Modal } from '.';

describe('<Modal />', () => {
  it('renders without errors', () => {
    mount(<Modal />);
  });
});
