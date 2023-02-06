import { FailReason, TaskType } from '@/apollo/generated/graphql';
import { TaskSection } from '@/screens/FinishedTaskListScreen/types';

import { getEmptyText, getTaskGroups } from '../utils';

const tasks = [
  {
    routePointId: '1',
    taskId: '12',
    externalOrderId: '123',
    isMultiOrder: false,
    multiOrderId: '12',
    recipientFio: '',
    deliveryAddress: '',
    isCanBeGrouped: false,
    type: TaskType.OrderDelivery,
    failReason: {
      reason: FailReason.CannotPay,
    },
    ordinalNumber: 1,
  },
  {
    routePointId: '2',
    taskId: '22',
    externalOrderId: '223',
    isMultiOrder: false,
    multiOrderId: '22',
    recipientFio: '',
    deliveryAddress: '',
    isCanBeGrouped: false,
    type: TaskType.OrderDelivery,
    failReason: {
      reason: FailReason.CannotPay,
    },
    ordinalNumber: 2,
  },
  {
    routePointId: '3',
    taskId: '33',
    externalOrderId: '333',
    isMultiOrder: true,
    multiOrderId: '4444',
    recipientFio: '',
    deliveryAddress: '',
    isCanBeGrouped: true,
    type: TaskType.OrderDelivery,
    failReason: {
      reason: FailReason.CannotPay,
    },
    ordinalNumber: 3,
  },
  {
    routePointId: '4',
    taskId: '44',
    externalOrderId: '444',
    isMultiOrder: true,
    multiOrderId: '4444',
    recipientFio: '',
    deliveryAddress: '',
    isCanBeGrouped: true,
    type: TaskType.OrderDelivery,
    failReason: {
      reason: FailReason.CannotPay,
    },
    ordinalNumber: 4,
  },
];

jest.mock('react-native-firebase', () => ({}));
jest.mock('react-native-device-info', () => ({
  getBundleId: jest.fn(),
  getVersion: jest.fn(),
}));

describe('FinishedTaskListScreen/utils/getEmptyText', () => {
  it('Вернет текст для колл-тасок', () => {
    expect(getEmptyText(TaskSection.CallTasks)).toEqual(
      'У вас нет ни одного завершенного звонка',
    );
  });

  it('Вернет текст для тасок по доставке', () => {
    expect(getEmptyText(TaskSection.DeliveryTasks)).toEqual(
      'У вас нет ни одной завершенной доставки',
    );
  });

  it('Вернет дефолтный текст', () => {
    expect(getEmptyText()).toEqual('У вас нет ни одного завершенного задания');
  });
});

describe('FinishedTaskListScreen/utils/getTaskGroups', () => {
  it('вернет пустой массив', () => {
    expect(getTaskGroups()).toEqual([]);
    expect(
      getTaskGroups({
        tasks: {
          tasks: [],
        },
      }),
    ).toEqual([]);
  });

  it('сгруппирует нужные таски', () => {
    expect(
      getTaskGroups({
        tasks: {
          tasks,
        },
      }),
    ).toEqual([[tasks[0]], [tasks[1]], [tasks[2], tasks[3]]]);
  });
});
