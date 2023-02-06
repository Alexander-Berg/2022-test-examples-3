const reorderTodoItems = require('../do-reorder-todo-items');

describe('models:todo -> do-reorder-todo-items', () => {
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

    reorderTodoItems({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен ходить в ручку todo с нужными параметрами', () => {
    const params = {a: 1};

    todoFn.mockResolvedValue({});

    reorderTodoItems(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(`/reorder-todo-items`, params);
  });
});
