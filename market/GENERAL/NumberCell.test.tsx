import React from 'react';
import { screen } from '@testing-library/react';

import { renderTableCell } from 'src/test/setupTestProvider';
import { NumberCell } from '.';

describe('<NumberCell />', () => {
  it('should be shown default number format', () => {
    renderTableCell(<NumberCell value={123.4567} maximumFractionDigits={3} />);

    expect(screen.getByText(/^123,457$/)).toBeInTheDocument();
  });

  it('should be shown without fraction digits', () => {
    renderTableCell(<NumberCell value={123.4567} maximumFractionDigits={0} />);

    expect(screen.getByText(/^123$/)).toBeInTheDocument();
  });

  it('should be shown with custom delimiter', () => {
    renderTableCell(<NumberCell value={123.4567} delimiter="." />);

    expect(screen.getByText(/123\.46/)).toBeInTheDocument();
  });

  it('should be call children fn', () => {
    const childrenFn = jest.fn();

    renderTableCell(
      <NumberCell value={100} minimumFractionDigits={2}>
        {childrenFn}
      </NumberCell>
    );

    expect(childrenFn).toBeCalledTimes(1);
    expect(childrenFn).toBeCalledWith('100,00');
  });
});
