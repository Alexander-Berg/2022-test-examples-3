import React from 'react';
import {
  waitFor,
  render,
  screen,
  fireEvent,
  getByRole,
  cleanup,
} from '@testing-library/react/pure';
import { delay } from 'bluebird';
import { Subject, Subscription } from 'rxjs';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { EmittingPayload, TargetMeta } from '../../types';
import { createById, createCategory } from '../../utils';

describe('emitting', () => {
  const byId = createById([
    createCategory(0, [createCategory(1), createCategory(2)]),
    createCategory(3),
  ]);
  const root = [0, 3];

  beforeAll(() => {
    HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  describe('emits events', () => {
    let store: Store;
    const mockSave = jest.fn();
    const mockSearch = jest.fn(() =>
      Promise.resolve({
        resultIds: [1, 2],
        highlightRangesById: {},
      }),
    );
    const mockTipLoad = jest.fn((id: number) => {
      if (id === 0) {
        return Promise.resolve('html document');
      }
      return Promise.resolve('');
    });
    const emitter = new Subject<{
      event: string;
      payload: TargetMeta & EmittingPayload;
    }>();
    const mockEmitSub = jest.fn();
    let sub: Subscription;

    beforeAll(() => {
      const handleLoad = () =>
        Promise.resolve({
          byId,
          root,
          highlightPath: [],
          valueAsTree: {
            0: {},
            3: {},
          },
        });
      store = new Store();

      render(
        <Provider store={store}>
          <Widget
            targetMeta={{ id: 1, type: 'Mail' }}
            onSave={mockSave}
            onLoad={handleLoad}
            onSearch={mockSearch}
            onTipLoad={mockTipLoad}
            emitter={emitter}
          />
          <Modal />
        </Provider>,
      );
    });

    afterAll(() => {
      cleanup();
    });

    beforeEach(() => {
      sub = emitter.subscribe(mockEmitSub);
    });

    afterEach(() => {
      sub.unsubscribe();
      jest.clearAllMocks();
    });

    it('emits "open" event', async () => {
      const openButton = screen.getByRole('button', { name: 'Разметить' });
      fireEvent.click(openButton);

      await waitFor(() => {
        screen.getByRole('dialog');
        screen.getByRole('treegrid');
        expect(screen.getAllByRole('treeitem')).toHaveLength(2);
        expect(screen.getByRole('button', { name: 'Сохранить' })).toBeDisabled();
      });

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'open',
          payload: {
            id: 1,
            type: 'Mail',
          },
        }),
      );
    });

    it('emits "tipLoadSuccess" event on category #0', async () => {
      fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
      await delay(0); // обработчик загрузки подсказки - микротаск, выполняем его

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'tipLoadSuccess',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 0,
            block: 'tree',
            hasTip: true,
          },
        }),
      );
    });

    it('emits "tipExpand" event on category #0', async () => {
      fireEvent.click(getByRole(screen.getByRole('note'), 'button', { name: 'expand' }));

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'tipExpand',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 0,
            block: 'categorization',
          },
        }),
      );
    });

    it('emits "categoryTurnOn" event on category #1', () => {
      fireEvent.click(getByRole(screen.getByRole('treeitem', { name: '1' }), 'checkbox'));

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'categoryTurnOn',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 1,
            block: 'tree',
            resultsCount: 2,
            order: 1,
          },
        }),
      );
    });

    it('emits "categoryTurnOn" event on category #2', () => {
      fireEvent.click(getByRole(screen.getByRole('treeitem', { name: '2' }), 'checkbox'));

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'categoryTurnOn',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 2,
            block: 'tree',
            resultsCount: 2,
            order: 2,
          },
        }),
      );
    });

    it('emits "categoryTurnOff" event on category #1', () => {
      fireEvent.click(getByRole(screen.getByRole('treeitem', { name: '1' }), 'checkbox'));

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'categoryTurnOff',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 1,
            block: 'tree',
            resultsCount: 2,
            order: 1,
          },
        }),
      );
    });

    it('emits "categoryTurnOff" event on category #2', () => {
      fireEvent.click(getByRole(screen.getByRole('treeitem', { name: '2' }), 'checkbox'));

      expect(mockEmitSub).toBeCalledWith(
        expect.objectContaining({
          event: 'categoryTurnOff',
          payload: {
            id: 1,
            type: 'Mail',
            categoryId: 2,
            block: 'tree',
            resultsCount: 2,
            order: 2,
          },
        }),
      );
    });

    describe('when searching', () => {
      it('emits "search" event', async () => {
        fireEvent.change(screen.getByRole('textbox'), { target: { value: 'search text' } });

        await waitFor(() => {
          expect(mockEmitSub).toBeCalledWith(
            expect.objectContaining({
              event: 'search',
              payload: {
                id: 1,
                type: 'Mail',
                resultsCount: 2,
                text: 'search text',
              },
            }),
          );
        });
      });

      it('emits "categoryTurnOn" event on category #1', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOn',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 1,
              block: 'search',
              resultsCount: 2,
              order: 1,
            },
          }),
        );
      });

      it('emits "categoryTurnOn" event on category #2', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[1], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOn',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 2,
              block: 'search',
              resultsCount: 2,
              order: 2,
            },
          }),
        );
      });

      it('emits "categoryTurnOff" event on category #1', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOff',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 1,
              block: 'search',
              resultsCount: 2,
              order: 1,
            },
          }),
        );
      });

      it('emits "categoryTurnOff" event on category #2', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[1], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOff',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 2,
              block: 'search',
              resultsCount: 2,
              order: 2,
            },
          }),
        );
      });
    });

    describe('"all selected" tab', () => {
      beforeAll(() => {
        store.tree.changeLeafValue(1, true);
        store.tree.changeBranchValue(2, true);
        store.tabs.go('selected');
      });

      it('emits "categoryTurnOff" event on category #1', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOff',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 1,
              block: 'selected',
              resultsCount: 2,
              order: 1,
            },
          }),
        );
      });

      it('emits "categoryTurnOff" event on category #2', () => {
        fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));

        expect(mockEmitSub).toBeCalledWith(
          expect.objectContaining({
            event: 'categoryTurnOff',
            payload: {
              id: 1,
              type: 'Mail',
              categoryId: 2,
              block: 'selected',
              resultsCount: 1,
              order: 1,
            },
          }),
        );
      });
    });
  });
});
