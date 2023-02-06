import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';

import { OrderedList } from './OrderedList';

describe('<OrderedList />', () => {
  it('renders without errors', () => {
    render(<OrderedList options={[]} onChange={() => 1} />);
  });

  it('renders', () => {
    const options = [
      {
        value: 1,
        label: 'Opt 1',
      },
      {
        value: 3,
        label: 'Opciya 3',
      },
      {
        value: 9876543456789876543,
        label: 'Testik Testovich Testinberg',
      },
    ];
    const onChange = jest.fn(res => res);
    render(<OrderedList options={options} onChange={onChange} />);

    const deleteButton = screen.queryAllByTitle('Удалить');
    expect(deleteButton[2]).toBeTruthy();
    fireEvent.click(deleteButton[2]!);
    expect(onChange).toHaveLastReturnedWith([
      {
        value: 1,
        label: 'Opt 1',
      },
      {
        value: 3,
        label: 'Opciya 3',
      },
    ]);
  });
});
