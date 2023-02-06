import React from 'react';
import { withRouter } from 'react-router';

import Page from './page-data.page';

import { render } from '@/test-utils';

const PageDataPage = withRouter(Page);

describe('<PageDataPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<PageDataPage />);
    }).not.toThrow();
  });
});
