import React from 'react';

import { emptySelection, SelectionContainer, selectionHandlers, updateSelection } from '../selection';

describe('selection', () => {
  function setup() {
    const state = {
      selection: emptySelection<number>(),
      items: [] as number[],
    };

    const selectionContainer: SelectionContainer<number> = updater =>
      (state.selection = updater(state.selection, state.items));

    const handlers = selectionHandlers(selectionContainer);

    return {
      ...handlers,
      sel: () => state.selection,
      items: (items: number[]) => {
        state.items = items;
        updateSelection(selectionContainer);
      },
    };
  }

  function eventWithValue(value: string): React.ChangeEvent<HTMLInputElement> {
    return {
      currentTarget: { value },
    } as any;
  }

  it('should select items on clicks', () => {
    const { items, sel, onItemClick } = setup();
    items([1, 2, 3, 4]);

    expect(sel().all).toBeFalsy();
    expect(sel().keys).toEqual([]);

    onItemClick(eventWithValue('1'));
    expect(sel().all).toBeFalsy();
    expect(sel().keys).toEqual([1]);
    expect(sel().selected[1]).toBeTruthy();
    expect(sel().selected[2]).toBeFalsy();

    onItemClick(eventWithValue('2'));
    expect(sel().all).toBeFalsy();
    expect(sel().keys).toEqual([1, 2]);
    expect(sel().selected[2]).toBeTruthy();

    onItemClick(eventWithValue('2'));
    expect(sel().all).toBeFalsy();
    expect(sel().keys).toEqual([1]);
    expect(sel().selected[2]).toBeFalsy();
  });

  it('should properly handle all click', () => {
    const { items, sel, onAllClick } = setup();

    items([1, 2, 3, 4]);
    expect(sel().keys).toEqual([]);
    expect(sel().all).toEqual(false);

    onAllClick();
    expect(sel().keys).toEqual([1, 2, 3, 4]);
    expect(sel().all).toEqual(true);

    onAllClick();
    expect(sel().keys).toEqual([]);
    expect(sel().all).toEqual(false);
  });

  it('should uncheck all whenever item selection is cleared', () => {
    const { items, sel, onAllClick, onItemClick } = setup();

    items([1, 2, 3, 4]);
    onAllClick();
    onItemClick(eventWithValue('2'));
    expect(sel().keys).toEqual([1, 3, 4]);
    expect(sel().all).toEqual(false);
  });

  it("shouldn't check all when all items are selected (by design, discussable)", () => {
    const { items, sel, onItemClick } = setup();
    items([1, 2]);

    onItemClick(eventWithValue('1'));
    onItemClick(eventWithValue('2'));

    expect(sel().keys).toEqual([1, 2]);
    expect(sel().all).toEqual(false);
  });

  it('should update items list when all is checked and items are changed', () => {
    const { items, sel, onAllClick } = setup();
    items([1, 2, 3, 4]);

    onAllClick();

    expect(sel().all).toBeTruthy();
    expect(sel().keys).toEqual([1, 2, 3, 4]);
    items([3, 4, 5, 6]);
    expect(sel().keys).toEqual([3, 4, 5, 6]);
    expect(sel().selected[1]).toBeFalsy();
    expect(sel().selected[5]).toBeTruthy();
  });
});
