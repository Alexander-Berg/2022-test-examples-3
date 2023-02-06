import React from 'react';
import { withRouter } from 'react-router';

import { CreateDocumentPage as Page } from '.';
import { render } from '@/test-utils';

const CreateDocumentPage = withRouter(Page);

describe('<CreateDocumentPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<CreateDocumentPage />);
    }).not.toThrow();
  });
});
