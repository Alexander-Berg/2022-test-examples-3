import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import {
  Model,
  ModelType,
  OperationStatusType,
  OperationType,
} from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { ModelEditorPopup } from 'src/tasks/common-logs/widgets/ModelEditorPopup/ModelEditorPopup';
import { ModelForm } from 'src/tasks/common-logs/widgets/ModelForm/ModelForm';
import { testModelProto } from 'src/shared/test-data/test-models';
import { testVendor } from 'src/shared/test-data/test-vendor';
import { wait } from 'src/shared/utils/testing/utils';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';
import { selectVendor } from 'src/tasks/common-logs/test/utils/selectVendor';
import minimalTask from 'src/tasks/common-logs/task-suits/minimal-task-data';

import cssSuggestSection from 'src/shared/components/ClosableTitle/ClosableTitle.module.scss';

const CLASS_NAME_PARAM_VALUE = `.${cssSuggestSection.Line}`;
const success = { status: { status: OperationStatus.SUCCESS } };

describe('all models exists and is SKU', () => {
  it('should process successfully', async () => {
    const { app, aliasMaker } = initCommonLogsApp({ initialData: minimalTask });

    const vendor = testVendor({ vendor_id: 100, name: 'Some Vendor' });
    const offer = minimalTask.logs[0];
    selectVendor(app, aliasMaker, vendor);

    const createModelBtn = app
      .find(ModelForm)
      .find('button')
      .findWhere(element => element.type() === 'button' && element.text() === 'Создать модель');

    expect(createModelBtn).toHaveLength(1);

    createModelBtn.simulate('click');

    const popup = app.find(ModelEditorPopup);

    expect(popup).toHaveLength(1);

    const saveModelBtn = popup
      .find('button')
      .findWhere(element => element.type() === 'button' && element.text() === 'Сохранить');

    expect(saveModelBtn).toHaveLength(1);

    saveModelBtn.simulate('click');

    await wait(1);

    const model: Model = testModelProto({
      id: 1000,
      modelType: ModelType.SKU,
      vendorId: vendor.vendor_id,
      published: true, // this flag needed for getModelsExported check
    });

    aliasMaker.updateModelsGroup.next().resolve({
      model: [model],
      result: {
        status: OperationStatus.SUCCESS,
        mbo_status: [
          {
            status: OperationStatusType.OK,
            type: OperationType.CREATE,
          },
        ],
      },
      reqId: 'test',
    });

    await wait(1);
    app.update();

    expect(app.find(ModelEditorPopup)).toHaveLength(0);

    aliasMaker.getUpdatedMatching.next().resolve({ task_offer: [offer], ...success });

    expect(app.find(ModelForm).find(CLASS_NAME_PARAM_VALUE).at(0).text().trim()).toBe(model.titles![0].value);
  });
});
