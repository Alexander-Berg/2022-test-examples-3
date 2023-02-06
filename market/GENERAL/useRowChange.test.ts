import { createStore } from '@reatom/core';
import { renderHook } from '@testing-library/react-hooks';

import { MboRole, UserCategoryRoles } from 'src/java/definitions';
import { useRowChange } from 'src/pages/Roles/hooks/useRowChange';
import { RolesActions, RolesAtom } from 'src/pages/Roles/store/atoms/RolesAtom';
import { getEmptyRole } from 'src/pages/Roles/utils';
import { getProviderWrapper } from 'src/test/utils/ProviderWrapper';

describe('useRowChange', () => {
  it('works empty', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const onChange = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore();
    const providerWrapper = getProviderWrapper({ reatomStore });
    reatomStore.dispatch(RolesActions.setRoles([]));

    const view = renderHook(() => useRowChange({ onChange, onDraftChange: jest.fn() }), {
      wrapper: providerWrapper,
    });

    const result = view.result.current;

    result('', '');

    expect(lastRoles).toBeUndefined();
  });
  it('works with draft', () => {
    let lastChangedOption: string | undefined;
    const onChange = (optionId: string) => {
      lastChangedOption = optionId;
    };
    const providerWrapper = getProviderWrapper({});
    const view = renderHook(() => useRowChange({ onChange: jest.fn(), onDraftChange: onChange }), {
      wrapper: providerWrapper,
    });

    const result = view.result.current;

    result('draft', 'test');

    expect(lastChangedOption).toEqual('test');
  });
  it('works with userId', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const onChange = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore(RolesAtom);
    const providerWrapper = getProviderWrapper({ reatomStore });
    reatomStore.dispatch(RolesActions.setRoles([getEmptyRole(1, 1), getEmptyRole(2, 1), getEmptyRole(3, 1)]));

    const view = renderHook(() => useRowChange({ onChange, onDraftChange: jest.fn(), userId: 1 }), {
      wrapper: providerWrapper,
    });

    const result = view.result.current;

    result('2', MboRole.SUPER);

    expect(lastRoles?.find(r => r.categoryId === 2)).toEqual({
      categoryId: 2,
      projects: [],
      roles: ['OPERATOR', 'SUPER'],
      userId: 1,
    });
  });
  it('works with categoryId', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const onChange = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore(RolesAtom);
    const providerWrapper = getProviderWrapper({ reatomStore });
    reatomStore.dispatch(RolesActions.setRoles([getEmptyRole(1, 1), getEmptyRole(2, 1), getEmptyRole(3, 1)]));

    const view = renderHook(() => useRowChange({ onChange, onDraftChange: jest.fn(), categoryId: 1 }), {
      wrapper: providerWrapper,
    });

    const result = view.result.current;

    result('1', MboRole.SUPER);

    expect(lastRoles?.find(r => r.categoryId === 1)).toEqual({
      categoryId: 1,
      projects: [],
      roles: ['OPERATOR', 'SUPER'],
      userId: 1,
    });
  });
});
