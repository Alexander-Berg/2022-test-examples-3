import React from 'react';
import { render } from '@testing-library/react';

import { DatePickerAdapter, SwitchAdapter } from './AuditAdapters';

const props: any = {
  input: {
    value: null,
    onChange: () => null,
    name: '',
    onBlur: () => null,
    onFocus: () => null,
    onSelect: () => null,
  },
  meta: {},
};

describe('AuditAdapters', () => {
  describe('<DatePickerAdapter>', () => {
    it('renders without errors', () => {
      render(<DatePickerAdapter {...props} />);
    });
  });

  describe('<SwitchAdapter>', () => {
    it('renders without errors', () => {
      render(<SwitchAdapter {...props} options={[]} />);
    });
  });
});
