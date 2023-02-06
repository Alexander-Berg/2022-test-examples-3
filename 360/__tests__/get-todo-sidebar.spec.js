jest.mock('../../../filters/todo/get-todo-sidebar');

const getTodoSidebar = require('../get-todo-sidebar');

describe('models:todo -> get-todo-sidebar', () => {
  let coreMock;
  let serviceFn;
  let todoFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    todoFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(todoFn);
  });

  test('должен вызывать сервисы todo и reminders, если передан withReminders', async () => {
    todoFn.mockResolvedValue({});

    await getTodoSidebar({withReminders: true}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(2);
    expect(serviceFn).toHaveBeenCalledWith('todo');
    expect(serviceFn).toHaveBeenCalledWith('reminders');
  });

  test('должен вызывать только сервис todo, если не передан withReminders', async () => {
    todoFn.mockResolvedValue({});

    await getTodoSidebar({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен ходить в ручку todo с нужными параметрами', async () => {
    const params = {a: 1};

    todoFn.mockResolvedValue({});

    await getTodoSidebar(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(`/get-todo-sidebar`, params);
  });
});
