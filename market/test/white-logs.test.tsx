import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';

import { TaskType, OfferFinalStatus, DeepMatcherTaskOutputData } from 'src/shared/common-logs/helpers/types';
import { wait } from 'src/shared/utils/testing/utils';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';
import { selectStatus } from 'src/tasks/common-logs/test/utils/selectStatus';
import { switchOnOffer } from 'src/tasks/common-logs/test/utils/switchOnOffer';

describe('testing tasks on white logs', () => {
  it('test POSTPONE status', async () => {
    const { app, submitHolder, aliasMaker } = initCommonLogsApp({
      initialData: {
        ...simpleTask,
        task_type: TaskType.WHITE_LOGS,
      },
    });
    const offersIds = simpleTask.logs.map(item => item.offer_id);
    const predefinedStatuses = ['Отложить', ...offersIds.slice(1).map(() => 'Не та категория')];

    offersIds.forEach((offerId, index) => {
      switchOnOffer(app, offerId);
      selectStatus(app, predefinedStatuses[index]);
    });

    const awaitTaskResult = submitHolder.submit();
    await wait(1);
    aliasMaker.getAuditActions.next().resolve({ result: { status: OperationStatus.SUCCESS } });
    const { task_result } = (await awaitTaskResult) as DeepMatcherTaskOutputData;
    const resultStatuses = [OfferFinalStatus.POSTPONE, ...offersIds.slice(1).map(() => OfferFinalStatus.TRASH)];
    expect(task_result!.every((item, index) => item.final_status === resultStatuses[index])).toBeTrue();
  });
});
