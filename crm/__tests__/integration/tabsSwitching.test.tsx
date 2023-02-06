import React from 'react';
import { waitFor, render, screen, fireEvent } from '@testing-library/react';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { createById, createCategory } from '../../utils';

describe('tabs switching', () => {
  describe('when on "all selected" tab and some categories are selected', () => {
    let store: Store;
    const byId = createById([
      createCategory(0, [createCategory(1, [createCategory(2)])]),
      createCategory(3),
    ]);
    const root = [0, 3];
    const handleLoad = () =>
      Promise.resolve({
        byId,
        root,
        highlightPath: [],
        valueAsTree: {
          0: {
            1: {
              2: {},
            },
          },
          3: {},
        },
      });

    beforeEach(() => {
      store = new Store();

      HTMLElement.prototype.scrollIntoView = jest.fn();
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('switches to tree tab when selected categories are gone', async () => {
      render(
        <Provider store={store}>
          <Widget targetMeta={{ id: 1, type: 'Mail' }} onLoad={handleLoad} />
          <Modal />
        </Provider>,
      );

      fireEvent.click(screen.getByRole('button', { name: 'Разметить' }));

      await waitFor(() => {
        screen.getByRole('list');
        screen.getByRole('listitem');
        screen.getByText('0');
        screen.getByText('1');
        screen.getByText('2');
        expect(screen.getAllByRole('separator')).toHaveLength(2);
        screen.getByRole('button', { name: 'Выбранные категории (1)' });
      });

      fireEvent.click(screen.getAllByRole('checkbox')[0]);

      screen.getByText('Выбранные категории (0)');

      await waitFor(() => {
        screen.getByRole('treegrid');
        screen.getByRole('treeitem', { name: '3' });
      });
    });
  });
});
