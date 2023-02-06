import React from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SnowMan } from './SnowMan';
import { MAX_FLAKES_COUNT, NEW_FLAKES_COUNT } from './constants';

describe('<SnowMan />', () => {
  it('renders limited number of flakes', async () => {
    jest.useFakeTimers();
    const app = render(<SnowMan />);
    userEvent.click(app.getByTitle('Жми'));
    expect(getAllFlakes()).toHaveLength(NEW_FLAKES_COUNT);
    act(() => {
      // every timer adds NEW_FLAKES_COUNT flakes (+ initial click)
      for (let i = 0; i < 10; i++) {
        jest.runOnlyPendingTimers();
      }
      // there should be (MAX_FLAKES_COUNT + NEW_FLAKES_COUNT) flakes, but there is a limit of MAX_FLAKES_COUNT
    });

    expect(getAllFlakes()).toHaveLength(MAX_FLAKES_COUNT);

    app.unmount();
    expect(getAllFlakes()).toHaveLength(0);
  });
});

function getAllFlakes() {
  return screen.queryAllByTitle('Это снежинка');
}
