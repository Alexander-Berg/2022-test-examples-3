import React from 'react';
import { render, screen } from '@testing-library/react';

import { Markup } from '.';

describe('<Markup />', () => {
  it('should wrap matched string', () => {
    render(<Markup term="t">test</Markup>);

    expect(screen.getAllByText('t')).toHaveLength(2);
  });
});
