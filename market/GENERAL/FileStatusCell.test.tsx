import React from 'react';
import { render } from '@testing-library/react';

import { FileStatusCell, STATUSES_MAP } from './FileStatusCell';
import { FileStatus, FileType } from './types';

const files: FileType[] = [
  {
    name: 'processing file',
    uploadDate: new Date(),
    status: FileStatus.CANCEL,
    withError: true,
  },
  {
    name: 'canceled file',
    uploadDate: new Date(),
    status: FileStatus.CANCEL,
    withError: true,
  },
  {
    name: 'done file',
    uploadDate: new Date(),
    totalModels: 1000,
    status: FileStatus.DONE,
  },
];

describe('FileStatusCell', () => {
  files.forEach(el => {
    test(el.name, () => {
      const { getByText } = render(<FileStatusCell file={el} />);
      getByText(STATUSES_MAP[el.status].name);
    });
  });
});
