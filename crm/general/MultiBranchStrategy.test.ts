import { MultiBranchStrategy } from './MultiBranchStrategy';
import { Tree } from '../../Tree';
import { createById, createCategory } from '../../../../../utils';

describe('MultiBranchStrategy', () => {
  let strategy: MultiBranchStrategy;
  let tree: Tree;
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(3)]), createCategory(2)]),
  ]);

  beforeEach(() => {
    strategy = new MultiBranchStrategy();
    tree = new Tree(strategy);
    tree.setup({
      byId,
      root: [0],
      valueAsTree: {
        0: {},
      },
    });
  });

  describe('.applyForLeaf', () => {
    it('allows to change on multiple categories', () => {
      strategy.applyForLeaf(3, [0, 1, 3], true);
      strategy.applyForLeaf(2, [0, 2], true);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
            1: {
              3: {},
            },
            2: {},
          },
        },
        initial: {
          0: {},
        },
      });
    });

    describe('when applied category value is true', () => {
      beforeEach(() => {
        tree.setup({
          byId,
          root: [0],
          valueAsTree: {
            0: {
              1: {
                3: {},
              },
              2: {},
            },
          },
        });
      });

      it('changes off multiple categories', () => {
        strategy.applyForLeaf(1, [0, 1], false);
        strategy.applyForLeaf(2, [0, 2], false);

        expect(tree.valueAsTree).toEqual({
          current: {
            0: {},
          },
          initial: {
            0: {
              1: {
                3: {},
              },
              2: {},
            },
          },
        });
      });
    });
  });

  describe('.applyForBranch', () => {
    beforeEach(() => {
      tree.setup({
        byId,
        root: [0],
        valueAsTree: {
          0: {
            1: {
              3: {},
            },
            2: {},
          },
        },
      });
    });

    describe('when value is false', () => {
      it('changes off whole branch', () => {
        strategy.applyForBranch(3, [0, 1, 3], false);

        expect(tree.valueAsTree).toEqual({
          current: {
            0: {
              2: {},
            },
          },
          initial: {
            0: {
              1: {
                3: {},
              },
              2: {},
            },
          },
        });
      });
    });
  });
});
