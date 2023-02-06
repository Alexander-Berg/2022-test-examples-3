import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { actions } from 'modules/userStatus/Status.slice';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import store from 'store';
import { Provider } from 'react-redux';
import { flagStub, responseStub } from 'modules/userStatus/Status.stubs';
import { StatusFlagList } from './StatusFlagList';

const requestSpy = jest.fn();
const newFlagStub = { ...flagStub, value: !flagStub.value };

const server = setupServer(
  rest.get('/user/status', (req, res, ctx) => {
    return res(ctx.json(responseStub));
  }),
  rest.post('/user/status', (req, res, ctx) => {
    requestSpy(req);
    return res(ctx.json({ ...responseStub, flags: [newFlagStub] }));
  }),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('StatusFlagList', () => {
  describe('when fetching user/status flags', () => {
    it('renders flags list', async () => {
      render(
        <Provider store={store}>
          <StatusFlagList />
        </Provider>,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(screen.queryAllByRole('checkbox').length).toBe(responseStub.flags.length);
      });
    });
  });

  describe('when updating user/status flags', () => {
    it('renders updated flags list', async () => {
      render(
        <Provider store={store}>
          <StatusFlagList />
        </Provider>,
      );

      const checkbox = screen.getByRole('checkbox');

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(checkbox).not.toBeChecked();
      });

      fireEvent.click(checkbox);

      await waitFor(() => {
        expect(checkbox).toBeChecked();
      });

      expect(requestSpy.mock.calls[0][0].body).toStrictEqual({
        flags: [
          {
            name: newFlagStub.name,
            value: newFlagStub.value,
          },
        ],
      });
    });

    it('renders loading spinner', async () => {
      const { container } = render(
        <Provider store={store}>
          <StatusFlagList />
        </Provider>,
      );

      const checkbox = screen.getByRole('checkbox');

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      fireEvent.click(checkbox);

      await waitFor(() => {
        expect(container.getElementsByClassName('crm-spinner').length).toBe(1);
      });

      await waitFor(() => {
        expect(container.getElementsByClassName('crm-spinner').length).toBe(0);
      });
    });
  });
});
