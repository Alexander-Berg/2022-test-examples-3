import React from 'react';
import { mount } from 'enzyme';

import { BugButton } from './BugButton';

describe('<BugButton />', () => {
  it('renders without errors', () => {
    mount(<BugButton />);
  });

  it('renders with position left', () => {
    mount(<BugButton position="left" />);
  });

  it('renders without popup', () => {
    mount(<BugButton withPopup={false} />);
  });
});
