import React from 'react';
import { render } from '@testing-library/react';

import { SingleAlias } from './SingleAlias';
import { Language } from 'src/java/definitions';

describe('<SingleAlias />', () => {
  it('renders without errors', () => {
    render(<SingleAlias value={{ word: '', morphological: false, language: Language.RUSSIAN }} onChange={() => 1} />);
  });
});
