import React from 'react';

import { OfferDiagnosticTable } from './OfferDiagnosticTable';
import {
  DatacampOfferInfo,
  OfferProcessingTaskStatus,
  OfferProcessingType,
  OfferStatus,
  OfferTarget,
} from 'src/entities/datacampOfferInfo/types';
import { setupTestProvider } from 'src/test/utils';
import { render, screen } from '@testing-library/react';

const TEST_DATACAMP_OFFER_INFO: DatacampOfferInfo = {
  offerId: '1',
  groupId: 2,
  businessId: 1,
  contentProcessingOfferStatus: OfferStatus.NOT_UP_TO_DATE,
  mbocOfferStatus: OfferStatus.UP_TO_DATE,
  dcOfferStatus: OfferStatus.MISSING,
  offerLink: '',
  datacampLink: 'url-v-nikuda',
  datacampGroupLink: '',
  auditLink: '',
  agLink: '',
  shouldBeProcessed: true,
  shouldBeStored: false,
  shouldProcessContent: true,
  messages: ['Offer has no market specific parameters', 'Offer has no pictures', 'Invalid message'],
  offerProcessingTaskInfo: [
    {
      taskPriority: 1,
      processingTicketId: 123,
      taskId: 111,
      status: OfferProcessingTaskStatus.CANCELLING,
      created: '',
      type: OfferProcessingType.IN_PROCESS,
      target: OfferTarget.YANG,
    },
  ],
  offerProcessingAssignmentInfo: {
    processingTicketId: null,
    ticketDeadline: undefined,
    trackerTicket: undefined,
    target: undefined,
  },
};

describe('<OfferDiagnosticTable />', () => {
  it('renders without errors', () => {
    const { Provider } = setupTestProvider();
    const app = render(
      <Provider>
        <OfferDiagnosticTable data={TEST_DATACAMP_OFFER_INFO} links={[]} />
      </Provider>
    );

    expect(app.getAllByText('НЕ OK')).toHaveLength(2);
  });

  it('renders empty data without errors', () => {
    const { Provider } = setupTestProvider();
    const offerInfo: DatacampOfferInfo = {} as DatacampOfferInfo;
    render(
      <Provider>
        <OfferDiagnosticTable data={offerInfo} links={[]} />
      </Provider>
    );
  });

  it('renders data with nulls without errors', () => {
    // Да, бывает, что приходят null несмотря на типы в definitions.ts
    const offerInfo: DatacampOfferInfo = {
      messages: null,
      shouldProcessContent: null,
      shouldBeStored: null,
      shouldBeProcessed: null,
      agLink: null,
      auditLink: null,
      datacampGroupLink: null,
      datacampLink: null,
      offerLink: null,
      groupId: null,
      dcOfferStatus: null,
      mbocOfferStatus: null,
      contentProcessingOfferStatus: null,
      businessId: null,
      offerId: null,
    } as unknown as DatacampOfferInfo;

    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <OfferDiagnosticTable data={offerInfo} links={[]} />
      </Provider>
    );
  });

  it('renders data with nulls without errors', () => {
    const { Provider } = setupTestProvider();

    const yqlLinks = [
      {
        text: 'Логфеллер КИ -> ЕОХ',
        url: 'market-datacamp-offers-to-mboc-united',
        source: 'yql',
      },
    ];

    render(
      <Provider>
        <OfferDiagnosticTable data={TEST_DATACAMP_OFFER_INFO} links={yqlLinks} />
      </Provider>
    );

    screen.getByText(yqlLinks[0].text);
  });
});
