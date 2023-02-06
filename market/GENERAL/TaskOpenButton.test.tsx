import React from 'react';
import { render } from '@testing-library/react';

import { ModelRuleTaskStatus } from 'src/java/definitions';
import { TaskOpenButton } from './TaskOpenButton';

const TASK = {
  canBeRolledback: true,
  categoryId: 1,
  changedModelCount: 1,
  errorStr: '',
  finishDate: '01.01.1970T14:00:00',
  fullyRolledbackModelCount: 1,
  initiatorName: 'test1',
  partiallyRolledbackModelCount: 1,
  resumesCount: 1,
  rollbackInitiated: true,
  rollbackInitiatorName: 'rollback test1',
  rulesList: [],
  sessionId: 1,
  startDate: '01.01.1970T12:00:00',
  status: ModelRuleTaskStatus.EXEC_CANCELLED,
  totalModelCount: 1,
};

describe('<TaskOpenButton />', () => {
  it('render without errors', () => {
    render(<TaskOpenButton task={TASK} onShowTask={jest.fn()} />);
  });
});
