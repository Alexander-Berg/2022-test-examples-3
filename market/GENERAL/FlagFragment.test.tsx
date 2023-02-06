import React from 'react';
import { render } from '@testing-library/react';

import { FlagFragment } from './FlagFragment';

const props: any = {
  input: {
    value: false,
    onChange: () => null,
    name: 'test',
    onBlur: () => null,
    onFocus: () => null,
  },
  meta: {},
};

describe('<FlagFragment />', () => {
  it('renders without errors', () => {
    render(<FlagFragment {...props} textContent="T" title="Test" />);
  });
});
