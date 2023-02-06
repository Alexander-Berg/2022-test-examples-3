import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { shopModel, approvedSkuMapping } from 'src/test/data/shopModel';
import { ProcessingStatus } from 'src/java/definitions';
import { PROCESSING_STATUSES_NAMES, PROCESSING_STATUSES_NAMES_MAPPING } from 'src/filters/options';
import { StatusCell } from './StatusCell';
import { processingStatusInfo } from './utils';

const cases = Object.entries(PROCESSING_STATUSES_NAMES).map(el => ({
  status: el[0],
  text: el[1],
}));

const casesWithMapping = Object.entries(PROCESSING_STATUSES_NAMES_MAPPING).map(el => ({
  status: el[0],
  text: el[1],
}));

describe('<StatusCell />', () => {
  cases.forEach(el => {
    test(`without mapping ${el.status}`, () => {
      render(<StatusCell model={{ ...shopModel, processingStatus: el.status, approvedSkuMapping: undefined }} />);
      screen.getByText(el.text as string);
    });
  });

  casesWithMapping.forEach(el => {
    test(`with mapping ${el.status}`, () => {
      render(<StatusCell model={{ ...shopModel, processingStatus: el.status, approvedSkuMapping }} />);
      screen.getByText(el.text as string);
    });
  });

  test(`show tooltip with info`, () => {
    render(<StatusCell model={{ ...shopModel, processingStatus: ProcessingStatus.NEED_INFO }} />);
    userEvent.hover(screen.queryByText(PROCESSING_STATUSES_NAMES[ProcessingStatus.NEED_INFO]));
    screen.getByText(processingStatusInfo[ProcessingStatus.NEED_INFO]);
  });
});
