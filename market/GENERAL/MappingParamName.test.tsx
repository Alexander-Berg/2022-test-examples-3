import React from 'react';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { MappingParamName } from './MappingParamName';

const row = {
  displayShopParams: '',
  displayMarketParams: 'Цвет',
  original: {},
};
describe('<MappingParamName />', () => {
  test('add mappping', () => {
    const onAdd = jest.fn();
    const app = render(<MappingParamName text="Добавить" row={row} onAdd={onAdd} propName="displayShopParams" />);
    const name = app.getByText('Добавить');

    act(() => {
      userEvent.click(name);
    });

    expect(onAdd).toBeCalled();
  });

  test('display mappping', () => {
    const onAdd = jest.fn();
    const app = render(
      <MappingParamName
        text="Добавить"
        row={{ ...row, displayShopParams: 'Цвет' }}
        onAdd={onAdd}
        propName="displayShopParams"
      />
    );
    app.getByText('Цвет');
  });
});
