import React from 'react';
import { render } from '@testing-library/react';

import { SizeChartMeasureDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { SizeMeasureEditor } from './SizeMeasureEditor';

describe('<SizeMeasureEditor />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <SizeMeasureEditor data={{} as SizeChartMeasureDto} sizeName="test" onChange={jest.fn()} onDelete={jest.fn()} />
      </Provider>
    );
  });
});
