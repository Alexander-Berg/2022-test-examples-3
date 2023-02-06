import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { actions } from 'modules/userStatus/Status.slice';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { createStore } from 'store/store';
import { Provider } from 'react-redux';
import { statusStub, transitionStub, responseStub } from 'modules/userStatus/Status.stubs';
import { StatusDropdown } from './StatusDropdown';

const requestSpy = jest.fn();

const server = setupServer(
  rest.get('/user/status', (req, res, ctx) => {
    return res(ctx.json(responseStub));
  }),
  rest.post('/user/status', (req, res, ctx) => {
    requestSpy(req);
    return res(ctx.json({ ...responseStub, status: transitionStub, transitions: [statusStub] }));
  }),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('StatusDropdown', () => {
  describe('when fetching user/status transitons', () => {
    it('renders transitons list', async () => {
      const store = createStore();
      render(
        <Provider store={store}>
          <StatusDropdown />
        </Provider>,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(screen.queryAllByRole('listbox').length).toBe(responseStub.transitions.length);
      });
    });

    it('renders status', async () => {
      const store = createStore();
      render(
        <Provider store={store}>
          <StatusDropdown />
        </Provider>,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(screen.getByText(statusStub.text)).toBeInTheDocument();
      });
    });
  });

  describe('when choosing other transiton', () => {
    it('renders new transiton status', async () => {
      const store = createStore();
      render(
        <Provider store={store}>
          <StatusDropdown />
        </Provider>,
      );

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      store.dispatch(actions.getStatus() as any);

      await waitFor(() => {
        expect(screen.getByText(statusStub.text)).toBeInTheDocument();
      });

      userEvent.click(screen.getByText(statusStub.text));
      userEvent.click(screen.getByText(transitionStub.text));

      await waitFor(() => {
        expect(screen.getByRole('listbox')).toHaveTextContent(transitionStub.text);
      });

      expect(requestSpy.mock.calls[0][0].body).toStrictEqual({
        id: transitionStub.id,
        statusId: transitionStub.id,
      });
    });

    describe('when Status in response is undefined', () => {
      it('renders new transiton status', async () => {
        server.use(
          rest.post('/user/status', (req, res, ctx) => {
            requestSpy(req);
            return res(ctx.json({ ...responseStub, status: undefined }));
          }),
        );

        const store = createStore();
        render(
          <Provider store={store}>
            <StatusDropdown />
          </Provider>,
        );

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        store.dispatch(actions.getStatus() as any);

        await waitFor(() => {
          expect(screen.getByText(statusStub.text)).toBeInTheDocument();
        });

        userEvent.click(screen.getByText(statusStub.text));
        userEvent.click(screen.getByText(transitionStub.text));

        await waitFor(() => {
          expect(screen.getByRole('listbox')).toHaveTextContent(transitionStub.text);
        });
      });
    });

    describe('when set new status request failed', () => {
      it('renders current transiton status', async () => {
        server.use(
          rest.post('/user/status', (req, res, ctx) => {
            requestSpy(req);
            return res.once(ctx.status(500));
          }),
        );

        const store = createStore();
        render(
          <Provider store={store}>
            <StatusDropdown />
          </Provider>,
        );

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        store.dispatch(actions.getStatus() as any);

        await waitFor(() => {
          expect(screen.getByText(statusStub.text)).toBeInTheDocument();
        });

        userEvent.click(screen.getByText(statusStub.text));
        userEvent.click(screen.getByText(transitionStub.text));

        await waitFor(() => {
          expect(screen.getByRole('listbox')).toHaveTextContent(statusStub.text);
        });
      });
    });
  });
});
