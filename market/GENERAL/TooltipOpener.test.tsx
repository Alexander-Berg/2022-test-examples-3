import React from 'react';
import { mount } from 'enzyme';

import { TooltipOpener } from '.';

describe('<TooltipOpener />', () => {
  it('renders without errors', () => {
    mount(<TooltipOpener />);
  });
});
