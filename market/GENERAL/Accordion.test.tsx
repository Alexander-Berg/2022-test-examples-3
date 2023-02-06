import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Accordion } from './Accordion';

describe('<Accordion />', () => {
  it('renders without errors', () => {
    const title = 'Testik';
    const content = 'Testikovich';
    const app = render(
      <Accordion title={title}>
        <span>Testikovich</span>
      </Accordion>
    );
    expect(app.queryByText(content)).toBeNull();
    userEvent.click(app.getByText(title));
    expect(app.queryByText(content)).not.toBeNull();
  });
});
