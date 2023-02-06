import React from 'react';
import { render, screen } from '@testing-library/react';

import { CategoryHistoricalDataDto, CategoryHistoricalDataType } from 'src/java/definitions';
import { ChangesHistory } from './ChangesHistory';

describe('<ChangesHistory/>', () => {
  it('renders without errors', () => {
    const value: CategoryHistoricalDataDto = {
      historicalHid: 123,
      modificationDate: '01.01.2001',
      comment: '',
      empty: false,
      type: CategoryHistoricalDataType.SELECTION,
    };
    render(<ChangesHistory value={value} onChange={() => 1} />);
  });

  it('renders invalid date', () => {
    const value: CategoryHistoricalDataDto = {
      historicalHid: 123,
      modificationDate: '1.1.2001',
      comment: '',
      empty: false,
      type: CategoryHistoricalDataType.SELECTION,
    };

    render(<ChangesHistory value={value} onChange={() => 1} />);

    expect(screen.getByDisplayValue('01.01.2001')).not.toBeNull();
  });
});
