import React from 'react';
import { render } from '@testing-library/react';

import { SizeMeasureDto } from 'src/java/definitions';
import { MeasureCell } from './MeasureCell';

describe('<MeasureCell />', () => {
  it('renders without errors', () => {
    render(<MeasureCell measure={{ name: 'test' } as SizeMeasureDto} onDelete={jest.fn()} />);
  });
});
