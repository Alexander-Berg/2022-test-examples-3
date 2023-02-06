import { TaskType } from 'src/shared/common-logs/helpers/types';
import { Loading } from 'src/shared/components';
import { wait } from 'src/shared/utils/testing/utils';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';

// behavior from https://st.yandex-team.ru/MBO-22540
describe('async matching test', () => {
  it('async matching in blue logs', async () => {
    const { app } = await initCommonLogsApp({
      initialData: { ...simpleTask, task_type: TaskType.BLUE_LOGS },
      isAsyncMatching: true,
    });

    await wait(1);
    app.update();

    expect(app.find('.BigLoader').find(Loading).length).toEqual(0);
    expect(app.find('.HeaderWrapper').find(Loading).length).toEqual(1);
    expect(app.find('.HeaderWrapper').find(Loading).html()).toContain('Загружаются данные матчинга и скутчинга...');
  });

  it('async matching in white logs', async () => {
    const { app } = await initCommonLogsApp({
      initialData: { ...simpleTask, task_type: TaskType.WHITE_LOGS },
      isAsyncMatching: true,
    });

    await wait(1);
    app.update();

    expect(app.find('.HeaderWrapper').find(Loading).length).toEqual(1);
    expect(app.find('.HeaderWrapper').find(Loading).html()).toContain('Загружаются данные матчинга и скутчинга...');
  });
});
