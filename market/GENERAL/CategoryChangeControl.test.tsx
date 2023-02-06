import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createStore } from '@reatom/core';

import { Subject } from 'src/java/definitions';
import { CategoriesActions, CategoriesListAtom } from 'src/store/atoms';
import { renderWithReatomStore } from 'src/test/setupTestProvider';
import { getValidRulePredicate } from '../RulePredicateEditor.helpers';
import { RulePredicateConditionType, RulePredicateCondition } from '../RulePredicateEditor.types';
import { CategoryChangeControl } from '.';

function createPredicate() {
  return getValidRulePredicate(RulePredicateConditionType.Then, [], {
    id: 0,
    subject: Subject.CATEGORY_CHANGE,
  });
}

describe('<CategoryChangeControl />', () => {
  it('renders without errors', () => {
    const predicate = createPredicate();

    renderWithReatomStore(<CategoryChangeControl predicate={predicate} onChange={jest.fn()} />);
  });

  it('should predicate change correctly', async () => {
    const handleChange = jest.fn();
    const predicate = createPredicate();
    const store = createStore();
    store.subscribe(CategoriesListAtom, () => null);
    store.dispatch(
      CategoriesActions.setCategories({
        0: {
          guruCategoryId: 0,
          guruCategoryName: '',
          hid: 151,
          name: 'Test category',
          parentHid: -1,
          published: true,
          tovarId: 1234455,
          visual: true,
        },
      })
    );
    renderWithReatomStore(<CategoryChangeControl predicate={predicate} onChange={handleChange} />, {
      store,
    });

    userEvent.click(screen.getByRole('button'));
    userEvent.click(screen.getByRole('checkbox', { checked: false }));

    expect(handleChange).toBeCalledWith(
      expect.objectContaining({
        id: 0,
        subject: Subject.CATEGORY_CHANGE,
        condition: RulePredicateCondition.EnumMatches,
        valueId: 151,
      })
    );
  });
});
