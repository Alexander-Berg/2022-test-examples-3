import { renderHook } from '@testing-library/react-hooks';

import { HeaderItem, UserRoles } from './types';
import { useItemsFilter } from './useItemsFilter';

const TEST_ITEMS: HeaderItem[] = [
  {
    id: 'operatorsRoot',
    label: 'Операторы',
    subMenu: [
      {
        id: '/gwt/#billingCustomActions',
        label: 'Нестандартные действия пользователей',
        href: '/gwt/#billingCustomActions',
        roles: [UserRoles.ADMIN],
      },
      {
        id: '/ui/roles',
        label: 'Роли пользователей в категориях',
        href: '/ui/roles',
        roles: [UserRoles.ADMIN, UserRoles.OPERATOR],
      },
      {
        id: '/docs/index.xml',
        label: 'Документация',
        href: '/docs/index.xml',
      },
    ],
  },
];

describe('useItemsFilter', () => {
  it('with empty roles', () => {
    const {
      result: { current },
    } = renderHook(() => useItemsFilter(TEST_ITEMS, []), {});

    expect(current).toEqual([
      {
        id: 'operatorsRoot',
        label: 'Операторы',
        subMenu: [{ id: '/docs/index.xml', label: 'Документация', href: '/docs/index.xml' }],
      },
    ]);
  });
  it('with OPERATOR role', () => {
    const {
      result: { current },
    } = renderHook(() => useItemsFilter(TEST_ITEMS, [UserRoles.OPERATOR]), {});

    expect(current).toEqual([
      {
        id: 'operatorsRoot',
        label: 'Операторы',
        subMenu: [
          {
            id: '/ui/roles',
            label: 'Роли пользователей в категориях',
            href: '/ui/roles',
            roles: ['visualAdmin', 'operator'],
          },
          { id: '/docs/index.xml', label: 'Документация', href: '/docs/index.xml' },
        ],
      },
    ]);
  });
  it('with ADMIN role', () => {
    const {
      result: { current },
    } = renderHook(() => useItemsFilter(TEST_ITEMS, [UserRoles.ADMIN]), {});

    expect(current).toEqual([
      {
        id: 'operatorsRoot',
        label: 'Операторы',
        subMenu: [
          {
            id: '/gwt/#billingCustomActions',
            label: 'Нестандартные действия пользователей',
            href: '/gwt/#billingCustomActions',
            roles: ['visualAdmin'],
          },
          {
            id: '/ui/roles',
            label: 'Роли пользователей в категориях',
            href: '/ui/roles',
            roles: ['visualAdmin', 'operator'],
          },
          { id: '/docs/index.xml', label: 'Документация', href: '/docs/index.xml' },
        ],
      },
    ]);
  });
});
