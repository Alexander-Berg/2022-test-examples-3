import React from 'react';
import {
  waitFor,
  render,
  screen,
  fireEvent,
  getByRole,
  cleanup,
} from '@testing-library/react/pure';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { createById, createCategory } from '../../utils';

describe('saving', () => {
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(2)])]),
    createCategory(3),
  ]);
  const root = [0, 3];

  beforeAll(() => {
    HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  describe('saves selected categories', () => {
    let store: Store;
    const mockSave = jest.fn();
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
    beforeAll(() => {
      store = new Store();

      render(
        <Provider store={store}>
          <Widget targetMeta={{ id: 1, type: 'Mail' }} onSave={mockSave} onLoad={handleLoad} />
          <Modal />
        </Provider>,
      );
    });

    afterAll(() => {
      cleanup();
    });

    it('opens with 2 available categories', async () => {
      const openButton = screen.getByRole('button', { name: 'Разметить' });
      fireEvent.click(openButton);

      await waitFor(() => {
        screen.getByRole('dialog');
        screen.getByRole('treegrid');
        expect(screen.getAllByRole('treeitem')).toHaveLength(2);
        expect(screen.getByRole('button', { name: 'Сохранить' })).toBeDisabled();
      });
    });

    it('enables save button after category change', () => {
      fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
      fireEvent.click(screen.getByRole('treeitem', { name: '1' }));
      const category2Checkbox = getByRole(screen.getByRole('treeitem', { name: '2' }), 'checkbox');
      fireEvent.click(category2Checkbox);

      const saveButton = screen.getByRole('button', { name: 'Сохранить' });
      expect(saveButton).toBeEnabled();
    });

    it('hides modal after click on save button', async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }));

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('calls onSave', () => {
      expect(mockSave).toBeCalledTimes(1);
      expect(mockSave).toBeCalledWith(expect.arrayContaining([{ id: 2 }]));
    });
  });
});
