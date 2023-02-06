import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { actions } from 'modules/userStatus/Status.slice';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import store from 'store';
import { Provider } from 'react-redux';
import { problemStub, responseStub } from 'modules/userStatus/Status.stubs';
import { StatusProblemList } from './StatusProblemList';

const requestSpy = jest.fn();

const server = setupServer(
  rest.get('/user/status', (req, res, ctx) => {
    return res(ctx.json(responseStub));
  }),
  rest.post('/user/status', (req, res, ctx) => {
    requestSpy(req);
    return res(ctx.json(responseStub));
  }),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('StatusProblemList', () => {
  describe('when fetching user/status problems', () => {
    it('renders problems list', async () => {
      render(
        <Provider store={store}>
          <StatusProblemList />
        </Provider>,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(screen.queryAllByRole('button').length).toBe(responseStub.problems.length);
      });
    });
  });

  describe('when updating user/status problems', () => {
    it('renders updated problems list', async () => {
      render(
        <Provider store={store}>
          <StatusProblemList />
        </Provider>,
      );

      const button = screen.getByRole('button');

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      fireEvent.click(button);

      await waitFor(() => {
        expect(requestSpy.mock.calls[0][0].body).toStrictEqual({
          problems: [
            {
              name: problemStub.name,
            },
          ],
        });
      });
    });
  });
});
