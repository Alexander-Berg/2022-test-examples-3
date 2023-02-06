import React from 'react';
import { render, screen } from '@testing-library/react';

import { ListSelector } from './ListSelector';

describe('<ListSelector/>', () => {
  it('renders empty data without errors', () => {
    render(<ListSelector items={[]} selectedItems={[]} onChange={() => 1} />);
  });

  it('renders correct number of rows', () => {
    const items = [
      { name: 'testik', value: 1 },
      { name: 'testovich', value: '2' },
    ];

    render(<ListSelector items={items} selectedItems={['2']} onChange={() => 1} />);
    const resultTestik = screen.getByText('testik');
    const resultTestovich = screen.getByText('testovich');
    expect(resultTestik).not.toBeUndefined();
    expect(resultTestovich).not.toBeUndefined();
  });

  it('correct number of checked checkboxes', () => {
    const items = [
      { name: 'testik', value: 1 },
      { name: 'testovich', value: '2' },
    ];

    render(<ListSelector items={items} selectedItems={['2']} onChange={() => 1} />);

    const checkboxes = screen.getAllByRole('checkbox');
    const checkedCheckboxes = checkboxes.filter(chBox => chBox.attributes.getNamedItem('checked'));
    expect(checkedCheckboxes.length).toBe(1);
  });

  it('correct number of checked checkboxes for incorrect selected values array', () => {
    const items = [
      { name: 'testik', value: 1 },
      { name: 'testovich', value: '2' },
    ];

    render(<ListSelector items={items} selectedItems={[2]} onChange={() => 1} />);

    const checkboxes = screen.getAllByRole('checkbox');
    const checkedCheckboxes = checkboxes.filter(chBox => chBox.attributes.getNamedItem('checked'));
    expect(checkedCheckboxes.length).toBe(0);
  });
});
