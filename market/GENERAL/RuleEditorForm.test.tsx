import React, { FC } from 'react';

import { shopModel, simpleMappingWithRule, categoryData, vendorParameter } from 'src/test/data';
import { RuleEditorForm, RuleEditorFormProps } from './RuleEditorForm';
import { setupWithReatom, useDescribeMapping, useDescribeCategoryData } from 'src/test/withReatom';
import { UiParamMapping } from 'src/utils/types';

const TestApp: FC<Partial<RuleEditorFormProps> & { mappings: UiParamMapping[] }> = ({ mappings, onClose }) => {
  useDescribeMapping(mappings);
  useDescribeCategoryData(categoryData);
  return (
    <RuleEditorForm model={shopModel} parameter={vendorParameter} categoryData={categoryData} onClose={onClose!} />
  );
};

describe('RuleEditorForm', () => {
  test('render with editable mapping', () => {
    const onClose = jest.fn();
    const { app } = setupWithReatom(<TestApp onClose={onClose} mappings={[simpleMappingWithRule]} />);
    expect(app.getAllByText(/king/i)).toHaveLength(2);
    app.getByText(/vendor/i);
  });

  test('render with no editable mapping ', () => {
    const onClose = jest.fn();
    const { app } = setupWithReatom(
      <TestApp onClose={onClose} mappings={[{ ...simpleMappingWithRule, editable: false }]} />
    );

    expect(app.queryAllByAltText(/king/i)).toHaveLength(0);
    expect(app.queryByText(/vendor/i)).toBeFalsy();
  });
});
