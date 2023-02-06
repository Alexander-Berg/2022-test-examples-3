import React from 'react';
import { Provider } from 'react-redux';
import { createMemoryHistory } from 'history';
import { Router } from 'react-router-dom';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { render, waitFor, screen, cleanup, fireEvent } from '@testing-library/react/pure';
import store from 'store';
import intersectionObserverMock from 'services/IntersectionWatcher/__mock__/intersectionObserverMock';
import { account, tabs } from './stubs';
import { AccountCardConnected } from '..';
import { AccountUrlContext } from '../../AccountUrlContext';

window.IntersectionObserver = intersectionObserverMock;

const server = setupServer(
  rest.get('/view/account', (req, res, ctx) => {
    return res(ctx.json(account));
  }),
  rest.get('/account/9955466/tabs', (req, res, ctx) => {
    return res(ctx.json(tabs));
  }),
);

describe('AccountCard', () => {
  beforeAll(() => {
    server.listen();
    server.resetHandlers();

    const history = createMemoryHistory();
    history.push('/account');

    render(
      <Provider store={store}>
        <Router history={history}>
          <AccountUrlContext.Provider value="/account">
            <AccountCardConnected id={9955466} />
          </AccountUrlContext.Provider>
        </Router>
      </Provider>,
    );

    return waitFor(() => screen.findAllByText(account.info.name));
  });

  afterAll(() => {
    server.close();
    cleanup();
  });

  test('display tabs', () => {
    expect(screen.getByText('Сводная информация')).toBeInTheDocument();
    expect(screen.getByText('Кредиты')).toBeInTheDocument();
  });

  test('has title', () => {
    expect(screen.getAllByText('Лебедев Никита').length).toBeGreaterThan(0);
  });

  test('switch to credits tab', async () => {
    fireEvent.click(screen.getByText('Кредиты'));

    await waitFor(() => screen.findByText(/AccountCredits/), { timeout: 5000 });

    expect(screen.getByText(/AccountCredits/)).toBeInTheDocument();
    expect(screen.getByText(/AccountDebts/)).toBeInTheDocument();
  });
});
