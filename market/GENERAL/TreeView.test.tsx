import React from 'react';
import { act, fireEvent, render } from '@testing-library/react';

import { TreeView } from './TreeView';
import { NodeValue } from './types';

const TREE_TEST_DATA: NodeValue[] = [
  {
    id: '0',
    name: 'Root',
    type: 'node',
    children: generateTreeNodes(['Testik', 'Testovich', 'Qwert', 'Qwertievich']),
  },
];

describe('<TreeView />', () => {
  it('check search filtering', () => {
    const app = render(<TreeView data={TREE_TEST_DATA} />);
    const searchInput = app.container.getElementsByTagName('input').item(0);
    expect(searchInput).toBeTruthy();

    act(() => {
      fireEvent.change(searchInput!, { target: { value: 'Test' } });
    });
    expect(app.queryAllByText('Qwert')).toHaveLength(0);
    expect(app.queryAllByText('Testik')).toHaveLength(1);
    expect(app.queryAllByText('Testovich')).toHaveLength(1);
    act(() => {
      fireEvent.change(searchInput!, { target: { value: '' } });
    });
    expect(app.queryAllByText('Qwert')).toHaveLength(1);
    expect(app.queryAllByText('Qwertievich')).toHaveLength(1);
    act(() => {
      fireEvent.change(searchInput!, { target: { value: 'Qwertievich' } });
    });
    expect(app.queryAllByText('Qwertievich')).toHaveLength(1);
    expect(app.queryAllByText('Qwert')).toHaveLength(0);
    expect(app.queryAllByText('Testik')).toHaveLength(0);
    expect(app.queryAllByText('Testovich')).toHaveLength(0);
  });
});

function generateTreeNodes(names: string[]): NodeValue[] {
  return names.map((name, index) => ({
    id: `${name}_${index}`,
    children: [],
    name,
    data: undefined,
    parent: undefined,
    type: 'node',
  }));
}
