import { ById } from 'modules/categorization2/types';
import { Tree } from './Tree';
import { OneBranchStrategy } from './ChangeStrategies';
import { createById, createCategory } from '../../../utils';
import { Category } from '../Category';

describe('Tree', () => {
  let tree = new Tree();

  beforeEach(() => {
    tree = new Tree();
  });

  describe('.setup', () => {
    it('sets external data', () => {
      const byId = {};
      const root = [];
      const highlightPath = [];
      const valueAsTree = {};

      tree.setup({
        byId,
        root,
        highlightPath,
        valueAsTree,
      });

      expect(tree.byId).toEqual(byId);
      expect(tree.root).toEqual(root);
      expect(tree.highlightPath).toEqual(highlightPath);
      expect(tree.valueAsTree.current).toEqual(valueAsTree);
      expect(tree.valueAsTree.initial).toEqual(valueAsTree);
    });
  });

  describe('.setChangeStrategy', () => {
    it('sets change strategy', () => {
      const strategy = new OneBranchStrategy();
      const mockSetTree = jest.fn();
      strategy.setTree = mockSetTree;

      tree.setChangeStrategy(strategy);

      expect(mockSetTree).toBeCalledTimes(1);
      expect(mockSetTree).toBeCalledWith(tree);
    });
  });

  describe('.changeLeafValue', () => {
    describe('sets .valueAsTree', () => {
      beforeEach(() => {
        tree.byId = createById([
          createCategory(1, [
            createCategory(2),
            createCategory(3, [
              createCategory(4, [
                createCategory(5, [createCategory(6)]),
                createCategory(7),
                createCategory(8),
              ]),
            ]),
          ]),
        ]);
        tree.root = [1];
      });

      test('case #1', () => {
        tree.changeLeafValue(2, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            2: {},
          },
        });
      });

      test('case #2', () => {
        tree.changeLeafValue(4, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {},
            },
          },
        });
      });

      test('case #3', () => {
        tree.changeLeafValue(6, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {
                5: {
                  6: {},
                },
              },
            },
          },
        });
      });

      test('case #4', () => {
        tree.valueAsTree.current = {
          1: {
            3: {
              4: {
                5: {
                  6: {},
                },
              },
            },
          },
        };

        tree.changeLeafValue(4, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {},
          },
        });
      });

      test('case #5', () => {
        tree.valueAsTree.current = {
          1: {
            3: {
              4: {
                5: {
                  6: {},
                },
                7: {
                  8: {},
                },
              },
            },
          },
        };

        tree.changeLeafValue(4, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {},
          },
        });
      });

      test('case #6', () => {
        tree.valueAsTree.current = {
          1: {
            3: {
              4: {
                5: {
                  6: {},
                },
                7: {
                  8: {},
                },
              },
            },
          },
        };

        tree.changeLeafValue(5, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {
                7: {
                  8: {},
                },
              },
            },
          },
        });
      });
    });
  });

  describe('.changeBranchValue', () => {
    describe('sets .valueAsTree', () => {
      let byId: ById;
      let root: number[];
      beforeEach(() => {
        byId = createById([
          createCategory(1, [
            createCategory(2),
            createCategory(3, [
              createCategory(4, [
                createCategory(5, [createCategory(6)]),
                createCategory(7),
                createCategory(8),
              ]),
              createCategory(9),
            ]),
          ]),
        ]);
        root = [1];
      });

      test('case #1', () => {
        tree.setup({
          valueAsTree: {},
          byId,
          root,
        });

        tree.changeBranchValue(2, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            2: {},
          },
        });
      });

      test('case #2', () => {
        tree.setup({
          valueAsTree: {},
          byId,
          root,
        });

        tree.changeBranchValue(4, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {},
            },
          },
        });
      });

      test('case #3', () => {
        tree.setup({
          valueAsTree: {},
          byId,
          root,
        });

        tree.changeBranchValue(6, true);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {
                5: {
                  6: {},
                },
              },
            },
          },
        });
      });

      test('case #4', () => {
        tree.setup({
          valueAsTree: {
            1: {
              3: {
                4: {
                  5: {
                    6: {},
                  },
                },
              },
            },
          },
          byId,
          root,
        });

        tree.changeBranchValue(6, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {},
        });
      });

      test('case #5', async () => {
        tree.setup({
          valueAsTree: {
            1: {
              3: {
                4: {
                  5: {
                    6: {},
                  },
                  7: {
                    8: {},
                  },
                },
              },
            },
          },
          byId,
          root,
        });

        tree.changeBranchValue(6, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {
                7: {
                  8: {},
                },
              },
            },
          },
        });
      });

      test('case #6', () => {
        tree.setup({
          valueAsTree: {
            1: {
              3: {
                4: {
                  5: {
                    6: {},
                  },
                  7: {
                    8: {},
                  },
                },
              },
            },
          },
          byId,
          root,
        });

        tree.changeBranchValue(5, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              4: {
                7: {
                  8: {},
                },
              },
            },
          },
        });
      });

      test('case #7', () => {
        tree.setup({
          valueAsTree: {
            1: {
              3: {
                4: {
                  5: {
                    6: {},
                  },
                  7: {
                    8: {},
                  },
                },
              },
            },
          },
          byId,
          root,
        });

        tree.changeBranchValue(4, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {},
        });
      });

      test('case #8', () => {
        tree.setup({
          valueAsTree: {
            1: {
              3: {
                4: {
                  5: {
                    6: {},
                  },
                },
                9: {},
              },
            },
          },
          byId,
          root,
        });

        tree.changeBranchValue(6, false);

        expect(tree.valueAsTree.current).toEqual({
          1: {
            3: {
              9: {},
            },
          },
        });
      });
    });
  });

  describe('.reset', () => {
    it('resets all data', () => {
      tree.setup({
        highlightPath: [1, 2, 3],
        valueAsTree: {
          1: {},
        },
        byId: {
          1: new Category({
            id: 1,
            name: '1',
            isLeaf: true,
          }),
        },
        root: [1, 2, 3],
      });

      tree.reset();

      expect(tree.highlightPath).toHaveLength(0);
      expect(tree.valueAsTree.current).toStrictEqual({});
      expect(tree.valueAsTree.initial).toStrictEqual({});
      expect(tree.byId).toStrictEqual({});
      expect(tree.root).toHaveLength(0);
    });
  });
});
