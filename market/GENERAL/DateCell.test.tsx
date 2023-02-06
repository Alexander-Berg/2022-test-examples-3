import React from 'react';
import { screen } from '@testing-library/react';

import { renderTableCell } from 'src/test/setupTestProvider';
import { DateCell } from '.';

describe('<DateCell />', () => {
  it('should be shown default date format', () => {
    renderTableCell(<DateCell value="2020-12-28T12:34:56" />);

    expect(screen.getByText(/28 декабря 2020/i)).toBeInTheDocument();
  });

  it('should be shown custom date formatt', () => {
    renderTableCell(<DateCell value="2020-12-28T12:34:56" dateFormat="dd.MM.yyyy" />);

    expect(screen.getByText(/28\.12\.2020/)).toBeInTheDocument();
  });

  it('should be shown value as is if invalid date passed', () => {
    jest.spyOn(console, 'error').mockImplementation(() => null);

    renderTableCell(<DateCell value="Invalid date" />);

    expect(screen.getByText(/Invalid date/i)).toBeInTheDocument();
  });

  it('should be call children fn', () => {
    const childrenFn = jest.fn();

    renderTableCell(
      <DateCell value="2020-12-28T12:34:56" dateFormat="dd/MM/yyyy">
        {childrenFn}
      </DateCell>
    );

    expect(childrenFn).toBeCalledTimes(1);
    expect(childrenFn).toBeCalledWith('28/12/2020');
  });
});
