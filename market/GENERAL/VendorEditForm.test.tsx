import { render, screen } from '@testing-library/react';
import React from 'react';
import userEvent from '@testing-library/user-event';

import { VendorEditForm } from './VendorEditForm';

const vendor = {
  categoryId: 6334304,
  guruVendor: false,
  id: 14229330,
  models: 0,
  name: 'A4Tech',
  operatorComment: '',
  partnerModels: 0,
  publishLevel: 'PUBLISHED',
  topVendor: false,
};

describe('VendorEditForm', () => {
  test('change name field', () => {
    const onChange = jest.fn();
    const onSubmit = jest.fn();

    render(<VendorEditForm vendor={vendor} onChange={onChange} onSubmit={onSubmit} />);

    const nameField = screen.getByDisplayValue(vendor.name);
    const newName = 'new A4Tech';
    userEvent.clear(nameField);
    userEvent.type(nameField, newName);
    // 1 - initialized, 2 - change value
    expect(onChange).toHaveBeenCalledTimes(2);

    const submitBtn = screen.getByText('Сохранить');
    userEvent.click(submitBtn);
    expect(onSubmit).lastCalledWith({ ...vendor, name: newName });
  });
});
