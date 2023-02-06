import React from 'react';
import { withRouter } from 'react-router';

import { ClientPage as Page } from './client.page';
import { render } from '@/test-utils';

const ClientPage = withRouter(Page);

describe('<ClientPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<ClientPage />);
    }).not.toThrow();
  });
});
