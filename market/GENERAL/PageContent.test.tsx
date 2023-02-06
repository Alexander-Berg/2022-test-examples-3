import React from 'react';
import { render } from '@testing-library/react';

import { PagedContent } from '.';

describe('<PagedContent />', () => {
  it('renders without errors', () => {
    render(
      <PagedContent className="some-class" pageSize={1} totalCount={1}>
        {() => <div>some child</div>}
      </PagedContent>
    );
  });
});
