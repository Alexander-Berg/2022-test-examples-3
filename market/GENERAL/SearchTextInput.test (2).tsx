import React from 'react';
import { render } from '@testing-library/react';

import { SearchTextInput } from './SearchTextInput';

describe('<SearchTextInput />', () => {
  it('renders without errors', () => {
    render(<SearchTextInput options={[]} onChange={() => null} />);
  });
});
