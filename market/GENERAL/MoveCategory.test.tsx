import React from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DisplayCategory } from 'src/java/definitions';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { api } from 'src/test/singletons/apiSingleton';
import { MoveCategory } from './MoveCategory';

const categories: DisplayCategory[] = [
  {
    guruCategoryName: 'категория',
    parentHid: -1,
    name: 'максимально уникальное имя, которое больше нигде не встретися',
    hid: 124,
    tovarId: 111,
    published: true,
    guruCategoryId: 222,
    visual: true,
  },
];

describe('<MoveCategory />', () => {
  it('renders without errors', async () => {
    const onSave = jest.fn((parentHid?: number) => parentHid);
    renderWithProvider(<MoveCategory onSave={onSave} />);
    await act(async () => {
      api.categoryTreeController.getCategories.next().resolve(categories);
    });
    userEvent.click(screen.getByText('–'));
    userEvent.click(await screen.findByText(categories[0].name));

    userEvent.click(await screen.findByText('Переместить'));

    expect(onSave).toHaveLastReturnedWith(categories[0].hid);

    userEvent.click(await screen.findByText('Подвесить к корню'));
    expect(onSave).toHaveLastReturnedWith(90401);
  });
});
