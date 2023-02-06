import React from 'react';
import { render } from '@testing-library/react';

import { SizeChartDto } from 'src/java/definitions';
import { setupTestProvider, TestProviderType } from 'src/test/setupTestProvider';
import { ChartEditor } from './ChartEditor';

describe('<ChartEditor />', () => {
  let Provider: TestProviderType;

  beforeEach(() => {
    Provider = setupTestProvider();
  });

  it('renders without errors', () => {
    render(
      <Provider>
        <ChartEditor data={{} as SizeChartDto} onChange={jest.fn()} onCancel={jest.fn()} />
      </Provider>
    );
  });
});
