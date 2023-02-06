import React from 'react';
import { render, RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AutoRuleField } from './AutoRuleField';
import { simpleMapping, mappingWithStringParam, categoryData } from 'src/test/data';
import { ParamMappingType } from 'src/java/definitions';
import { UiParamMapping } from 'src/utils/types';

const forceMapping = { ...simpleMapping, mappingType: ParamMappingType.FORCE_MAPPING };
const directMapping = { ...mappingWithStringParam, mappingType: ParamMappingType.DIRECT };
const pictureMapping = { ...simpleMapping, mappingType: ParamMappingType.PICTURE };

const clickCheckbox = (app: RenderResult) => {
  userEvent.click(app.getByRole('checkbox'));
};

const checkMappingTypeMock = (type?: ParamMappingType) => {
  return jest.fn((mapping: UiParamMapping) => {
    expect(mapping.mappingType).toBe(type);
  });
};

describe('AutoRuleField', () => {
  test('unchecked FORCE_MAPPING on MAPPING mapping', () => {
    // с FORCE_MAPPING должно измениться на MAPPING, так как тип параметра enum
    const onChange = checkMappingTypeMock(ParamMappingType.MAPPING);

    const app = render(<AutoRuleField mapping={forceMapping} categoryData={categoryData} onChange={onChange} />);
    clickCheckbox(app);

    expect(onChange).toBeCalled();
  });

  test('checked FORCE_MAPPING on PICTURE mapping', () => {
    const onChange = jest.fn();

    const app = render(<AutoRuleField mapping={pictureMapping} categoryData={categoryData} onChange={onChange} />);

    clickCheckbox(app);
    // не должеен меняться у маппинга с картинками
    expect(onChange).not.toBeCalled();
  });

  test('checked FORCE_MAPPING on !editable mapping', () => {
    const onChange = jest.fn();

    const app = render(<AutoRuleField mapping={pictureMapping} categoryData={categoryData} onChange={onChange} />);

    clickCheckbox(app);
    // не должеен меняться у нередактируемых маппингов
    expect(onChange).not.toBeCalled();
  });

  test('checked FORCE_MAPPING on DIRECT mapping', () => {
    const onChange = checkMappingTypeMock(ParamMappingType.FORCE_MAPPING);
    const app = render(<AutoRuleField mapping={directMapping} categoryData={categoryData} onChange={onChange} />);
    clickCheckbox(app);

    expect(onChange).toBeCalled();
  });

  test('unchecked FORCE_MAPPING on DIRECT mapping', () => {
    const onChange = checkMappingTypeMock(ParamMappingType.DIRECT);

    const app = render(
      <AutoRuleField
        mapping={{ ...directMapping, mappingType: ParamMappingType.FORCE_MAPPING }}
        categoryData={categoryData}
        onChange={onChange}
      />
    );

    clickCheckbox(app);

    expect(onChange).toBeCalled();
  });
});
