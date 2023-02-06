import React from 'react';
import { render } from '@testing-library/react';

import { ProcessingAssignmentInfo } from './ProcessingAssignmentInfo';
import { OfferProcessingType, OfferTarget, SkuType } from 'src/entities/datacampOfferInfo/types';

describe('<ProcessingAssignmentInfo />', () => {
  it('renders without errors', () => {
    const data = {
      assignedTs: '1994-06-24T17:14:03.765788',
      created: '',
      priority: 0,
      processingCounter: null,
      processingTicketId: undefined,
      skuType: SkuType.FAST_SKU,
      target: OfferTarget.YANG,
      targetSkuId: 123,
      ticketCritical: true,
      ticketDeadline: '2012-12-21T17:14:03.765788',
      trackerTicket: 'SPOTT-1149',
      type: OfferProcessingType.IN_PROCESS,
    };
    const app = render(<ProcessingAssignmentInfo offerProcessingAssignmentInfo={data} />);

    expect(app.getByText('1994', { exact: false })).toBeInTheDocument();
    expect(app.getByText('0', { exact: true })).toBeInTheDocument();
    expect(app.getByText('SPOTT-1149', { exact: false })).toHaveAttribute(
      'href',
      'https://st.yandex-team.ru/SPOTT-1149'
    );
  });
});
