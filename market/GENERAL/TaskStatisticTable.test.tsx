import React from 'react';
import { render } from '@testing-library/react';

import { ModelRuleTaskCounterDto } from 'src/java/definitions';
import { TaskStatisticTable } from './TaskStatisticTable';

const TASK_COUNTERS = [
  {
    categoryId: 1,
    modifiedModelCount: 1,
    partRollbackModelCount: 1,
    processFailedModelCount: 1,
    processedModelCount: 1,
    rollbackFailedModelCount: 1,
    rollbackModelCount: 1,
    taskId: 1,
  },
] as ModelRuleTaskCounterDto[];

describe('<TaskStatisticTable />', () => {
  it('render without errors', () => {
    render(<TaskStatisticTable taskCounters={TASK_COUNTERS} />);
  });
});
