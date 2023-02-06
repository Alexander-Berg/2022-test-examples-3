import React from 'react';
import { render, screen } from '@testing-library/react';

import { KeyValueTable } from './KeyValueTable';

describe('<KeyValueTable />', () => {
  it('renders without errors', () => {
    const jsonObject = {
      array: [1, 2, 3],
      str: 'Testik',
      numb: 123,
      bool: false,
      smth: undefined,
      nullable: null,
    };

    const filter = (_: string, v: any) => typeof v !== 'number' || v !== 123;
    render(<KeyValueTable obj={jsonObject} filter={filter} />);

    expect(screen.getByText('str')).toBeTruthy();
    expect(screen.getByText('Testik')).toBeTruthy();
    expect(screen.queryByText('numb')).toBeFalsy();
  });
});
