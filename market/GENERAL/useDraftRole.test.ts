import { createStore } from '@reatom/core';
import { renderHook } from '@testing-library/react-hooks';
import { act } from 'react-test-renderer';

import { MboRole, TaskType } from 'src/java/definitions';
import { getProviderWrapper } from 'src/test/utils/ProviderWrapper';
import { useDraftRole } from 'src/pages/Roles/hooks/useDraftRole';
import { DraftRolesActions } from 'src/pages/Roles/store/atoms/DraftRolesAtom';
import { SavingQueueActions } from 'src/pages/Roles/store/atoms/SavingQueueActions';

describe('useDraftRole', () => {
  const providerWrapper = getProviderWrapper({});

  it('inits', () => {
    const view = renderHook(() => useDraftRole(321, 123, 1), { wrapper: providerWrapper });

    const result = view.result.current;

    expect(result.draftRole).toEqual({
      categoryId: 321,
      projects: [],
      roles: ['OPERATOR'],
      userId: 123,
    });
    expect(result.isSaving).toBeUndefined();
    expect(result.checkedIds).toEqual([]);
    expect(result.toggleOption).toBeDefined();
    expect(result.onCategoryChange).toBeDefined();
    expect(result.onUserChange).toBeDefined();
  });
  it('toggleOption works with roles', () => {
    const view = renderHook(() => useDraftRole(321, 123, 1), { wrapper: providerWrapper });

    expect(view.result.current.checkedIds).toEqual([]);

    act(() => {
      view.result.current.toggleOption(MboRole.SUPER);
    });

    expect(view.result.current.checkedIds).toEqual([MboRole.SUPER]);

    act(() => {
      view.result.current.toggleOption(MboRole.SUPER);
    });

    expect(view.result.current.checkedIds).toEqual([]);
  });
  it('toggleOption works with projects', () => {
    const view = renderHook(() => useDraftRole(321, 123, 1), { wrapper: providerWrapper });

    expect(view.result.current.checkedIds).toEqual([]);

    act(() => {
      view.result.current.toggleOption(TaskType.BLUE_LOGS);
    });

    expect(view.result.current.checkedIds).toEqual([TaskType.BLUE_LOGS]);

    act(() => {
      view.result.current.toggleOption(TaskType.BLUE_LOGS);
    });
    expect(view.result.current.checkedIds).toEqual([]);
  });
  it('trigger save', () => {
    const reatomStore = createStore();
    let lastSet: any;
    let lastAdd: any;

    reatomStore.subscribe(DraftRolesActions.setRole, data => {
      lastSet = data;
    });
    reatomStore.subscribe(SavingQueueActions.add, data => {
      lastAdd = data;
    });
    const saveWrapper = getProviderWrapper({ reatomStore });
    const view = renderHook(() => useDraftRole(321, 123, 1), { wrapper: saveWrapper });

    act(() => {
      view.result.current.toggleOption(MboRole.SUPER);
    });

    expect(lastSet).toEqual({
      categoryId: 321,
      projects: [],
      roles: ['OPERATOR', 'SUPER'],
      userId: 123,
    });
    expect(lastAdd).toEqual([
      {
        categoryId: 321,
        projects: [],
        roles: ['OPERATOR', 'SUPER'],
        userId: 123,
      },
    ]);
  });
});
