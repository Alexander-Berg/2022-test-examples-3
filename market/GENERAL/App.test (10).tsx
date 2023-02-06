import React from 'react';
import { render, screen } from '@testing-library/react';

import { App } from './App';

describe('<App />', () => {
  it('render without errors', () => {
    render(<App />);
    expect(screen.getByText('Something here')).toBeInTheDocument();
  });
});
