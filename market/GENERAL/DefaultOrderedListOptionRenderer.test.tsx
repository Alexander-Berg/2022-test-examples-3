import React from 'react';
import { render, screen } from '@testing-library/react';

import { DefaultOrderedListOptionRenderer } from './DefaultOrderedListOptionRenderer';

describe('<DefaultOrderedListOptionRenderer />', () => {
  it('renders without errors', () => {
    render(<DefaultOrderedListOptionRenderer label="label" />);
    expect(screen.getByText('label')).not.toBeNull();
  });

  it('renders falsy values', () => {
    const view = render(<DefaultOrderedListOptionRenderer label="0" />);

    let label = screen.queryByText('0');
    expect(label).not.toBeNull();

    view.rerender(<DefaultOrderedListOptionRenderer />);
    label = screen.queryByText('0');
    expect(label).toBeNull();
  });
});
