import React from 'react';
import { mount } from 'enzyme';
import CreateBill from './index';

describe('<CreateBill />', () => {
  const onCreateClick = jest.fn();
  const wrapper = mount(
    <CreateBill
      onCreateClick={onCreateClick}
      isNewBillSubmitting={false}
      isBillCreating={true}
    />
  );

  it('Выдаёт ошибку для незаполненных полей при создании Платежа', () => {
    wrapper.setState({
      caption: '',
      items: [
        {
          name: '',
          amount: '10',
          price: '100',
          nds: 'nds_18',
          total_price: 0,
          errors: [],
          currency: 'RUB'
        }
      ],
      totalAmount: 1,
      price: 1000
    });
    wrapper
      .find('.CreateBill__row_buttons')
      .find('button')
      .simulate('click');
    expect(wrapper.state().captionError).toEqual(true);
    wrapper.state().items.forEach(item => {
      expect(item.errors.length).toEqual(1);
    });
  });
});
