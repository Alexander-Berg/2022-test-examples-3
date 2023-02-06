import React from 'react';
import { render } from '@testing-library/react';

import { PossibleValues } from './PossibleValues';

describe('<PossibleValues />', () => {
  test('render with values', () => {
    const app = render(<PossibleValues possibleValues={['satake']} />);
    app.getByText('satake');
  });

  test('render without values', () => {
    const app = render(<PossibleValues title="Empty values" possibleValues={[]} />);
    expect(app.queryByTitle('Empty values')).not.toBeInTheDocument();
  });
});
