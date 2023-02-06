import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';

import { TaskType, MappingStatuses } from 'src/shared/common-logs/helpers/types';
import { Carousel } from 'src/tasks/common-logs/components/Carousel/Carousel';
import { Scene } from 'src/tasks/common-logs/components/Scene/Scene';
import { StatusButtons } from 'src/tasks/common-logs/components/StatusButtons';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';
import { switchOnOffer } from 'src/tasks/common-logs/test/utils/switchOnOffer';
import { CheckBox } from 'src/shared/components';

describe('psku task test', () => {
  it('all deleted offers processing', async () => {
    const { app } = initCommonLogsApp({
      initialData: {
        ...simpleTask,
        task_type: TaskType.MSKU_FROM_PSKU_GENERATION,
        logs: simpleTask.logs.map(item => {
          return {
            ...item,
            deleted: true,
          };
        }),
      },
    });

    expect(app.find(Scene).text()).toContain(
      'У оферов обновился статус и они более недоступны. Нажмите кнопку «Отправить» для завершения задания'
    );
  });
  it('some deleted offers processing', async () => {
    const deletedOffer = { ...simpleTask.logs[0], deleted: true };
    const otherOffers = simpleTask.logs.slice(1, simpleTask.logs.length);
    const allOffers = [deletedOffer, ...otherOffers];

    const { app, aliasMaker, submitHolder, store } = initCommonLogsApp({
      initialData: {
        ...simpleTask,
        task_type: TaskType.MSKU_FROM_PSKU_GENERATION,
        logs: allOffers,
      },
    });

    // deleted offer is not selectable
    expect(app.find(Carousel).find(CheckBox).at(1).prop('disabled')).toBeTrue();

    // try to submit task
    const state = store.getState();
    state.data!.taskOfferIds!.forEach(item => {
      if (item === deletedOffer.offer_id) {
        return;
      }
      switchOnOffer(app, item);
      app
        .find(StatusButtons)
        .findWhere(_item => _item.text() === 'Не та категория')
        .at(0)
        .simulate('click');
    });

    const awaitingTaskResult: Promise<any> = submitHolder.submit();

    aliasMaker.getModelsExported.next().resolve({
      model: [],
      result: { status: OperationStatus.SUCCESS },
    });

    const { task_result } = await awaitingTaskResult;

    const resultForDeletedOffer = task_result.find((item: any) => item.offer_id === deletedOffer.offer_id);

    expect(resultForDeletedOffer.offer_mapping_status).toBe(MappingStatuses.UNDEFINED);
  });
});
