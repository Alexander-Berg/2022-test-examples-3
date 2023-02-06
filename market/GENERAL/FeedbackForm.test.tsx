import React from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'src/test/utils';
import { FeedbackForm } from './FeedbackForm';

describe('<FeedbackForm />', () => {
  test('send feedback', () => {
    const { Provider, api } = setupTestProvider();
    render(
      <Provider>
        <FeedbackForm />
      </Provider>
    );

    userEvent.click(screen.getByText('10'));

    userEvent.type(screen.getByRole('textbox'), 'good');

    userEvent.click(screen.getByText('Отправить'));

    expect(api.sessionController.setFeedback.activeRequests()).toHaveLength(1);
    expect(api.sessionController.setFeedback).toHaveBeenCalledWith({ feedback: { message: 'good', rating: 10 } });

    act(() => {
      api.sessionController.setFeedback.next().resolve();
    });
  });
});
