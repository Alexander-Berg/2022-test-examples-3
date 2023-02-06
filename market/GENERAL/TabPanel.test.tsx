import React from 'react';
import { render } from '@testing-library/react';

import { TabPanel } from './TabPanel';

describe('<TabPanel />', () => {
  it('renders one tab without errors', () => {
    render(<TabPanel tabs={[{ title: 'Tab_1', content: 'Tab 1 content' }]} />);
  });

  it('renders two tabs without errors', () => {
    render(
      <TabPanel
        tabs={[
          { title: 'Tab_1', content: 'Tab 1 content' },
          { title: 'Tab_2', content: 'Tab 2 content' },
        ]}
      />
    );
  });
});
