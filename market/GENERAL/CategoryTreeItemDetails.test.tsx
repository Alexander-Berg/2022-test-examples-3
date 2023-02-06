import React from 'react';

import { categoryInfo } from 'src/test/data';
import { CategoryTreeItemDetails } from './CategoryTreeItemDetails';
import { setupWithReatom } from 'src/test/withReatom';

const renderActions = () => {
  return <button type="button">Нажми на кнопку</button>;
};

describe('<CategoryTreeItemDetails />', () => {
  test('Show info about no good content category', async () => {
    const { app } = setupWithReatom(
      <CategoryTreeItemDetails category={{ ...categoryInfo, acceptGoodContent: false }} />
    );

    await app.findByText(/В этой категории нельзя самостоятельно создавать карточки/);
  });

  test('Shop addition info about category', async () => {
    const { app } = setupWithReatom(<CategoryTreeItemDetails category={{ ...categoryInfo }} />);

    app.getByText(new RegExp(categoryInfo.inCategory.substring(0, 40), 'i'));
    app.getByText(new RegExp(categoryInfo.outOfCategory.substring(0, 20), 'i'));
  });

  test('Test render props', async () => {
    const { app } = setupWithReatom(
      <CategoryTreeItemDetails category={{ ...categoryInfo }} renderActions={renderActions} />
    );

    app.getByText(/Нажми на кнопку/);
  });
});
