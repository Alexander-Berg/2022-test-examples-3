import React from 'react';

import { setupWithReatom } from '../../../../test/withReatom';
import { ImportResultDetails } from './ImportResultDetails';
import { ImportResult } from '../../../../java/definitions';

describe('<ImportResultDetails />', () => {
  it('renders without errors', () => {
    const { app } = setupWithReatom(<ImportResultDetails />);
    expect(app.queryByText('Товары с ошибками')).toBeNull();
    expect(app.queryByText('Незагруженные товары')).toBeNull();
    expect(app.queryByText('Проигнорированные товары')).toBeNull();
  });

  it('renders details', () => {
    const importResult: ImportResult = {
      duplicateSkus: [],
      failedModelCount: 1,
      failedModelSamples: ['123456787654323456765432123456789'],
      notModifiedCount: 2,
      notModifiedSamples: undefined,
      skusNotFoundCount: 22,
      skusNotFoundSamples: ['qwe', 'rty'],
    };
    const { app } = setupWithReatom(<ImportResultDetails {...importResult} />);
    expect(app.queryByText('123456787654323456765432123456789')).not.toBeNull();
    expect(app.queryByText('qwe, rty')).not.toBeNull();
  });
});
