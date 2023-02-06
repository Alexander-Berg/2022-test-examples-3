import React from 'react';
import Bluebird from 'bluebird';
import { mocked } from 'ts-jest/utils';
import { delay } from 'utils/delay';
import { render, act, screen } from '@testing-library/react';
import { Router, Route } from 'react-router-dom';
import { createMemoryHistory } from 'history';
import { RedirectByClientId } from './RedirectByClientId';
import { loadAccount } from '../../api';
import { AccountBackendResponse } from '../../types';

jest.mock('../../api', () => ({
  loadAccount: jest.fn(),
}));

const mockLoadAccount = mocked(loadAccount);

const renderPage = () => {
  const history = createMemoryHistory();

  return render(
    <Router history={history}>
      <Route exact path={`/account/${2}`}>
        account2
      </Route>
      <RedirectByClientId clientId={1} />
    </Router>,
  );
};

describe('RedirectByClientId', () => {
  const loadAccountResponse = Bluebird.resolve({ id: 2 } as AccountBackendResponse);
  const mockCancel = jest.spyOn(loadAccountResponse, 'cancel');
  mockLoadAccount.mockImplementation(() => loadAccountResponse);

  beforeEach(() => {
    mockCancel.mockClear();
    mockLoadAccount.mockClear();
  });

  test('redirect by clientId', async () => {
    await act(async () => {
      renderPage();
    });

    expect(screen.getByText('account2')).toBeInTheDocument();
  });

  test('cancel request on unmount', async () => {
    const loadAccountResponse = Bluebird.delay(100, { id: 2 } as AccountBackendResponse);
    mockLoadAccount.mockImplementation(() => loadAccountResponse);

    const { rerender } = renderPage();

    await delay(0);

    await act(async () => {
      rerender(<div />);
    });

    expect(loadAccountResponse.isCancelled()).toBeTruthy();
  });
});
