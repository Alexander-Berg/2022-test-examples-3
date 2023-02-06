import React from 'react';
import { render } from '@testing-library/react';

import { QueueStatisticList } from './QueueStatisticList';
import { MdmQueueStatistic } from 'src/entities/mdmOfferInfo/types';

const refreshReason = {
  time: '2021-10-25T17:33:23.254351Z',
  reason: 'CHANGED_MSKU_DATA',
};

const processedData = {
  addedTimestamp: '2021-10-25T17:33:23.254351Z',
  processedTimestamp: '2021-10-25T17:33:23.254351Z',
  refreshReasons: [refreshReason],
  processed: false,
};

const items: MdmQueueStatistic[] = [
  {
    newestProcessed: undefined,
    numberOfProcessed: 1,
    numberOfUnProcessed: 1,
    oldestUnprocessed: processedData,
    queueName: 'Очередь SSKU, упавших на чтении ЕОХ',
  },
];

describe('<QueueStatisticList />', () => {
  it('display data and open json', async () => {
    const app = render(<QueueStatisticList data={items} />);
    app.getByText('Очередь SSKU, упавших на чтении ЕОХ');
    app.getAllByText(processedData.addedTimestamp);
  });
});
