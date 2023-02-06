jest.mock('../../../filters/todo/do-create-todo-list');

const filter = require('../../../filters/todo/do-create-todo-list');
const createTodoList = require('../do-create-todo-list');

describe('models:todo -> do-create-todo-list', () => {
  let coreMock;
  let serviceFn;
  let todoFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    filter.mockClear();
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

    createTodoList({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('todo');
  });

  test('должен возвращать ответ от todo, пропущенный через фильтр', async () => {
    const params = {};
    const response = [1, 2, 3];
    const filterResult = [1];

    filter.mockReturnValue(filterResult);
    todoFn.mockResolvedValue(response);

    expect(await createTodoList(params, coreMock)).toEqual(filterResult);
  });
});
