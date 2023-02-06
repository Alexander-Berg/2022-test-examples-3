import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';

import { ModelRuleDto } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { ModelRulesTable } from './ModelRulesTable';

const RULES = [
  {
    id: 1,
    name: 'test1',
    group: 'group1',
    priority: 10,
    active: true,
  },
  {
    id: 2,
    name: 'test2',
    group: 'group2',
    priority: 20,
    active: true,
  },
] as ModelRuleDto[];

const mockedDeleteFunction = jest.fn();

describe('<ModelRulesTable />', () => {
  const Provider = setupTestProvider();

  it('render without rules', () => {
    render(
      <Provider>
        <ModelRulesTable rules={[]} onDelete={jest.fn()} />
      </Provider>
    );
    const tableText = screen?.getByText('Правила для текущей категории отсутствуют');

    expect(tableText).toBeTruthy();
  });

  it('delete rule', () => {
    render(
      <Provider>
        <ModelRulesTable rules={RULES} onDelete={mockedDeleteFunction} />
      </Provider>
    );

    expect(screen?.getAllByRole('rowgroup').length).toEqual(2);

    const deleteButton = screen?.getAllByTitle('Удалить')?.[0];
    userEvent.click(deleteButton);

    expect(mockedDeleteFunction).toBeCalledTimes(1);
    expect(mockedDeleteFunction).lastCalledWith(RULES[0]);
  });
});
