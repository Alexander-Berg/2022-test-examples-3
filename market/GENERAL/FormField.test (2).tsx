import React from 'react';
import { render } from '@testing-library/react';

import { Link } from 'src/components';
import { LinkProps } from 'src/components/Link/Link';
import { FormField } from './AuditFormField';

describe('<FormField />', () => {
  it('renders without errors', () => {
    render(<FormField label="Test label" />);
  });

  it('renders with link', () => {
    render(<FormField label={(<Link href="/">Test label</Link>) as LinkProps} />);
  });
});
