import {
  CallTaskStatus,
  Task,
  TaskStatus,
  TaskType,
} from '@/apollo/generated/graphql';
import {
  extractActiveDeliveryTasks,
  extractFailedDeliveryTasks,
  extractSuccessfulDeliveryTasks,
} from '@/utils/tasks';

jest.mock('@/nativeModules/crashlytics', () => ({}));

const deliveryTaskStatuses = [
  TaskStatus.NotDelivered,
  TaskStatus.Delivered,
  TaskStatus.DeliveryFailed,
];

const generateDeliveryTaskListOfSize = (size: number) => {
  const taskList: Task[] = [];
  const numberOfStatuses = deliveryTaskStatuses.length;

  for (let i = 0; i < size; ++i) {
    taskList.push({
      isMultiOrder: false,
      id: i.toString(),
      multiOrderId: i.toString(),
      name: i.toString(),
      type: TaskType.OrderDelivery,
      status: deliveryTaskStatuses[i % numberOfStatuses],
      callStatus: CallTaskStatus.NotCalled,
      callAttemptCount: 1,
    });
  }

  return taskList;
};

describe('Global utils / tasks', () => {
  const mockedTaskList: Task[] = generateDeliveryTaskListOfSize(100);

  describe('extractActiveDeliveryTasks', () => {
    const activeStatuses = [TaskStatus.NotDelivered];

    it('Должен отдавать массив активных заданий для переданного массива заданий с разными статусами', () => {
      const result = extractActiveDeliveryTasks(mockedTaskList);
      for (const task of result) {
        const { status } = task;
        expect(activeStatuses).toContain(status);
      }
    });

    it('Должен отдавать пустой массив, если передан пустой массив', () => {
      expect(extractActiveDeliveryTasks([])).toEqual([]);
    });
  });

  describe('extractFailedDeliveryTasks', () => {
    it('Должен отдавать массив невыполненных заданий для переданного массива заданий с разными статусами', () => {
      const result = extractFailedDeliveryTasks(mockedTaskList);

      for (const task of result) {
        const { status } = task;
        expect(status).toEqual(TaskStatus.DeliveryFailed);
      }
    });

    it('Должен отдавать пустой массив, если передан пустой массив', () => {
      expect(extractFailedDeliveryTasks([])).toEqual([]);
    });
  });

  describe('extractSuccessfulDeliveryTasks', () => {
    it('Должен отдавать массив выполненных заданий для переданного массива заданий с разными статусами', () => {
      const result = extractSuccessfulDeliveryTasks(mockedTaskList);

      for (const task of result) {
        const { status } = task;
        expect(status).toEqual(TaskStatus.Delivered);
      }
    });

    it('Должен отдавать пустой массив, если передан пустой массив', () => {
      expect(extractSuccessfulDeliveryTasks([])).toEqual([]);
    });
  });
});
