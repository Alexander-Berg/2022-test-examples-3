import React from 'react';
import {
  waitFor,
  render,
  screen,
  fireEvent,
  getByRole,
  cleanup,
} from '@testing-library/react/pure';
import { Store, Tree, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { createById, createCategory } from '../../utils';

describe('tree behavior', () => {
  const byId = createById([
    createCategory(0, [
      createCategory(1, [
        createCategory(2),
        createCategory(3),
        createCategory(4, [createCategory(5)]),
      ]),
    ]),
    createCategory(6, [createCategory(7)]),
  ]);
  const root = [0, 6];

  beforeAll(() => {
    HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  describe('multi-branch strategy', () => {
    describe('highlights categories', () => {
      beforeAll(() => {
        const handleLoad = () =>
          Promise.resolve({
            byId,
            root,
            highlightPath: [],
            valueAsTree: {
              0: {},
              6: {},
            },
          });
        const store = new Store();

        render(
          <Provider store={store}>
            <Widget
              targetMeta={{ id: 1, type: 'Mail' }}
              changeStrategy="multi-branch"
              onLoad={handleLoad}
            />
            <Modal />
          </Provider>,
        );

        const openButton = screen.getByRole('button', { name: 'Разметить' });
        fireEvent.click(openButton);

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

      describe('root category #1', () => {
        it('highlights root category', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
          const highlighted = screen.getAllByRole('treeitem', { selected: true });
          expect(highlighted[0]).toHaveTextContent('0');
        });

        it('highlights depth=1 category', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '1' }));
          const highlighted = screen.getAllByRole('treeitem', { selected: true });
          expect(highlighted).toHaveLength(2);
          expect(highlighted[1]).toHaveTextContent('1');
        });

        describe('highlights depth=2 category', () => {
          test('case #1', () => {
            fireEvent.click(screen.getByRole('treeitem', { name: '2' }));
            const highlighted = screen.getAllByRole('treeitem', { selected: true });
            expect(highlighted).toHaveLength(3);
            expect(highlighted[2]).toHaveTextContent('2');
          });

          test('case #2', () => {
            fireEvent.click(screen.getByRole('treeitem', { name: '3' }));
            const highlighted = screen.getAllByRole('treeitem', { selected: true });
            expect(highlighted).toHaveLength(3);
            expect(highlighted[2]).toHaveTextContent('3');
          });

          test('case #3', () => {
            fireEvent.click(screen.getByRole('treeitem', { name: '4' }));
            const highlighted = screen.getAllByRole('treeitem', { selected: true });
            expect(highlighted).toHaveLength(3);
            expect(highlighted[2]).toHaveTextContent('4');
          });
        });

        it('highlights depth=3 category', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '5' }));
          const highlighted = screen.getAllByRole('treeitem', { selected: true });
          expect(highlighted).toHaveLength(4);
          expect(highlighted[3]).toHaveTextContent('5');
        });
      });

      describe('root category #2', () => {
        it('highlights root category and resets other ones', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '6' }));
          const highlighted = screen.getAllByRole('treeitem', { selected: true });
          expect(highlighted).toHaveLength(1);
          expect(highlighted[0]).toHaveTextContent('6');
        });

        it('highlights depth=1 category', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '7' }));
          const highlighted = screen.getAllByRole('treeitem', { selected: true });
          expect(highlighted).toHaveLength(2);
          expect(highlighted[1]).toHaveTextContent('7');
        });
      });
    });

    describe('change category values', () => {
      let tree: Tree;
      beforeAll(() => {
        const handleLoad = () =>
          Promise.resolve({
            byId,
            root,
            highlightPath: [],
            valueAsTree: {
              0: {},
              6: {},
            },
          });
        const store = new Store();
        tree = store.tree as Tree;

        render(
          <Provider store={store}>
            <Widget
              targetMeta={{ id: 1, type: 'Mail' }}
              changeStrategy="multi-branch"
              onLoad={handleLoad}
            />
            <Modal />
          </Provider>,
        );

        const openButton = screen.getByRole('button', { name: 'Разметить' });
        fireEvent.click(openButton);

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

      describe('root category #1', () => {
        it('sets depth=2 checkboxes properly', async () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
          fireEvent.click(screen.getByRole('treeitem', { name: '1' }));
          const category2Checkbox = getByRole(
            screen.getByRole('treeitem', { name: '2' }),
            'checkbox',
          );
          fireEvent.click(category2Checkbox);

          await waitFor(() => {
            expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(2);
            expect(tree.finiteSelected).toHaveLength(1);
          });
        });

        it('sets depth=3 checkboxes properly', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '4' }));
          const category5Checkbox = getByRole(
            screen.getByRole('treeitem', { name: '5' }),
            'checkbox',
          );
          fireEvent.click(category5Checkbox);

          expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(4);
          expect(tree.finiteSelected).toHaveLength(2);
        });
      });

      describe('root category #6', () => {
        it('sets depth=1 checkboxes properly', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '6' }));
          let category7Checkbox = getByRole(
            screen.getByRole('treeitem', { name: '7' }),
            'checkbox',
          );
          fireEvent.click(category7Checkbox);

          // Другие колонки не закрепляются в DOM
          expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(1);
          expect(tree.finiteSelected).toHaveLength(3);
        });
      });

      describe('resets checkboxes', () => {
        it('resets category #1 checkboxes', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
          const category1Checkbox = getByRole(
            screen.getByRole('treeitem', { name: '1' }),
            'checkbox',
          );
          fireEvent.click(category1Checkbox);

          expect(screen.queryAllByRole('checkbox', { checked: true })).toHaveLength(0);
          expect(tree.finiteSelected).toHaveLength(1);
        });

        it('resets category #7 checkboxes', () => {
          fireEvent.click(screen.getByRole('treeitem', { name: '6' }));
          const category7Checkbox = getByRole(
            screen.getByRole('treeitem', { name: '7' }),
            'checkbox',
          );
          fireEvent.click(category7Checkbox);

          expect(screen.queryAllByRole('checkbox', { checked: true })).toHaveLength(0);
          expect(tree.finiteSelected).toHaveLength(0);
        });
      });
    });
  });

  describe('one-branch strategy', () => {
    let tree: Tree;
    beforeAll(() => {
      const handleLoad = () =>
        Promise.resolve({
          byId,
          root,
          highlightPath: [],
          valueAsTree: {
            0: {},
            6: {},
          },
        });
      const store = new Store();
      tree = store.tree as Tree;

      render(
        <Provider store={store}>
          <Widget
            targetMeta={{ id: 1, type: 'Mail' }}
            changeStrategy="one-branch"
            onLoad={handleLoad}
          />
          <Modal />
        </Provider>,
      );

      const openButton = screen.getByRole('button', { name: 'Разметить' });
      fireEvent.click(openButton);

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

    it('sets depth=2 checkboxes properly', async () => {
      fireEvent.click(screen.getByRole('treeitem', { name: '0' }));
      fireEvent.click(screen.getByRole('treeitem', { name: '1' }));
      const category2Checkbox = getByRole(screen.getByRole('treeitem', { name: '2' }), 'checkbox');
      fireEvent.click(category2Checkbox);

      await waitFor(() => {
        expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(2);
        expect(tree.finiteSelected).toHaveLength(1);
      });
    });

    it('sets depth=3 checkboxes properly', () => {
      fireEvent.click(screen.getByRole('treeitem', { name: '4' }));
      const category5Checkbox = getByRole(screen.getByRole('treeitem', { name: '5' }), 'checkbox');
      fireEvent.click(category5Checkbox);

      expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(3);
      expect(tree.finiteSelected).toHaveLength(1);
    });

    it('sets depth=1 checkboxes properly', () => {
      fireEvent.click(screen.getByRole('treeitem', { name: '6' }));
      let category7Checkbox = getByRole(screen.getByRole('treeitem', { name: '7' }), 'checkbox');
      fireEvent.click(category7Checkbox);

      expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(1);
      expect(tree.finiteSelected).toHaveLength(1);
    });
  });
});
