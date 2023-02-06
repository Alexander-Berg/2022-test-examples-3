import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { Filter } from './Filter';

describe('<Filter />', () => {
  it('fill filter fields', () => {
    let queryParams = {};
    const acceptFilter = jest.fn();
    const onChangeQueryParams = jest.fn(params => {
      queryParams = params;
    });

    const app = render(
      <Filter acceptFilter={acceptFilter} onChangeQueryParams={onChangeQueryParams} queryParams={queryParams} />
    );

    const uploadedAtStart = new Date(1652378219260);
    const inputs = app.getAllByRole('textbox');
    fireEvent.change(inputs[0], { target: { value: uploadedAtStart } });

    expect(onChangeQueryParams).toBeCalledTimes(1);
    app.rerender(
      <Filter acceptFilter={acceptFilter} onChangeQueryParams={onChangeQueryParams} queryParams={queryParams} />
    );

    const userLogin = 'I.V.Stalin';
    fireEvent.change(inputs[inputs.length - 1], { target: { value: userLogin } });
    expect(onChangeQueryParams).toBeCalledTimes(2);
    expect(onChangeQueryParams).toHaveBeenLastCalledWith({
      uploadedAtStart: [
        uploadedAtStart.getFullYear(),
        `00${uploadedAtStart.getMonth() + 1}`.slice(-2),
        `00${uploadedAtStart.getDate()}`.slice(-2),
      ].join('-'),
      userLogin,
    });
  });
});
