import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Variants } from './Variants';

const TEST_VARIANTS = [
  [111, 111],
  [222, 222],
];

describe('<Variants />', () => {
  it('renders without errors', () => {
    render(<Variants data={TEST_VARIANTS} onChange={jest.fn} />);
  });

  it('contains data', () => {
    const onChange = jest.fn();
    render(<Variants data={TEST_VARIANTS} onChange={onChange} />);

    userEvent.click(screen.getByText(/111/i));
    expect(onChange).toBeCalledTimes(1);

    userEvent.click(screen.getByText(/222/i));
    expect(onChange).toBeCalledTimes(2);
  });
});
