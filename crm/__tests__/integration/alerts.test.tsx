import React from 'react';
import { waitFor, render, screen, getByRole, cleanup } from '@testing-library/react/pure';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import userEvent from '@testing-library/user-event';
import { get } from 'api/common';
import { AlertDTO } from 'types/dto/AlertDTO';
import { User } from 'types/entities/user';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { createById, createCategory } from '../../utils';
import { AlertLoadHandler } from '../../types';

const createAlertDTO = (categoryId: number): AlertDTO => ({
  id: categoryId,
  category: {
    id: categoryId,
    name: categoryId.toString(),
  },
  name: `Name ${categoryId}`,
  description: `Description ${categoryId}`,
  modifiedOn: new Date().toISOString(),
  modifiedBy: {} as User,
  expiredOn: new Date().toISOString(),
});
const server = setupServer(
  rest.get('/alerts', (_req, res, ctx) => {
    return res(ctx.json({}));
  }),
);

describe('alerts', () => {
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(2)])]),
    createCategory(4),
  ]);
  const root = [0, 4];

  beforeAll(() => {
    HTMLElement.prototype.scrollIntoView = jest.fn();
    server.listen();
    server.resetHandlers();
  });

  afterAll(() => {
    server.close();
  });

  describe('alert showing behavior', () => {
    const handleAlertsLoad: AlertLoadHandler = (categoryIds) => {
      return get({
        url: '/alerts',
        data: {
          categoryIds,
        },
      }).then((response: { alerts: AlertDTO[] }) => response.alerts);
    };

    beforeAll(() => {
      const store = new Store();
      const handleLoad = () =>
        Promise.resolve({
          byId,
          root,
          highlightPath: [],
          valueAsTree: {
            0: {},
            4: {},
          },
        });

      render(
        <Provider store={store}>
          <Widget
            targetMeta={{ id: 1, type: 'Mail' }}
            onLoad={handleLoad}
            onAlertsLoad={handleAlertsLoad}
          />
          <Modal />
        </Provider>,
      );

      const openButton = screen.getByRole('button', { name: 'Разметить' });
      userEvent.click(openButton);

      return waitFor(() => {
        screen.getByRole('dialog');
        screen.getByRole('treegrid');
      });
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    afterAll(() => {
      cleanup();
    });

    describe('root category #0', () => {
      it('shows alert for category #0', async () => {
        server.use(
          rest.get('/alerts', (req, res, ctx) =>
            res(
              ctx.json({
                alerts: [createAlertDTO(0)],
              }),
            ),
          ),
        );

        userEvent.click(screen.getByRole('treeitem', { name: '0' }));

        await waitFor(() => {
          const titleNode = getByRole(screen.getByRole('alert'), 'heading');
          expect(titleNode).toHaveTextContent('Name 0');
        });
      });

      it('has expanded state by default', async () => {
        const descriptionNode = getByRole(screen.getByRole('alert'), 'article');
        expect(descriptionNode).toHaveTextContent('Description 0');
      });

      it(`doesn't show "related bugs" button`, () => {
        const relatedBugsNode = screen.queryByText('Есть связанный баги!');
        expect(relatedBugsNode).not.toBeInTheDocument();
      });
    });

    describe('category #1', () => {
      it('shows alert for category #0', async () => {
        server.use(
          rest.get('/alerts', (req, res, ctx) =>
            res(
              ctx.json({
                alerts: [createAlertDTO(0)],
              }),
            ),
          ),
        );

        userEvent.click(screen.getByRole('treeitem', { name: '1' }));

        await waitFor(() => {
          const titleNode = getByRole(screen.getByRole('alert'), 'heading');
          expect(titleNode).toHaveTextContent('Name 0');
        });
      });

      it('continues to show description', () => {
        const descriptionNode = getByRole(screen.getByRole('alert'), 'article');
        expect(descriptionNode).toHaveTextContent('Description 0');
      });
    });

    describe('category #2', () => {
      it('shows alert for category #2', async () => {
        server.use(
          rest.get('/alerts', (req, res, ctx) =>
            res(
              ctx.json({
                alerts: [createAlertDTO(0), createAlertDTO(2)],
              }),
            ),
          ),
        );

        userEvent.click(screen.getByRole('treeitem', { name: '2' }));

        await waitFor(() => {
          const titleNode = getByRole(screen.getByRole('alert'), 'heading');
          expect(titleNode).toHaveTextContent('Name 2');
        });
      });

      it('continues to show description, but changes content', () => {
        const descriptionNode = getByRole(screen.getByRole('alert'), 'article');
        expect(descriptionNode).toHaveTextContent('Description 2');
      });

      it('shows "related bugs" button', () => {
        const relatedBugsNode = screen.getByText('Есть связанные баги!');
        expect(relatedBugsNode).toBeInTheDocument();
      });
    });

    describe('on "related bugs" button click', () => {
      it('shows modal with related bugs', async () => {
        const viewRelatedBugsButton = screen.getByRole('button', { name: 'Посмотреть' });
        userEvent.click(viewRelatedBugsButton);

        await waitFor(() => {
          const modal = screen.getByRole('dialog', { name: 'related bugs modal' });
          expect(modal).toBeInTheDocument();
        });

        const relatedBugsNodes = screen.getAllByRole('listitem');
        expect(relatedBugsNodes).toHaveLength(2);

        const modal = screen.getByRole('dialog', { name: 'related bugs modal' });
        const closeButton = getByRole(modal, 'button');
        userEvent.click(closeButton);
      });
    });

    describe('category #4', () => {
      it(`doesn't show any alert`, async () => {
        server.use(
          rest.get('/alerts', (req, res, ctx) =>
            res(
              ctx.status(500),
              ctx.json({
                message: 'Error',
              }),
            ),
          ),
        );

        userEvent.click(screen.getByRole('treeitem', { name: '4' }));

        await waitFor(() => {
          const alertNode = screen.queryByRole('alert');
          expect(alertNode).not.toBeInTheDocument();
        });
      });
    });

    describe('category #0', () => {
      it('saves expanded state of description after alert disappearing', async () => {
        server.use(
          rest.get('/alerts', (req, res, ctx) =>
            res(
              ctx.json({
                alerts: [createAlertDTO(0)],
              }),
            ),
          ),
        );

        userEvent.click(screen.getByRole('treeitem', { name: '0' }));

        await waitFor(() => {
          const descriptionNode = getByRole(screen.getByRole('alert'), 'article');
          expect(descriptionNode).toHaveTextContent('Description 0');
        });
      });
    });
  });
});
