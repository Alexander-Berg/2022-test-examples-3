import React from 'react';
import { render } from '@testing-library/react';

import { InheritButton } from './InheritButton';

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

describe('<InheritButton />', () => {
  it('renders without errors', () => {
    render(<InheritButton {...props} />);
  });
});
