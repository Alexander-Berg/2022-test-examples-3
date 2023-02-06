import React, { useEffect } from 'react';
import { useAction } from '@reatom/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { render } from 'src/test/customRender';
import { setModelRuleTasksAction } from 'src/widgets/Rules/store';
import { RulesHistory } from 'src/widgets/Rules/widgets';
import { RuleTaskDto } from 'src/java/definitions';

describe('<RulesHistory />', () => {
  it('renders without errors', () => {
    const Provider = setupTestProvider();
    expect(() =>
      render(
        <Provider>
          <MyComp />
        </Provider>
      )
    ).not.toThrow();
  });
});

function MyComp() {
  const setTasks = useAction(setModelRuleTasksAction);
  useEffect(() => {
    setTasks([{ categoryId: 123, rulesList: [{ categoryId: 456, modelRule: { active: false } }] }] as RuleTaskDto[]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return <RulesHistory />;
}
