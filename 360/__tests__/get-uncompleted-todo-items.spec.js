const getUncompletedTodoItems = require('../get-uncompleted-todo-items');
const TodoItem = require('../../../filters/todo/todo-item');

describe('models:todo -> get-uncomoleted-todo-items', () => {
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
    todoFn.mockResolvedValue({'todo-items': []});

    await getUncompletedTodoItems({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен ходить в ручку todo с нужными параметрами', async () => {
    const dueFrom = 123;
    const dueTo = 567;
    const params = {dueFrom, dueTo};

    todoFn.mockResolvedValue({'todo-items': []});

    await getUncompletedTodoItems(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(
      `/get-todo-items`,
      {
        'due-from': params.dueFrom,
        'due-to': params.dueTo,
        'only-not-completed': true
      },
      {
        retryOnTimeout: 1
      }
    );
  });

  test('должен возвращать ответ от todo, пропущенный через фильтр', async () => {
    const dueFrom = 123;
    const dueTo = 567;
    const params = {dueFrom, dueTo};
    const items = [1, 2, 3];
    const response = {'todo-items': items};

    todoFn.mockResolvedValue(response);

    const modelResult = await getUncompletedTodoItems(params, coreMock);

    expect(todoFn).toHaveBeenCalledTimes(1);
    expect(todoFn).toHaveBeenCalledWith(
      `/get-todo-items`,
      {
        'due-from': params.dueFrom,
        'due-to': params.dueTo,
        'only-not-completed': true
      },
      {
        retryOnTimeout: 1
      }
    );

    expect(modelResult).toEqual(items.map(item => new TodoItem(item)));
  });
});
