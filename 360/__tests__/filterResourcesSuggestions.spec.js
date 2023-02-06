import filterResourcesSuggestions from '../filterResourcesSuggestions';

describe('filterResourcesSuggestions', () => {
  test('должен возвращать исходный список, если переговрки из него не ещё не добавлены', () => {
    const suggestions = [{email: '1'}, {email: '2'}, {email: '3'}];

    const props = {
      canEdit: true,
      resources: []
    };

    expect(filterResourcesSuggestions(suggestions, props)).toEqual(suggestions);
  });
  test('должен убирать из списка уже добавленные переговорки', () => {
    const suggestions = [{email: '1'}, {email: '2'}, {email: '3'}];

    const props = {
      canEdit: true,
      resources: [{resource: {email: '2'}}]
    };

    expect(filterResourcesSuggestions(suggestions, props)).toEqual([{email: '1'}, {email: '3'}]);
  });
  test('должен фильтровать переговорки с коллизией при редактировании серии и отсутствии прав на редактирование', () => {
    const suggestions = [{email: '1', dueDate: {}}, {email: '2'}, {email: '3'}];

    const props = {
      canEdit: false,
      applyToFuture: true,
      resources: []
    };

    expect(filterResourcesSuggestions(suggestions, props)).toEqual([{email: '2'}, {email: '3'}]);
  });
  test('не должен фильтровать переговорки с коллизией при редактировании серии и наличии прав на редактирование', () => {
    const suggestions = [{email: '1', dueDate: {}}, {email: '2'}, {email: '3'}];

    const props = {
      canEdit: true,
      applyToFuture: true,
      resources: []
    };

    expect(filterResourcesSuggestions(suggestions, props)).toEqual(suggestions);
  });
  test('не должен фильтровать переговорки с коллизией при редактировании инстанса и отсутствии прав на редактирование', () => {
    const suggestions = [{email: '1', dueDate: {}}, {email: '2'}, {email: '3'}];

    const props = {
      canEdit: false,
      applyToFuture: false,
      resources: []
    };

    expect(filterResourcesSuggestions(suggestions, props)).toEqual(suggestions);
  });
});
