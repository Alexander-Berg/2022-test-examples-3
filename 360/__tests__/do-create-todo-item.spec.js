jest.mock('../../../filters/todo/todo-item');

const createTodoItem = require('../do-create-todo-item');

describe('models:todo -> do-create-todo-item', () => {
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

  test('должен вызывать сервис todo', async () => {
    todoFn.mockResolvedValue({});

    await createTodoItem({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен ходить в ручку todo с нужными параметрами', async () => {
    const dueDate = 123;
    const params = {dueDate};

    todoFn.mockResolvedValue({});

    await createTodoItem(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(`/create-todo-item`, {'due-date': dueDate});
  });
});
