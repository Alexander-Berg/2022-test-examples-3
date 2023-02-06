import React from 'react';
import { render } from '@testing-library/react';

import { SizeChartMeasureDto, SizeDto, SizeMeasureDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SizeTableCell } from './SizeTableCell';

const testSize: SizeDto = {
  category_ids: [123, 456],
  measures: [{ measure_id: 1 }, { measure_id: 2 }] as SizeChartMeasureDto[],
  size_name: 'test',
} as SizeDto;

describe('<SizeTableCell />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SizeTableCell size={testSize} measure={{} as SizeMeasureDto} />
      </Provider>
    );
  });
});
