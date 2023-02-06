import React from 'react';
import { render } from '@testing-library/react';

import { AutoRuleHeader } from './AutoRuleHeader';

describe('AutoRuleHeader', () => {
  test('render', () => {
    const app = render(<AutoRuleHeader />);
    app.getByText(/Автоправила/);
  });
});
