import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { DIRECT_DISCOUNT_PROMO } from 'src/pages/promo/tests/mocks/promo';
import { DatePeriod } from './DatePeriod';

describe('<DatePeriod />', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
    jest.clearAllMocks();
  });

  const renderComponent = () => {
    wrapper = mount(<DatePeriod promo={DIRECT_DISCOUNT_PROMO} />);
  };

  function getDatePeriodComponent() {
    return wrapper!.find(DatePeriod);
  }

  beforeEach(() => {
    renderComponent();
  });

  it('should be render without errors', () => {
    expect(getDatePeriodComponent()).toHaveLength(1);
  });

  it('should contain promo period', () => {
    expect(getDatePeriodComponent().text()).toEqual(
      // eslint-disable-next-line no-irregular-whitespace
      `${DIRECT_DISCOUNT_PROMO.period.from} — ${DIRECT_DISCOUNT_PROMO.period.to}`
    );
  });
});
