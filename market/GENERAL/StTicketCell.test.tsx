import React from 'react';
import { fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupWithStore } from 'src/test/setupApp';
import { StTicketCell } from './StTicketCell';
import { DataBucketStatus, FileProcessStatus, LongPipelineColor, PipelineType } from 'src/rest/definitions';

const longPipeline = {
  businessId: 1,
  categoryId: 1,
  dataBucketId: 1,
  dataBucketStatus: DataBucketStatus.FINISHED,
  fileProcessStatus: FileProcessStatus.FINISHED,
  notFinishedTasks: [],
  pipelineId: 2,
  pipelineStartTs: 2,
  pipelineType: PipelineType.DATA_CAMP,
  processId: 2,
  requestId: 2,
  stTicket: '22',
  taskStartTs: 2,
  ticketsCount: 2,
  waitingDataBucketIds: [],
  color: LongPipelineColor.ORANGE,
};

describe('StTicketCell', () => {
  test('change report', async () => {
    const onChangeReport = jest.fn(report => {
      expect(report.stTicket).toBe('MBO-232323');
    });

    const { app, api } = setupWithStore(<StTicketCell onChangeReport={onChangeReport} item={longPipeline} />);

    userEvent.click(app.getByTitle('Редактировать'));

    const input = app.getByRole('textbox');

    userEvent.type(input, 'MBO-232323');

    fireEvent.blur(input);

    await waitFor(() => expect(api.goodContentController.linkStTicket.activeRequests().length).toBe(1));
  });
});
