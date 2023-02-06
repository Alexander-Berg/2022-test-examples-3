import { ChangeOnStrategy } from './ChangeOnStrategy';
import { Tree } from '../../Tree';
import { createById, createCategory } from '../../../../../utils';

describe('ChangeOnStrategy', () => {
  let strategy: ChangeOnStrategy;
  let tree: Tree;
  const byId = createById([
    createCategory(0, [createCategory(1, [createCategory(3)]), createCategory(2)]),
  ]);

  beforeEach(() => {
    strategy = new ChangeOnStrategy();
    tree = new Tree(strategy);
    tree.setup({
      byId,
      root: [0],
      valueAsTree: {
        0: {},
      },
    });
  });

  it('changes deep value', () => {
    strategy.applyForLeaf(3, [0, 1, 3]);

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
  });

  it('changes non-deep value', () => {
    strategy.applyForLeaf(2, [0, 2]);

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
            1: {
              3: {},
            },
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
