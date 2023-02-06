import React from 'react';
import { render, screen } from '@testing-library/react';
import { UpsaleFactorsComponent } from './UpsaleFactorsComponent';

describe('UpsaleFactorsComponent', () => {
  it('should not throw error with null factors', () => {
    expect(() => {
      render(<UpsaleFactorsComponent />);
    }).not.toThrow();
  });

  it('should render two factors', () => {
    render(
      <UpsaleFactorsComponent
        factors={[
          { id: 1, name: 'factor 1' },
          { id: 2, name: 'factor 2' },
        ]}
      />,
    );

    screen.getByText('factor 1');
    screen.getByText('factor 2');
    const factors = screen.getAllByText('factor', { exact: false });
    expect(factors).toHaveLength(2);
  });
});
