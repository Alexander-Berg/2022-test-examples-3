import filterTree from '../filterTree';

describe('filter tree map', () => {
  test('simple', () => {
    const input = {
      text: 'hello',
    };

    expect(filterTree({ node: input, predicate: node => node.text === 'hello' })).toEqual(input);
  });

  test('tree', () => {
    const input = {
      text: 'text',
      content: [
        {
          text: 'hello',
        },
      ],
    };

    expect(filterTree({ node: input, predicate: node => node.text === 'hello' })).toEqual(input);
  });

  test('tree', () => {
    const input = {
      text: 'text',
      content: [
        {
          text: 'hello',
          content: [
            {
              text: 'hello1',
            },
            {
              text: 'hello2',
            },
          ],
        },
      ],
    };

    expect(filterTree({ node: input, predicate: node => node.text === 'hello' })).toEqual(input);
  });

  test('tree', () => {
    const input = {
      text: 'text',
      content: [
        {
          text: 'hello',
          content: [
            {
              text: 'hello1',
            },
            {
              text: 'hello2',
            },
          ],
        },
      ],
    };

    const output = {
      text: 'text',
      content: [
        {
          text: 'hello',
          content: [
            {
              text: 'hello1',
            },
          ],
        },
      ],
    };

    expect(filterTree({ node: input, predicate: node => node.text === 'hello1' })).toEqual(output);
  });

  test('tree', () => {
    const input = {
      text: 'text',
      content: [
        {
          text: 'hello',
          content: [
            {
              text: 'hello1',
            },
            {
              text: 'hello1',
            },
          ],
        },
      ],
    };

    expect(filterTree({ node: input, predicate: node => node.text === 'hello1' })).toEqual(input);
  });
});
