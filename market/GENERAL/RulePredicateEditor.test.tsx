import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createStore } from '@reatom/core';
import * as R from 'ramda';

import { renderWithReatomStore } from 'src/test/setupTestProvider';
import { Subject, CategoryParameterDto, CategoryParameterType, DisplayCategory } from 'src/java/definitions';
import { CategoriesActions, CategoriesListAtom } from 'src/store/atoms';
import { RulePredicateEditor, RulePredicateEditorProps, RulePredicateConditionType, getValidRulePredicate } from '.';
import { RulePredicateCondition } from './RulePredicateEditor.types';
import { getConditionLabel } from './RulePredicateEditor.helpers';

const defaultParametersMap: Record<string, CategoryParameterDto> = R.indexBy(p => `${p.id}`, [
  {
    id: 1,
    type: CategoryParameterType.NUMERIC,
    name: 'numeric-param',
    options: [],
    xslName: 'numeric',
  },
  {
    id: 100,
    type: CategoryParameterType.NUMERIC,
    name: 'other-param-100',
    options: [],
    xslName: 'numeric',
  },
  {
    id: 2,
    type: CategoryParameterType.BOOLEAN,
    name: 'enum-param',
    options: [
      {
        id: 10,
        name: 'true-option',
        parameterId: 2,
      },
      {
        id: 20,
        name: 'false-option',
        parameterId: 2,
      },
    ],
    xslName: 'enum',
  },
]);

const defaultParameters = Object.values(defaultParametersMap);

const defaultProps: RulePredicateEditorProps = {
  conditionType: RulePredicateConditionType.If,
  index: 0,
  onChange: jest.fn(),
  onRemove: jest.fn(),
  parameters: defaultParameters,
  predicate: getValidRulePredicate(RulePredicateConditionType.If, defaultParameters, {
    id: 0,
    subject: Subject.PARAMETER,
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

  it('should be display subject value', () => {
    const { predicate } = defaultProps;
    renderWithReatomStore(<RulePredicateEditor {...defaultProps} />);

    const pattern = new RegExp(defaultParametersMap[predicate.paramId].name);

    expect(screen.getByText(pattern)).toBeInTheDocument();
  });

  it('should be display condition value', () => {
    const { predicate, conditionType } = defaultProps;
    renderWithReatomStore(<RulePredicateEditor {...defaultProps} />);

    const pattern = new RegExp(getConditionLabel(conditionType, predicate.condition as RulePredicateCondition));

    expect(screen.getByText(pattern)).toBeInTheDocument();
  });

  describe('value box', () => {
    it('should be display number value', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 1,
        condition: RulePredicateCondition.NumberMatches,
        minValue: 456,
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      expect(screen.getByDisplayValue(`${predicate.minValue}`)).toBeInTheDocument();
    });

    it('should be display range values', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 1,
        condition: RulePredicateCondition.NumberRange,
        minValue: 456,
        maxValue: 654,
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      expect(screen.getByDisplayValue(`${predicate.minValue}`)).toBeInTheDocument();
      expect(screen.getByDisplayValue(`${predicate.maxValue}`)).toBeInTheDocument();
    });

    it('should be display copy values', () => {
      const conditionType = RulePredicateConditionType.Then;
      const predicate = getValidRulePredicate(conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 1,
        condition: RulePredicateCondition.CopyValue,
        sourceParamId: 100,
      });

      renderWithReatomStore(
        <RulePredicateEditor {...defaultProps} conditionType={conditionType} predicate={predicate} />
      );

      const pattern = new RegExp(defaultParametersMap[predicate.sourceParamId].name);

      expect(screen.getByText(pattern)).toBeInTheDocument();
    });

    it('should be display enum matches option', () => {
      const predicate = getValidRulePredicate(defaultProps.conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 2,
        condition: RulePredicateCondition.EnumMatches,
        valueId: 20,
      });

      renderWithReatomStore(<RulePredicateEditor {...defaultProps} predicate={predicate} />);

      const { options } = defaultParametersMap[predicate.paramId];
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      const option = options.find(o => o.id === predicate.valueId)!;
      const pattern = new RegExp(option?.name);

      expect(screen.getByText(pattern)).toBeInTheDocument();
    });

    it('should be display exclude option', () => {
      const conditionType = RulePredicateConditionType.Then;
      const predicate = getValidRulePredicate(conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 2,
        condition: RulePredicateCondition.ValueUndefined,
        excludeRevokeValueIds: [10, 20],
      });

      renderWithReatomStore(
        <RulePredicateEditor {...defaultProps} conditionType={conditionType} predicate={predicate} />
      );

      const { options } = defaultParametersMap[predicate.paramId];
      const [o1, o2] = options;

      expect(screen.getByText(new RegExp(o1.name))).toBeInTheDocument();
      expect(screen.getByText(new RegExp(o2.name))).toBeInTheDocument();
    });

    it('should be display category change', () => {
      const conditionType = RulePredicateConditionType.Then;
      const predicate = getValidRulePredicate(conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.CATEGORY_CHANGE,
        paramId: 0,
        condition: RulePredicateCondition.EnumMatches,
        valueId: 9485,
      });

      const store = createStore();
      store.subscribe(CategoriesListAtom, () => null);
      store.dispatch(
        CategoriesActions.setCategories({
          0: {
            guruCategoryId: 0,
            guruCategoryName: '',
            hid: 9485,
            name: 'Test category',
            parentHid: -1,
            published: true,
          } as DisplayCategory,
        })
      );

      renderWithReatomStore(
        <RulePredicateEditor {...defaultProps} conditionType={conditionType} predicate={predicate} />,
        { store }
      );

      expect(screen.getByText(/Test category/)).toBeInTheDocument();
    });

    it('should be display tag value', () => {
      const conditionType = RulePredicateConditionType.Then;
      const predicate = getValidRulePredicate(conditionType, defaultProps.parameters, {
        id: 0,
        subject: Subject.PARAMETER,
        paramId: 0,
        condition: RulePredicateCondition.SetProcessingTag,
        processingTag: 'super-tag',
      });

      renderWithReatomStore(
        <RulePredicateEditor {...defaultProps} conditionType={conditionType} predicate={predicate} />
      );

      expect(screen.getByDisplayValue(`${predicate.processingTag}`)).toBeInTheDocument();
    });
  });
});
