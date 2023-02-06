import React from 'react';
import { render } from '@testing-library/react';

import { LinkedParam } from 'src/java/definitions';
import { ParameterLink } from './ParameterLink';

describe('<ParameterLink />', () => {
  it('renders without errors', () => {
    render(<ParameterLink value={{} as LinkedParam} onChange={jest.fn()} />);
  });
});
