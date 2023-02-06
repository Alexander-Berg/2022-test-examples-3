import React from 'react';
import { render, screen } from '@testing-library/react';

import { Pendable } from '.';

describe('<Pendable />', () => {
  it('should contains content', () => {
    render(<Pendable>Some content</Pendable>);

    expect(screen.getByText(/Some content/)).toBeInTheDocument();
  });
});
