import React from 'react';
import { mount } from 'enzyme';
import Bill from './index';

import bills from '../../test/mocks/bill';

beforeEach(() => {
  jest.clearAllMocks();
});

describe('<Bill />', () => {
  const handleChangeBill = jest.fn();

  const BillNew = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.PAY.NEW}
    />
  );
  const BillPaid = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.PAY.PAID}
    />
  );
  const BillRejected = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.PAY.REJECTED}
    />
  );
  const BillInactive = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.PAY.INACTIVE}
      active={false}
    />
  );
  const RefundRequested = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.REFUND.REQUESTED}
    />
  );
  const RefundCompleted = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.REFUND.COMPLETED}
    />
  );
  const RefundFailed = mount(
    <Bill
      opened={true}
      handleChangeBill={handleChangeBill}
      {...bills.REFUND.FAILED}
    />
  );

  it('Отрисовывается правильный футер для неактивного Платежа', () => {
    expect(BillInactive.find('.AdditionalBlock__activate')).toHaveLength(1);
  });

  it('Отрисовывается правильный футер для нового Платежа', () => {
    expect(BillNew.find('.AdditionalBlock__totalLabels')).toHaveLength(1);
    expect(
      BillNew.find(`#orderUrl${bills.PAY.NEW.id}`)
        .hostNodes()
        .instance().value
    ).toEqual(bills.PAY.NEW.orderUrl);
  });

  it('Отрисовывается правильный футер для оплаченного Платежа', () => {
    expect(
      BillPaid.find('.AdditionalBlock')
        .find('.AdditionalBlock__field')
        .at(0)
        .text()
    ).toContain(bills.PAY.PAID.userEmail);
    expect(
      BillPaid.find('.AdditionalBlock')
        .find('.AdditionalBlock__field')
        .at(1)
        .text()
    ).toContain(bills.PAY.PAID.userDescription);
  });

  it('Отрисовывается правильный футер для отклонённого Платежа', () => {
    expect(BillRejected.find('.AdditionalBlock--hidden')).toHaveLength(1);
  });

  it('Отрисовывается правильный футер для Возвратов', () => {
    expect(RefundRequested.find('.AdditionalBlock--hidden')).toHaveLength(1);
    expect(RefundCompleted.find('.AdditionalBlock--hidden')).toHaveLength(1);
    expect(RefundFailed.find('.AdditionalBlock--hidden')).toHaveLength(1);
  });
});
