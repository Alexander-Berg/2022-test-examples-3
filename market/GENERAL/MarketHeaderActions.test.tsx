import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { parameter } from 'src/test/data';
import { MarketHeaderActions } from './MarketHeaderActions';
import { CHANGE_RULE_TITLE } from './ManualRuleMakerAction';
import { MASS_EDITOR_TITLE } from './MassMarketEditorAction';
import { MAPPING_EDITOR_TITLE } from './MappingEditorAction';
import { PIN_COLUMN_TITLE } from './PinColumnAction';

describe('<MarketHeaderActions />', () => {
  test('render with readonly', () => {
    const { app } = setupWithReatom(<MarketHeaderActions parameter={parameter} readonly />);

    expect(app.queryByTitle(CHANGE_RULE_TITLE)).toBeFalsy();
    expect(app.queryByTitle(MASS_EDITOR_TITLE)).toBeFalsy();
    expect(app.queryByTitle(MAPPING_EDITOR_TITLE)).toBeFalsy();

    app.getByTitle(PIN_COLUMN_TITLE);
  });
});
