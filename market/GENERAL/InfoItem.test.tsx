import React from 'react';
import { render } from '@testing-library/react';

import { InfoItem } from './InfoItem';
import { Link } from 'src/components';
import { LinkProps } from 'src/components/Link/Link';

describe('<InfoItem />', () => {
  it('renders without errors', () => {
    render(<InfoItem title="Hello world" />);
  });

  it('renders with tooltip', () => {
    render(<InfoItem title="Hello world" />);
  });

  it('renders with link', () => {
    render(<InfoItem title={(<Link href="/">Hello world</Link>) as LinkProps} />);
  });
});
