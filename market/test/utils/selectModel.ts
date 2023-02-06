import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';
import { MockedApi } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';

import { ModelForm } from 'src/tasks/common-logs/widgets/ModelForm/ModelForm';
import { ModelSelect } from 'src/shared/common-logs/components/ModelSelect/ModelSelect';
import { checkIfModelIsSku } from 'src/shared/common-logs/helpers/checkIfModelIsSku';
import { AliasMaker } from 'src/shared/services';

import cssSuggestSection from 'src/shared/components/ClosableTitle/ClosableTitle.module.scss';

const CLASS_NAME_PARAM_VALUE = `.${cssSuggestSection.Line}`;

export const selectModel = (
  app: ReactWrapper,
  aliasMaker: MockedApi<AliasMaker>,
  model: ProtoModel,
  skus?: ProtoModel[]
) => {
  app.find(ModelSelect).props().onSelect(model.id!);

  aliasMaker.getModels.next().resolve({
    model: [model],
    result: { status: OperationStatus.SUCCESS },
  });

  aliasMaker.getVendors.next().resolve({
    vendor: [{ vendor_id: model.vendor_id! }],
    result: { status: OperationStatus.SUCCESS },
  });

  /*
    get images from searches started every time when new model is selected,
    so for clear it from active requests we should write next line
  */
  aliasMaker.getImagesFromSearch.process(request => {
    request.resolve({
      image: [],
      result: {
        status: OperationStatus.SUCCESS,
      },
    });
  });

  app.update();

  const hasChildRelations = model.relations?.some(relation => relation.type === RelationType.SKU_MODEL);

  if (!checkIfModelIsSku(model) && hasChildRelations) {
    aliasMaker.getModels.next().resolve({
      model: skus,
      result: { status: OperationStatus.SUCCESS },
    });
    app.update();
  }

  expect(app.find(ModelForm).find(CLASS_NAME_PARAM_VALUE).at(0).text().trim()).toBe(model.titles![0].value);
};
