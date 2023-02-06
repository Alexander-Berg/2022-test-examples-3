const updateTodoItem = require('../do-update-todo-item');

describe('models:todo -> do-update-todo-item', () => {
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

  test('должен вызывать сервис todo', () => {
    todoFn.mockResolvedValue({});

    updateTodoItem({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен ходить в ручку todo с нужными параметрами', () => {
    const dueDate = 123;
    const params = {dueDate};

    todoFn.mockResolvedValue({});

    updateTodoItem(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(`/update-todo-item`, {'due-date': dueDate});
  });
});
