import { TreeRootAdapter } from './TreeRootAdapter';

const createStructureNode = (id: string, items?: string[]) => {
  return {
    eof: true,
    disabled: true,
    position: 1,
    id,
    counter: 1,
    items,
  };
};

export const createDataNode = (id: string, nestingLevel: number) => {
  return {
    id,
    name: id,
    color: id,
    counter: id,
    nestingLevel,
    disabled: false,
    isLeaf: false,
  };
};

describe('TreeRootAdapter', () => {
  it('creates items list in proper order', () => {
    const treeRootAdapter = new TreeRootAdapter(
      {
        0: createStructureNode('0', ['1', '2']),
        2: createStructureNode('2', ['102', '101']),
      },
      {
        0: createDataNode('0', 0),
        1: createDataNode('1', 1),
        2: createDataNode('2', 1),
        102: createDataNode('102', 2),
        101: createDataNode('101', 2),
      },
    );

    expect(treeRootAdapter.items).toHaveLength(2);
    const id2Node = treeRootAdapter.items.find((item) => item.id === '2')!;
    expect(id2Node.items).toHaveLength(2);
    expect(id2Node.items![0].id).toBe('102');
    expect(id2Node.items![1].id).toBe('101');
  });
});
