import { renderHook } from '@testing-library/react-hooks';

import { useItemsFilter } from './useItemsFilter';
import { MenuItem } from '../type';

const TEST_ITEMS: MenuItem[] = [
  {
    id: 'operatorsRoot',
    label: 'Операторы',
    subMenu: [
      {
        id: '/gwt/#billingCustomActions',
        label: 'Нестандартные действия пользователей',
        href: '/gwt/#billingCustomActions',
        roles: ['ADMIN'],
      },
      {
        id: '/ui/roles',
        label: 'Роли пользователей в категориях',
        href: '/ui/roles',
        roles: ['ADMIN', 'OPERATOR'],
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
    } = renderHook(() => useItemsFilter(TEST_ITEMS, ['OPERATOR']), {});

    expect(current).toEqual([
      {
        id: 'operatorsRoot',
        label: 'Операторы',
        subMenu: [
          {
            id: '/ui/roles',
            label: 'Роли пользователей в категориях',
            href: '/ui/roles',
            roles: ['ADMIN', 'OPERATOR'],
          },
          { id: '/docs/index.xml', label: 'Документация', href: '/docs/index.xml' },
        ],
      },
    ]);
  });
  it('with ADMIN role', () => {
    const {
      result: { current },
    } = renderHook(() => useItemsFilter(TEST_ITEMS, ['ADMIN']), {});

    expect(current).toEqual([
      {
        id: 'operatorsRoot',
        label: 'Операторы',
        subMenu: [
          {
            id: '/gwt/#billingCustomActions',
            label: 'Нестандартные действия пользователей',
            href: '/gwt/#billingCustomActions',
            roles: ['ADMIN'],
          },
          {
            id: '/ui/roles',
            label: 'Роли пользователей в категориях',
            href: '/ui/roles',
            roles: ['ADMIN', 'OPERATOR'],
          },
          { id: '/docs/index.xml', label: 'Документация', href: '/docs/index.xml' },
        ],
      },
    ]);
  });
});
