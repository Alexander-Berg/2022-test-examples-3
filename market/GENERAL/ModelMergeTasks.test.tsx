import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { api } from 'src/test/singletons/apiSingleton';
import { ModelMergeTasksPage } from './ModelMergeTasks';
import { renderWithReatomStore } from 'src/test/setupTestProvider';
import { User } from 'src/java/definitions';
import { localTasks } from '../../test/data/localTasks';

const users = [
  {
    email: '',
    fullname: 'AutoS_10_Кравченко Татьяна',
    globalRoles: ['OPERATOR'],
    login: '2574kt',
    manager: false,
    managerCategories: [],
    pureLogin: '2574kt',
    staffEmail: '2574kt@yandex-team.ru',
    staffLogin: '2574kt',
    subordinates: [],
    uid: 1,
    yandexEmail: '2574kt@yandex.ru',
  },
] as User[];

const taskFile = new File([new Blob([''], { type: 'text/plain' })], 'машинки_hero_Н_36.xlsx');

const getFilesInput = (container: HTMLElement) => {
  // eslint-disable-next-line testing-library/no-node-access
  const inputs = container.querySelectorAll('input');
  expect(inputs[0]).toBeTruthy();
  return inputs[0];
};

describe('ModelMergeTasksPage::', () => {
  test('full cases', async () => {
    const view = renderWithReatomStore(<ModelMergeTasksPage />);

    // резолв стартовых загрузок
    await waitFor(() => {
      api.userController.getUsers.next().resolve(users);
      api.modelMergeTaskController.getTasks.next().resolve(localTasks);
      api.modelMergeTaskController.countTasks.next().resolve(localTasks.length);
    });

    expect(api.userController.activeRequests()).toHaveLength(0);
    expect(api.modelMergeTaskController.activeRequests()).toHaveLength(0);

    // смотрим что таска отображается
    screen.getByText(localTasks[0].fileName);
    screen.getAllByText(users[0].fullname);

    // проверяем действия в строках
    userEvent.click(screen.getAllByText('Повторить валидацию')[0]);

    expect(api.modelMergeTaskController.validate.activeRequests()).toHaveLength(1);

    userEvent.click(screen.getByText('Схлопнуть'));

    expect(api.modelMergeTaskController.merge.activeRequests()).toHaveLength(1);

    // проверяем загрузку файла
    const fileInput = getFilesInput(view.container);
    userEvent.upload(fileInput, taskFile);

    expect(api.modelMergeTaskController.upload.activeRequests()).toHaveLength(1);
  });
});
