import treeConvertor from '../treeConvertor';

describe('tree convertor', () => {
  test('empty', () => {
    expect(treeConvertor({ func: () => {} })).toEqual(null);
  });

  test('test', () => {
    const input = {
      type: 'Root',
      text: 'Root',
      items: [
        {
          type: 'Group',
          text: 'Group',
        },
      ],
    };

    const output = {
      component: 'Root',
      text: 'Root',
      content: [
        {
          component: 'Group',
          text: 'Group',
        },
      ],
    };

    const convertor = node => {
      const result = { ...node };
      delete result.items;
      delete result.type;
      result.component = node.type;
      return result;
    };

    expect(treeConvertor({ node: input, func: convertor, newChildrenSlug: 'content' })).toEqual(
      output,
    );
  });
});
