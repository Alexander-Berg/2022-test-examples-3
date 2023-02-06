import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { Slot, SLOT_CLASS_NAME } from './Slot';

describe('Slot', () => {
  it('renders', () => {
    const { container } = render(<Slot />);
    const slots = container.getElementsByClassName(SLOT_CLASS_NAME);
    expect(slots).toHaveLength(1);
  });

  it('click works', () => {
    const onClickCallback = jest.fn();
    const { container } = render(<Slot onClick={onClickCallback} />);
    const slots = container.getElementsByClassName(SLOT_CLASS_NAME);

    fireEvent.click(slots.item(0)!);
    expect(onClickCallback).toBeCalledTimes(1);
  });
});
