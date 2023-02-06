import React from 'react';
import { render, RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  shopModel,
  simpleMappingWithRule,
  ruleKey,
  rule,
  categoryData,
  vendorParameter,
  numericParameter,
  stringParameter,
} from 'src/test/data';
import { RuleEditor } from './RuleEditor';
import { setupWithReatom } from 'src/test/withReatom';

const clickAdd = (app: RenderResult) => {
  const add = app.getByText(/добавить/i);
  userEvent.click(add);
};

const inputStringValue = (app: RenderResult, value: string, role: string) => {
  const input = app.getByRole(role);
  userEvent.type(input, value);
};

describe('RuleEditor', () => {
  test('show existing rule', () => {
    const onChange = jest.fn();
    const { getByText } = render(
      <RuleEditor
        parameter={vendorParameter}
        categoryData={categoryData}
        model={shopModel}
        mapping={simpleMappingWithRule}
        rule={rule}
        ruleKey={ruleKey}
        onChange={onChange}
      />
    );
    getByText(/king/i);
  });

  test('add new rule with single numeric value', async () => {
    const onChange = jest.fn((_, receivedRule, marketValues) => {
      expect(marketValues[0].value.numericValue).toEqual(1);
      expect(receivedRule.shopValues).toEqual({ vendor: 'KING' });
    });

    const { app } = setupWithReatom(
      <RuleEditor
        parameter={numericParameter}
        categoryData={categoryData}
        model={shopModel}
        mapping={{ ...simpleMappingWithRule, rules: undefined }}
        ruleKey={ruleKey}
        onChange={onChange}
      />
    );

    clickAdd(app);

    inputStringValue(app, '1', 'spinbutton');

    expect(onChange.mock.calls.length).toEqual(1);
  });

  test('add new rule with single string value', async () => {
    const onChange = jest.fn((_, receivedRule, marketValues) => {
      expect(marketValues[0].value.stringValue).toEqual('1');
      expect(receivedRule).toBeTruthy();
    });

    const { app } = setupWithReatom(
      <RuleEditor
        parameter={stringParameter}
        categoryData={categoryData}
        model={shopModel}
        mapping={{ ...simpleMappingWithRule, rules: undefined }}
        ruleKey={ruleKey}
        onChange={onChange}
      />
    );

    clickAdd(app);

    inputStringValue(app, '1', 'textbox');

    expect(onChange.mock.calls.length).toEqual(1);
  });
});
