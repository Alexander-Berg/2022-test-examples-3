import React from 'react';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { BlueWoodpecker } from './BlueWoodpecker';

const peckers = {
  name: 'ParameterTutorialVideo',
  querySelector: '.ParameterTutorialVideo',
  content: 'ParameterTutorialVideo',
  expires: new Date(2024, 1, 1),
};

describe('BlueWoodpecker', () => {
  test('show/close', async () => {
    const markAsRead = jest.fn();
    const app = render(
      <div>
        <div className={peckers.name} />
        <BlueWoodpecker {...peckers} markAsRead={markAsRead} />
      </div>
    );

    await waitFor(() => {
      app.getByText(peckers.content);
    });

    const peck = app.container.querySelector(peckers.querySelector);
    expect(peck).toBeTruthy();
    userEvent.click(peck!);

    expect(app.queryByText(peckers.content)).toBeFalsy();
    expect(markAsRead.mock.calls.length).toBe(1);
  });
});
