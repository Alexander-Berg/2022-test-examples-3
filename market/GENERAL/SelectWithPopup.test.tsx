import React from 'react';
import { render } from '@testing-library/react';

import { SelectWithPopup } from './SelectWithPopup';

describe('<SelectWithPopup />', () => {
  it('renders data without errors', () => {
    render(<SelectWithPopup label=" " value={1} options={[{ value: 1, label: '' }]} onChange={() => 1} />);
  });

  it('renders empty data without errors', () => {
    render(<SelectWithPopup label=" " value={undefined} options={[]} onChange={() => 1} />);
  });
});
