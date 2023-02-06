import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import { ReadOnlyField } from './ReadOnlyField';

describe('<ReadOnlyField/>', () => {
  it('renders without errors', () => {
    const value = 12345678909876543234567;
    render(<ReadOnlyField value={value} />);
    document.execCommand = jest.fn();
    userEvent.click(screen.getByText(value.toString(10)));
    expect(document.execCommand).toBeCalled();
  });
});
