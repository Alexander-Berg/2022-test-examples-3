import React from 'react';
import { render } from '@testing-library/react';

import { MboErrorToast } from '.';

describe('<MboErrorToast />', () => {
  it('renders without errors', () => {
    render(<MboErrorToast />);
  });
});
