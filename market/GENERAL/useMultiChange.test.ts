import { createStore } from '@reatom/core';
import { renderHook } from '@testing-library/react-hooks';

import { ButtonActionType } from 'src/components';
import { MboRole, UserCategoryRoles } from 'src/java/definitions';
import { useMultiChange } from 'src/pages/Roles/hooks/useMultiChange';
import { RolesActions, RolesAtom } from 'src/pages/Roles/store/atoms/RolesAtom';
import { getEmptyRole } from 'src/pages/Roles/utils';
import { getProviderWrapper } from 'src/test/utils/ProviderWrapper';

const EMPTY_ROLES = [getEmptyRole(1, 1), getEmptyRole(2, 1), getEmptyRole(3, 1)];
const ROLES_WITH_SUPER: UserCategoryRoles[] = [
  {
    categoryId: 1,
    projects: [],
    roles: [MboRole.OPERATOR, MboRole.SUPER],
    userId: 1,
  },
  {
    categoryId: 2,
    projects: [],
    roles: [MboRole.OPERATOR, MboRole.SUPER],
    userId: 1,
  },
  {
    categoryId: 3,
    projects: [],
    roles: [MboRole.OPERATOR, MboRole.SUPER],
    userId: 1,
  },
];

describe('useMultiChange', () => {
  it('works empty', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const callback = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore();
    const providerWrapper = getProviderWrapper({ reatomStore });
    reatomStore.dispatch(RolesActions.setRoles([]));

    const view = renderHook(() => useMultiChange(callback), { wrapper: providerWrapper });

    const result = view.result.current;

    result('', ButtonActionType.ADD);

    expect(lastRoles).toEqual([]);
  });
  it('works with add', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const callback = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore(RolesAtom);
    const providerWrapper = getProviderWrapper({ reatomStore });

    reatomStore.dispatch(RolesActions.setRoles(EMPTY_ROLES));

    const view = renderHook(() => useMultiChange(callback), { wrapper: providerWrapper });

    const result = view.result.current;

    result(MboRole.SUPER, ButtonActionType.ADD);

    expect(lastRoles).toEqual(ROLES_WITH_SUPER);
  });
  it('works with remove', () => {
    let lastRoles: UserCategoryRoles[] | undefined;
    const callback = (roles: UserCategoryRoles[]) => {
      lastRoles = roles;
    };
    const reatomStore = createStore(RolesAtom);
    const providerWrapper = getProviderWrapper({ reatomStore });

    reatomStore.dispatch(RolesActions.setRoles(ROLES_WITH_SUPER));

    const view = renderHook(() => useMultiChange(callback), { wrapper: providerWrapper });

    const result = view.result.current;

    result(MboRole.SUPER, ButtonActionType.REMOVE);

    expect(lastRoles).toEqual(EMPTY_ROLES);
  });
});
