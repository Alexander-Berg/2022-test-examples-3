import { ClearStrategy } from './ClearStrategy';
import { Tree } from '../../Tree';
import { createById, createCategory } from '../../../../../utils';

describe('ClearStrategy', () => {
  let strategy: ClearStrategy;
  let tree: Tree;
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(3)]), createCategory(2)]),
    createCategory(4),
  ]);

  beforeEach(() => {
    strategy = new ClearStrategy();
    tree = new Tree(strategy);
    tree.setup({
      byId,
      root: [0, 4],
      valueAsTree: {
        0: {
          1: {
            3: {},
          },
          2: {},
        },
        4: {},
      },
    });
  });

  it('clears the tree', () => {
    strategy.applyForLeaf();

    expect(tree.valueAsTree).toEqual({
      current: {
        0: {},
        4: {},
      },
      initial: {
        0: {
          1: {
            3: {},
          },
          2: {},
        },
        4: {},
      },
    });
  });

  describe('when tree is already clear', () => {
    beforeEach(() => {
      tree.setup({
        byId,
        root: [0, 4],
        valueAsTree: {
          0: {},
          4: {},
        },
      });
    });

    it(`doesn't do anything`, () => {
      strategy.applyForLeaf();

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {},
          4: {},
        },
        initial: {
          0: {},
          4: {},
        },
      });
    });
  });
});
