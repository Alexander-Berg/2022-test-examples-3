import { waitFor } from '@testing-library/react';
import { get, post } from 'api/common';
import { RuleService } from './RuleService';

const RULE_TEXT = 'rule text';

jest.mock('api/common', () => {
  return {
    __esModule: true,
    get: jest.fn(() => Promise.resolve(`<div>${RULE_TEXT}</div>`)),
    post: jest.fn(() => Promise.resolve()),
  };
});

describe('RuleService', () => {
  it('loads rule', async () => {
    jest.useFakeTimers();

    const ruleName = 'rule';
    const rs = new RuleService(ruleName);

    expect(get).toBeCalledWith(
      expect.objectContaining({
        data: {
          name: ruleName,
        },
      }),
    );

    await waitFor(() => {
      expect(rs.ruleText).toEqual(RULE_TEXT);
    });

    jest.useRealTimers();
  });

  it('saves rule', () => {
    const ruleName = 'rule';
    const ruleText = '# Rule text';

    const rs = new RuleService(ruleName);
    rs.setRuleText(ruleText);
    rs.save();

    expect(post).toBeCalledWith(
      expect.objectContaining({
        data: {
          Name: ruleName,
          Rules: ruleText,
        },
      }),
    );
  });
});
