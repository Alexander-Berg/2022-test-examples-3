import React from 'react';
import { mount } from 'enzyme';
import Bills from './index';
import Bill from '../Bill/index';
import Filters from './__filters/index';

import bills from '../../test/mocks/bill';
const billMock = bills.PAY.NEW;

describe('<Bills />', () => {
  const BillsEmpty = mount(<Bills bills={[]} />);
  const BillsOne = mount(<Bills bills={[billMock]} />);
  const BillsFive = mount(
    <Bills bills={[billMock, billMock, billMock, billMock, billMock]} />
  );

  it('Отрисовывает правильное количество Платежей и панель фильтров', () => {
    expect(BillsOne.find(Bill)).toHaveLength(1);
    expect(BillsOne.find(Filters).prop('isEmpty')).toEqual(false);
    expect(BillsOne.find('.Bills__filters')).toHaveLength(1);
    expect(BillsOne.find('.Bills__notfound')).toHaveLength(0);
  });

  it('Отрисовывает приветствие, если Платежей нет изначально', () => {
    expect(BillsEmpty.find(Filters).prop('isEmpty')).toEqual(true);
    expect(BillsEmpty.find(Filters).prop('isBillCreating')).toEqual(false);
    expect(BillsEmpty.find('.Bills__empty')).toHaveLength(1);
  });

  it('Отрисовывает "Загрузить ещё", если Платежей >= 5', () => {
    expect(BillsFive.find('.Bills__pagination')).toHaveLength(1);
  });

  it('Не отрисовывает "Загрузить ещё", если Платежей < 5', () => {
    expect(BillsOne.find('.Bills__pagination')).toHaveLength(0);
  });

  it('Форма создания появляется при клике на "Создать новый"', () => {
    BillsFive.find('.Bills__leftContainer')
      .find('a')
      .simulate('click');
    expect(BillsFive.find('.CreateBill--hidden')).toHaveLength(0);
  });
});
