import React from 'react';
import { render } from '@testing-library/react';

import { FieldInfo } from './Form.types';
import { Form } from './Form';

const testConfig: FieldInfo<TestFormProps>[] = [
  {
    name: 'isExist',
    label: '',
    inputRenderer: () => <></>,
  },
];

interface TestFormProps {
  isExist?: boolean;
}

describe('<Form />', () => {
  it('renders without errors', () => {
    render(<Form config={testConfig} data={{ isExist: false }} onChange={() => null} />);
  });
});
