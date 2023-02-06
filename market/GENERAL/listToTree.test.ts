import * as R from 'ramda';

import { listToTree, ListToTreeConfig } from './listToTree';

describe('listToTree', () => {
  it('should return an empty array if an empty array was passed to input', () => {
    expect(listToTree([])).toEqual([]);
  });

  it('should return an empty array when input list with some random objects', () => {
    const randomObjectsList = [{ asd: 'aasd' }, { qwe: 123 }];
    expect(listToTree(randomObjectsList)).toEqual([]);
  });

  it('should create tree without config with objects having id/parent_id', () => {
    const dataSet = [
      {
        id: 1,
        Phone: '(403) 125-2552',
        City: 'Coevorden',
        Name: 'Grady',
      },
      {
        id: 2,
        parent_id: 1,
        Phone: '(979) 486-1932',
        City: 'Chełm',
        Name: 'Scarlet',
      },
    ];

    const expectedDataTree = [
      {
        id: 1,
        Phone: '(403) 125-2552',
        City: 'Coevorden',
        Name: 'Grady',
        children: [
          {
            id: 2,
            parent_id: 1,
            Phone: '(979) 486-1932',
            City: 'Chełm',
            Name: 'Scarlet',
            children: [],
          },
        ],
      },
    ];

    expect(listToTree(dataSet)).toEqual(expectedDataTree);
  });

  it('should create tree from any array with custom config', () => {
    const inputList = [
      { asd: 'aasd', name: 'Mike' },
      { qwe: 123, name: 'Jack', daddy: 'Mike' },
    ];

    const expectedDataTree = [
      {
        name: 'Mike',
        data: { asd: 'aasd', name: 'Mike' },
        children: [
          {
            name: 'Jack',
            data: { qwe: 123, name: 'Jack', daddy: 'Mike' },
            children: [],
          },
        ],
      },
    ];

    const config: ListToTreeConfig<any> = {
      getId: R.prop('name'),
      getParentId: R.prop('daddy'),
      getNode: (origNode, id) => {
        return {
          name: id,
          data: origNode,
        };
      },
    };
    expect(listToTree(inputList, config)).toEqual(expectedDataTree);
  });
});
