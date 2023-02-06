import React from 'react';
import { withRouter } from 'react-router';

import { UnauthorizedPage as Page } from '.';

import { render } from '@/test-utils';

const UnauthorizedPage = withRouter(Page);

describe('<UnauthorizedPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<UnauthorizedPage />);
    }).not.toThrow();
  });
});
