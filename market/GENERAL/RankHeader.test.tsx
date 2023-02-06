import React from 'react';
import { render } from '@testing-library/react';

import { RankHeader } from './RankHeader';

describe('RankHeader', () => {
  test('render', () => {
    const app = render(<RankHeader />);
    app.getByText(/Приоритет/);
  });
});
