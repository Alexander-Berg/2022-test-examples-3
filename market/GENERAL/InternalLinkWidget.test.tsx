import React from 'react';
import { render } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';

import { InternalLinkWidget } from './InternalLinkWidget';

describe('<InternalLinkWidget/>', () => {
  it('renders without errors', () => {
    render(
      <BrowserRouter>
        <InternalLinkWidget href="/link-address" />
      </BrowserRouter>
    );
  });
});
