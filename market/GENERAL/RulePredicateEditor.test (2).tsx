import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as R from 'ramda';

import { renderWithReatomStore } from 'src/test/setupTestProvider';
import {
  CategoryParameterType,
  PredicateType,
  ValueSource,
  PredicateOperation,
  LocalCategoryParameter,
  ParamOptionShortDto,
} from 'src/java/definitions';
import { RulePredicateEditor, RulePredicateEditorProps, getValidRulePredicate } from '.';
import { getConditionLabel } from './RulePredicateEditor.helpers';

const defaultParametersMap: Record<string, LocalCategoryParameter> = R.indexBy(p => `${p.paramId}`, [
  {
    paramId: 1,
    type: CategoryParameterType.NUMERIC,
    name: 'numeric-param',
    options: [] as ParamOptionShortDto[],
    xslName: 'numeric',
  },
  {
    paramId: 100,
    type: CategoryParameterType.NUMERIC,
    name: 'other-param-100',
    options: [] as ParamOptionShortDto[],
    xslName: 'numeric',
  },
  {
    paramId: 2,
    type: CategoryParameterType.BOOLEAN,
    name: 'enum-param',
    options: [
      {
        valueId: 10,
        name: 'true-option',
        parameterId: 2,
      },
      {
        valueId: 20,
        name: 'false-option',
        parameterId: 2,
      },
    ],
    xslName: 'enum',
  },
] as LocalCategoryParameter[]);

const defaultParameters = Object.values(defaultParametersMap);

const defaultProps: RulePredicateEditorProps = {
  conditionType: PredicateType.IF,
  index: 0,
  onChange: jest.fn(),
  onRemove: jest.fn(),
  globalParameters: defaultParameters,
  localParameters: defaultParameters,
  predicate: getValidRulePredicate(PredicateType.IF, defaultParameters, {
    ruleId: 0,
    source: ValueSource.MODEL_PARAMETER,
  }),
};

describe('<RulePredicateEditor />', () => {
  it('renders without errors', () => {
    renderWithReatomStore(<RulePredicateEditor {...defaultProps} />);
  });

  it('should call onRemove prop', () => {
    const handleRemove = jest.fn();

    renderWithReatomStore(<RulePredicateEditor {...defaultProps} index={2} onRemove={handleRemove} />);

    userEvent.click(screen.getByRole('button'));

    expect(handleRemove).toBeCalledTimes(1);
    expect(handleRemove).toBeCalledWith(2);
  });

  it('should be display parameter name', () => {
    const { predicate } = defaultProps;
    renderWithReatomStore(<RulePredicateEditor {...defaultProps} />);

    const pattern = new RegExp(defaultParametersMap[predicate.paramId].name);

    expect(screen.getByText(pattern)).toBeInTheDocument();
  });

  it('should be display condition value', () => {
    const { predicate, conditionType } = defaultProps;
    renderWithReatomStore(<RulePredicateEditor {...defaultProps} />);

    const pattern = new RegExp(getConditionLabel(conditionType, predicate.predicateOperation));

    expect(screen.getByText(pattern)).toBeInTheDocument();
  });

  describe('value box', () => {
    it('should be display number value', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.localParameters, {
        ruleId: 0,
        source: ValueSource.MODEL_PARAMETER,
        paramId: 1,
        predicateOperation: PredicateOperation.MATCHES,
        minValue: 456,
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      expect(screen.getByDisplayValue(`${predicate.minValue}`)).toBeInTheDocument();
    });

    it('should be display range values', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.localParameters, {
        ruleId: 0,
        source: ValueSource.MODEL_PARAMETER,
        paramId: 1,
        predicateOperation: PredicateOperation.INSIDE_RANGE,
        minValue: 456,
        maxValue: 654,
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      expect(screen.getByDisplayValue(`${predicate.minValue}`)).toBeInTheDocument();
      expect(screen.getByDisplayValue(`${predicate.maxValue}`)).toBeInTheDocument();
    });

    it('should be display enum matches option', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.localParameters, {
        ruleId: 0,
        source: ValueSource.MODEL_PARAMETER,
        paramId: 2,
        predicateOperation: PredicateOperation.MATCHES,
        allValueId: [20],
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      const { options } = defaultParametersMap[predicate.paramId];
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      const option = options.find(o => o.valueId === predicate.allValueId[0])!;
      const pattern = new RegExp(option?.name);

      expect(screen.getByText(pattern)).toBeInTheDocument();
    });
  });
});
