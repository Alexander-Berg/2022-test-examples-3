import { OneBranchStrategy } from './OneBranchStrategy';
import { Tree } from '../../Tree';
import { createById, createCategory } from '../../../../../utils';

describe('OneBranchStrategy', () => {
  let strategy: OneBranchStrategy;
  let tree: Tree;
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(3)]), createCategory(2)]),
  ]);

  beforeEach(() => {
    strategy = new OneBranchStrategy();
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
    it('changes on applied category and changes off another ones', () => {
      strategy.applyForLeaf(3, [0, 1, 3], true);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
            1: {
              3: {},
            },
          },
        },
        initial: {
          0: {},
        },
      });

      strategy.applyForLeaf(2, [0, 2], true);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
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
            },
          },
        });
      });

      it('changes off category', () => {
        strategy.applyForLeaf(1, [0, 1], false);

        expect(tree.valueAsTree).toEqual({
          current: {
            0: {},
          },
          initial: {
            0: {
              1: {
                3: {},
              },
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
