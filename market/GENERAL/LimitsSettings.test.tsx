import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { LimitsSettings } from './LimitsSettings';

describe('<LimitsSettings />', () => {
  it('render empty data', () => {
    const onChange = jest.fn();
    const app = render(<LimitsSettings limits={{}} onChange={onChange} />);

    const input = app.getByLabelText('BLUE_CLASSIFICATION');
    userEvent.paste(input, '123');

    expect(onChange).toHaveBeenLastCalledWith({ BLUE_CLASSIFICATION: 123 });
  });
});
