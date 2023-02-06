import { ModelForm } from '@yandex-market/market-proto-dts/Market/Mbo/ModelForms';
import { Category, Parameter } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { RUSSIAN_LANG_ID } from 'src/shared/constants';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { removeEmpty } from 'src/shared/utils/array-utils';
import { ModelFormTabs } from 'src/tasks/mapping-moderation/helpers/xsl-names';

export interface TestCategorySetup {
  id?: number;
  parameters?: Parameter[];
}

let nextId = 1;

export function testCategoryProto(setup: TestCategorySetup = {}): Category {
  const { id = nextId++, parameters = [testParameterProto({})] } = setup;

  return {
    hid: id,
    parameter: parameters,
    name: [{ name: `Category #${id}`, lang_id: RUSSIAN_LANG_ID }],
  };
}

export function testFormProto(category: Category): ModelForm {
  return {
    published: true,
    category_id: category.hid,
    form_tabs: [
      {
        name: ModelFormTabs.PARAMETERS_TAB_TITLE,
        blocks: [{ name: 'Params', properties: removeEmpty((category.parameter || []).map(p => p.xsl_name)) }],
      },
    ],
  };
}
