import React from 'react';
import { mount } from 'enzyme';

import { ArrayInput } from '.';
import { DatePicker } from '../DatePicker';

const formatDateToDDMMYYYY = (date: Date) =>
  `${date
    .getDate()
    .toString()
    .padStart(2, '0')}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getFullYear()}`;

describe('<ArrayInput />', () => {
  it('renders with date inputs', () => {
    const arrayInput = mount(
      <ArrayInput<Date | null>
        inputRender={({ value, ...props }) => <DatePicker selected={value} {...props} />}
        defaultValue={new Date()}
        value={[new Date()]}
        onChange={() => null}
      />
    );

    const inputNode = (arrayInput
      .find('.MboComponentsArrayInputWrapper input')
      .first()
      .instance() as unknown) as HTMLInputElement;

    expect(inputNode.value).toBe(formatDateToDDMMYYYY(new Date()));
  });
});
