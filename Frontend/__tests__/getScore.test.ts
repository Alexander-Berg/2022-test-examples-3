import { getScore } from '../getScore';

describe('getScore', () => {
  it('показывает нулевой результат матча, если подаем пустой результат', () => {
    const count = getScore();
    expect(count).toEqual({ result: [null, null], resultAdditional: [null, null] });
  });

  it('показывает результат матча, если подаем ненулевой результат двух команд', () => {
    const count = getScore([{
      result: 1,
      result_additional: 15,
    }, {
      result: 1,
      result_additional: 1,
    }]);
    expect(count).toEqual({ result: [1, 1], resultAdditional: [15, 1] });
  });
  it('показывает нулевой результат матча, если подаем ненулевой результат трех команд', () => {
    const count = getScore([{
      result: 1,
      result_additional: 15,
    }, {
      result: 1,
      result_additional: 1,
    }, {
      result: 1,
      result_additional: 1,
    }]);
    expect(count).toEqual({ result: [null, null], resultAdditional: [null, null] });
  });
});
