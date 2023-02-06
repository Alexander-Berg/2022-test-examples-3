import React from 'react';
import { withRouter } from 'react-router';

import { NotFoundPage as Page } from '.';

import { render } from '@/test-utils';

const NotFoundPage = withRouter(Page);

describe('<NotFoundPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<NotFoundPage />);
    }).not.toThrow();
  });
});
