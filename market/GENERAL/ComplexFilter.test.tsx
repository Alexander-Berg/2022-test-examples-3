import React from 'react';
import { fireEvent, render, RenderResult, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ComplexFilter } from './ComplexFilter';
import { EConditionOperator, EConditionType, EParamInfoType, FieldType, IParamInfo } from './types';

const DOWN_ARROW = { keyCode: 40 }; // down arrow key code

describe('<ComplexFilter/>', () => {
  it('renders without errors', () => {
    const TEST_PARAMS: IParamInfo[] = [
      {
        type: EParamInfoType.NUMBER,
        name: 'Number',
        id: '123',
        operators: [
          {
            name: 'operator1',
            condition: EConditionType.CONTAINS,
            fields: [
              {
                type: FieldType.NUMBER,
                id: 'val',
                label: 'Number-Number',
              },
            ],
          },
        ],
      },
    ];
    const onChangeCallback = jest.fn();
    const app = render(<ComplexFilter params={TEST_PARAMS} onChange={onChangeCallback} />);
    const addBtn = app.getByTitle('Добавить');
    addFilterAndOperator(app, 'Number', 'operator1', 'operator1');

    const input = app.getByText('Number-Number')?.parentElement?.parentElement?.getElementsByTagName('input').item(0);
    userEvent.type(input!, '456');

    userEvent.click(addBtn);

    const selectBetweenFilters = app.getByText('И');
    fireEvent.keyDown(selectBetweenFilters, DOWN_ARROW);
    const orOption = screen.getByText('ИЛИ');
    userEvent.click(orOption);

    const applyButton = app.getByText('Применить');
    userEvent.click(applyButton);

    expect(onChangeCallback.mock.calls.pop()[0]).toEqual([
      {
        id: '123',
        condition: EConditionType.CONTAINS,
        data: { val: '456' },
      },
    ]);
  });

  it('render select', () => {
    const TEST_SELECT_PARAMS: IParamInfo[] = [
      {
        type: EParamInfoType.ENUM,
        name: 'SELECT_PARAM',
        id: '123',
        operators: [
          {
            name: 'operator1',
            condition: EConditionType.INCLUDES_ANY,
            fields: [
              {
                type: FieldType.SELECT,
                id: 'val',
                label: 'Select-control-label',
                extraParams: {
                  options: [
                    { value: 1, label: 'option1' },
                    { value: 2, label: 'option2' },
                  ],
                },
              },
            ],
          },
        ],
      },
    ];

    const onChangeCallback = jest.fn();
    const app = render(<ComplexFilter params={TEST_SELECT_PARAMS} onChange={onChangeCallback} />);
    addFilterAndOperator(
      app,
      TEST_SELECT_PARAMS[0].name,
      TEST_SELECT_PARAMS[0].operators[0].name,
      TEST_SELECT_PARAMS[0].operators[0].name
    );

    const selectControl = app.getByText('Выбрать');
    fireEvent.keyDown(selectControl, DOWN_ARROW);

    const selectOption = screen.getByText('option1');
    userEvent.click(selectOption);

    const applyButton = app.getByText('Применить');
    userEvent.click(applyButton);

    expect(onChangeCallback).toHaveBeenLastCalledWith(
      [
        {
          id: '123',
          condition: EConditionType.INCLUDES_ANY,
          data: {
            val: [1],
          },
        },
      ],
      [EConditionOperator.AND]
    );
  });

  it('drop filters', () => {
    const TEST_SELECT_PARAMS: IParamInfo[] = [
      {
        type: EParamInfoType.ENUM,
        name: 'SELECT_PARAM',
        id: '123',
        operators: [
          {
            name: 'operator1',
            condition: EConditionType.INCLUDES_ANY,
            fields: [
              {
                type: FieldType.SELECT,
                id: 'val',
                label: 'Select-control-label',
                extraParams: {
                  options: [{ value: 1, label: 'option1' }],
                },
              },
            ],
          },
          {
            name: 'operator2',
            condition: EConditionType.INCLUDES_ANY,
            fields: [
              {
                type: FieldType.SELECT,
                id: 'val',
                label: 'Select-control-label',
                extraParams: {
                  options: [{ value: 1, label: 'option1' }],
                },
              },
            ],
          },
        ],
      },
    ];

    const onChangeCallback = jest.fn();
    const app = render(<ComplexFilter params={TEST_SELECT_PARAMS} onChange={onChangeCallback} />);
    addFilterAndOperator(
      app,
      TEST_SELECT_PARAMS[0].name,
      TEST_SELECT_PARAMS[0].operators[1].name,
      TEST_SELECT_PARAMS[0].operators[0].name
    );
    const dropFiltersBtn = app.getByText('Сбросить');
    userEvent.click(dropFiltersBtn);

    expect(onChangeCallback).toBeCalledTimes(1);
  });
});

function addFilterAndOperator(
  app: RenderResult,
  filterName: string,
  operatorName: string,
  defaultOperatorTitle: string
) {
  const addBtn = app.getByTitle('Добавить');
  expect(addBtn).toBeTruthy();
  userEvent.click(addBtn);

  const selectControl = app.getByText('Выбрать');
  fireEvent.keyDown(selectControl, DOWN_ARROW);

  let selectOption = screen.getByText(filterName);
  userEvent.click(selectOption);

  const conditionSelectControl = app.getByText(defaultOperatorTitle);
  fireEvent.keyDown(conditionSelectControl, DOWN_ARROW);

  selectOption = screen.getAllByText(operatorName)[0]!;
  userEvent.click(selectOption);
}
