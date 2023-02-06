import { ChangeOffStrategy } from './ChangeOffStrategy';
import { Tree } from '../../Tree';
import { createById, createCategory } from '../../../../../utils';

describe('ChangeOffStrategy', () => {
  let strategy: ChangeOffStrategy;
  let tree: Tree;
  const byId = createById([
    createCategory(0, [
      createCategory(1, [createCategory(3, [createCategory(4)])]),
      createCategory(2),
    ]),
  ]);

  beforeEach(() => {
    strategy = new ChangeOffStrategy();
    tree = new Tree(strategy);
    tree.setup({
      byId,
      root: [0],
      valueAsTree: {
        0: {
          1: {
            3: {
              4: {},
            },
          },
          2: {},
        },
      },
    });
  });

  describe('.applyForLeaf', () => {
    it('changes deep value', () => {
      strategy.applyForLeaf(1, [0, 1]);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
            2: {},
          },
        },
        initial: {
          0: {
            1: {
              3: {
                4: {},
              },
            },
            2: {},
          },
        },
      });
    });

    it('changes non-deep value', () => {
      strategy.applyForLeaf(2, [0, 2]);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
            1: {
              3: {
                4: {},
              },
            },
          },
        },
        initial: {
          0: {
            1: {
              3: {
                4: {},
              },
            },
            2: {},
          },
        },
      });
    });

    describe('when applied category value is false', () => {
      beforeEach(() => {
        tree.setup({
          byId,
          root: [0],
          valueAsTree: {
            0: {
              1: {},
              2: {},
            },
          },
        });
      });

      it(`doesn't do anything`, () => {
        strategy.applyForLeaf(3, [0, 1, 3]);

        expect(tree.valueAsTree).toEqual({
          current: {
            0: {
              1: {},
              2: {},
            },
          },
          initial: {
            0: {
              1: {},
              2: {},
            },
          },
        });
      });
    });
  });

  describe('.applyForBranch', () => {
    it('changes off whole branch', () => {
      strategy.applyForBranch(4, [0, 1, 3, 4]);

      expect(tree.valueAsTree).toEqual({
        current: {
          0: {
            2: {},
          },
        },
        initial: {
          0: {
            1: {
              3: {
                4: {},
              },
            },
            2: {},
          },
        },
      });
    });
  });
});
