import React from 'react';
import { render } from '@testing-library/react';

import { SimpleTextRenderer } from './SimpleTextRenderer';

describe('<SimpleTextRenderer/>', () => {
  it('renders string without errors', () => {
    render(<SimpleTextRenderer value="string" />);
  });

  it('renders number without errors', () => {
    render(<SimpleTextRenderer value={23123123131313757575121121212112112121275757575748} />);
  });

  it('renders undefined without errors', () => {
    render(<SimpleTextRenderer value={undefined} />);
  });
});
