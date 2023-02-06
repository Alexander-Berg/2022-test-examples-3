import makeActionPatternChecker from '../makeActionPatternChecker';

describe('utils/makeActionPatternChecker', () => {
  test('должен проверять совпадение типа экшна', () => {
    const actionType = 'pew';
    const patternChecker = makeActionPatternChecker(actionType);

    expect(patternChecker({type: actionType})).toBe(true);
    expect(patternChecker({type: 'actionType'})).toBe(false);
  });

  test('должен проверять наличие типа экшна', () => {
    const actionType = 'pew';
    const patternChecker = makeActionPatternChecker(actionType);

    expect(patternChecker({})).toBe(false);
  });

  test('должен игнорировать тип экшна, если его не передали', () => {
    const patternChecker = makeActionPatternChecker();

    expect(patternChecker({type: 'actionType'})).toBe(true);
    expect(patternChecker({})).toBe(true);
  });

  test('должен проверять наличие payload, если передали', () => {
    const patternChecker = makeActionPatternChecker(null, {});

    expect(patternChecker({payload: {}})).toBe(true);
    expect(patternChecker({})).toBe(false);
  });

  test('должен проверять нужные поля payload, если передали', () => {
    const fieldValue = Symbol();
    const payloadCheckers = {field: fieldValue};
    const patternChecker = makeActionPatternChecker(null, payloadCheckers);

    expect(patternChecker({payload: {}})).toBe(false);
    expect(patternChecker({payload: {field: fieldValue}})).toBe(true);
  });

  test('должен проверять наличие meta, если передали', () => {
    const patternChecker = makeActionPatternChecker(null, null, {});

    expect(patternChecker({meta: {}})).toBe(true);
    expect(patternChecker({})).toBe(false);
  });

  test('должен проверять нужные поля meta, если передали', () => {
    const fieldValue = Symbol();
    const patternChecker = makeActionPatternChecker(null, null, {field: fieldValue});

    expect(patternChecker({meta: {}})).toBe(false);
    expect(patternChecker({meta: {field: fieldValue}})).toBe(true);
  });

  test('должен запускать функциональные чекеры', () => {
    const fieldValue = Symbol();
    const checker = value => value === fieldValue;
    const patternChecker = makeActionPatternChecker(null, null, {field: checker});

    expect(patternChecker({meta: {field: Symbol()}})).toBe(false);
    expect(patternChecker({meta: {field: fieldValue}})).toBe(true);
  });
});
