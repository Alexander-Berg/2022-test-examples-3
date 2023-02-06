import { cleanup, render } from '@testing-library/react';
import React from 'react';
import { UserRun, UserRunData, UserRunDataType, UserRunState, UserRunType } from 'src/rest/definitions';

import { BaseSettings } from './BaseSettings';

const testUserRun = {
  name: 'testName',
  state: UserRunState.CANCELLED,
  startTime: new Date().toISOString(),
  endTime: new Date().toISOString(),
  accountName: 'testAccount',
  notificationRecipients: ['test'],
  userRunType: UserRunType.SM,
  input: {
    type: UserRunDataType.YT,
    ytPath: 'test',
    cluster: 'testCluster',
  } as UserRunData,
  result: {
    type: UserRunDataType.YT,
    ytPath: 'test',
    cluster: 'testCluster',
  } as UserRunData,
} as UserRun;

describe('<BaseSettings />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders with userRun', () => {
    render(<BaseSettings userRun={testUserRun} />);
  });

  it('renders without userRun', () => {
    render(<BaseSettings userRun={null} />);
  });
});
