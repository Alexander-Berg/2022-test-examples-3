import React from 'react';
import { createMemoryHistory } from 'history';
import { Router, Route } from 'react-router-dom';
import { render, act, screen } from '@testing-library/react';
import { mocked } from 'ts-jest/utils';
import { AccountRouter } from './AccountRouter';
import { RedirectByClientId } from './RedirectByClientId';
import { AccountCardConnected } from './AccountCard';

jest.mock('./RedirectByClientId', () => ({
  RedirectByClientId: jest.fn(({ clientId }) => `clientId:${clientId}`),
}));

jest.mock('./AccountCard', () => ({
  AccountCardConnected: jest.fn(({ id }) => `accountId:${id}`),
}));

const MockedRedirectByClientId = mocked(RedirectByClientId);
const MockedAccountCardConnected = mocked(AccountCardConnected);

const renderPage = ({ path }: { path: string }) => {
  const history = createMemoryHistory();

  history.push(path);

  return render(
    <Router history={history}>
      <Route path="/account/:id?" component={AccountRouter} />
    </Router>,
  );
};

describe('AccountRouter', () => {
  beforeEach(() => {
    MockedRedirectByClientId.mockClear();
    MockedAccountCardConnected.mockClear();
  });

  test('redirect by clientId', async () => {
    await act(async () => {
      renderPage({ path: '/account?clientId=1' });
    });

    expect(screen.getByText('clientId:1')).toBeInTheDocument();
  });

  test('display account card by accountId', async () => {
    await act(async () => {
      renderPage({ path: '/account/1' });
    });

    expect(screen.getByText('accountId:1')).toBeInTheDocument();
  });

  test('render empty page', () => {
    const { container } = renderPage({ path: '/account/text' });

    expect(container.innerHTML).toBe('');
  });
});
