import React from 'react';
import { act, render, screen } from '@testing-library/react';

import { api } from 'src/test/singletons/apiSingleton';
import { LinkedNavigationNodes } from 'src/widgets';
import { treeMap } from './constants';

const linkedCategory = {
  nodeId: 79959,
  treeId: 80369,
  treeName: 'Навигационное дерево для Синего Маркета',
  nodeName: 'Компьютерная техника',
};

const listData = {
  data: {
    items: [linkedCategory],
    limit: 1,
    offset: 0,
    total: 1,
  },
};

describe('<LinkedNavigationNodes />', () => {
  test('render', async () => {
    render(<LinkedNavigationNodes hid={555} />);

    expect((api as any).linkedNavigationController.getLinkedNodeByHid.activeRequests()).toHaveLength(1);

    await act(async () => {
      await api.linkedNavigationController.getLinkedNodeByHid.next().resolve(listData);
    });

    await screen.findByText(treeMap[linkedCategory.treeId]?.name);
    await screen.findByText(new RegExp(linkedCategory.nodeName));
  });
});
