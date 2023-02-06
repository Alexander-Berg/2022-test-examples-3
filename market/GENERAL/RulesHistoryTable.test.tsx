import React from 'react';
import { render, screen } from '@testing-library/react';

import { ModelRuleTaskStatus, RuleTaskDto } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { RulesHistoryTable } from './RulesHistoryTable';

const TASKS = [
  {
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
  },
  {
    canBeRolledback: false,
    categoryId: 2,
    changedModelCount: 2,
    errorStr: '',
    finishDate: '02.01.1970T14:00:00',
    fullyRolledbackModelCount: 2,
    initiatorName: 'test2',
    partiallyRolledbackModelCount: 2,
    resumesCount: 2,
    rollbackInitiated: false,
    rollbackInitiatorName: 'rollback test2',
    rulesList: [],
    sessionId: 2,
    startDate: '02.01.1970T12:00:00',
    status: ModelRuleTaskStatus.EXEC_INPROGRESS,
    totalModelCount: 2,
  },
] as RuleTaskDto[];

const mockedRollbackFunction = jest.fn();
const mockedShowDetailsViewFunction = jest.fn();

describe('<RulesHistoryTable />', () => {
  it('render without rules', () => {
    render(<RulesHistoryTable tasks={[]} onRollback={jest.fn()} />);
    const tableText = screen?.getByText('История применения групповых правил для текущей категории отсутствуют');

    expect(tableText).toBeTruthy();
  });

  it('rollback rule', () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <RulesHistoryTable
          tasks={TASKS}
          onRollback={mockedRollbackFunction}
          onShowDetailsView={mockedShowDetailsViewFunction}
        />
      </Provider>
    );

    expect(screen?.getAllByRole('rowgroup').length).toEqual(2);
  });
});
