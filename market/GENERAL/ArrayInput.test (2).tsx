import React from 'react';
import { DatePicker } from '@yandex-market/mbo-components';
import { shallow } from 'enzyme';
import { Form } from 'react-final-form';

import { ArrayInput } from './ArrayInput';

interface FormObject {
  123: Date[];
}

describe('ArrayInput', () => {
  it('main flow', () => {
    const inputRender = () => <DatePicker onChange={() => null} />;

    expect(() =>
      shallow(
        <Form
          onSubmit={() => undefined}
          render={() => <ArrayInput<Date, FormObject> inputRender={inputRender} defaultValue={new Date()} name="123" />}
        />
      )
    ).not.toThrow();
  });
});
