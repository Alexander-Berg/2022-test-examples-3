import React from 'react';
import { waitFor, render, screen, fireEvent, getByRole } from '@testing-library/react';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { ById } from '../../types';
import { createById, createCategory } from '../../utils';

describe('search', () => {
  let byId: ById;
  let root: number[];

  beforeEach(() => {
    byId = createById([
      createCategory(0, [createCategory(1), createCategory(2), createCategory(3)]),
    ]);
    root = [0];

    HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows search results', async () => {
    const handleLoad = () =>
      Promise.resolve({
        byId,
        root,
        highlightPath: [],
        valueAsTree: {
          0: {},
        },
      });
    const store = new Store();
    const mockSearch = jest.fn(() =>
      Promise.resolve({
        resultIds: [1, 2, 3],
        highlightRangesById: {},
      }),
    );

    render(
      <Provider store={store}>
        <Widget targetMeta={{ id: 1, type: 'Mail' }} onSearch={mockSearch} onLoad={handleLoad} />
        <Modal />
      </Provider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Разметить' }));

    await waitFor(() => {
      screen.getByRole('dialog');
      screen.getByRole('treegrid');
    });

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'search text' } });

    await waitFor(() => {
      expect(screen.getAllByRole('listitem')).toHaveLength(3);
    });

    fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'button'));

    await waitFor(() => {
      screen.getByRole('treegrid');
    });

    const highlightedCategories = screen.getAllByRole('treeitem', { selected: true });
    expect(highlightedCategories).toHaveLength(2);
    expect(highlightedCategories[1]).toHaveTextContent('1');
  });
});
