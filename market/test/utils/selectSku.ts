import { Tab } from '@yandex-market/mbo-components';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';
import { ReactWrapper } from 'enzyme';
import { MockedApi } from '@yandex-market/mbo-test-utils';

import { AliasMaker } from 'src/shared/services';
import { CheckBox } from 'src/shared/components';
import { ContextMenuItem } from 'src/tasks/common-logs/components/ModelEditor/types';
import { SkuRow } from 'src/tasks/common-logs/components/SkuEditor/SkuTable/SkuRow';

export const selectSku = (app: ReactWrapper, aliasMaker: MockedApi<AliasMaker>, sku: ProtoModel) => {
  app
    .find(Tab)
    .findWhere(item => item.prop('value') === ContextMenuItem.SKU)
    .first()
    .simulate('click');

  const onChange = app
    .find(SkuRow)
    .findWhere(item => item.key() === String(sku.id))
    .first()
    .find(CheckBox)
    .prop('onChange');

  if (onChange) {
    onChange({} as any);
  }
};
