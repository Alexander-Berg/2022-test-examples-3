import React from 'react';
import { render } from '@testing-library/react';

import { VendorName } from './VendorName';

describe('<VendorName />', () => {
  it('renders without errors', () => {
    render(<VendorName name="test vendor" id="1" />);
  });
});
