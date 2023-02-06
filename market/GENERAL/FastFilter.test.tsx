import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Table, TableBody } from '@yandex-market/mbo-components';

import { FastFilter as FastFilterType } from 'src/java/definitions';
import { FastFilter } from './FastFilter';

const onChange = jest.fn();
const onDelete = jest.fn();
const fastFilter = {
  name: 'foo',
  is_published: true,
} as FastFilterType;

describe('<FastFilter />', () => {
  it('should delete row', () => {
    render(
      <Table size="m">
        <TableBody>
          <FastFilter onChange={onChange} onDelete={onDelete} data={fastFilter} />
        </TableBody>
      </Table>
    );

    userEvent.click(screen.getByTitle('удалить'));

    expect(onDelete).toBeCalledTimes(1);
  });
});
