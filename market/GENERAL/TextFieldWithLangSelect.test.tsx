import React from 'react';
import { render } from '@testing-library/react';

import { TextFieldWithLangSelect } from './TextFieldWithLangSelect';
import { DEFAULT_NAME } from '../../../Parameters.constants';

describe('<TextFieldWithLangSelect />', () => {
  it('renders without errors', () => {
    render(<TextFieldWithLangSelect value={DEFAULT_NAME} onChange={() => null} />);
  });
});
