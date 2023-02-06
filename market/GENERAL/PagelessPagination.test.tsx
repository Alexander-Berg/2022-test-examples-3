import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { PagelessPagination } from './PagelessPagination';

describe('<PagelessPagination />', () => {
  it('renders 0 rows without', async () => {
    const app = render(
      <PagelessPagination
        itemsPerPageOptions={[1, 50, 100]}
        rowsNumber={0}
        itemsPerPage={20}
        currentPage={0}
        onPageChanged={() => 1}
        onItemsPerPageChanged={() => 1}
      />
    );

    expect(app.getByText('0 - 0')).toBeTruthy();

    const prevButton = await app.findByTitle('Предыдущая страница');
    expect(prevButton.hasAttribute('disabled')).toBeTruthy();
    const nextButton = await app.findByTitle('Следующая страница');
    expect(nextButton.hasAttribute('disabled')).toBeTruthy();
  });

  it('calls callbacks', async () => {
    const onPageChanged = jest.fn(page => page);

    const app = render(
      <PagelessPagination
        itemsPerPageOptions={[1, 50, 100]}
        rowsNumber={21}
        itemsPerPage={20}
        currentPage={1}
        onPageChanged={onPageChanged}
        onItemsPerPageChanged={() => 1}
      />
    );

    const nextButton = await app.findByTitle('Следующая страница');
    fireEvent.click(nextButton);
    expect(onPageChanged).toBeCalledTimes(1);
    expect(onPageChanged).toHaveLastReturnedWith(2);
  });
});
