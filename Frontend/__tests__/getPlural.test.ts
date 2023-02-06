import { getMedalsPlural } from '../getPlural';

describe('getMedalsPlural', () => {
  it('Возвращает верное окончание в именительном падеже числительного', () => {
    const count = getMedalsPlural(1);
    expect(count).toBe('медаль');
  });

  it('Возвращает верное окончание в винительном падеже при отправке числительного', () => {
    const count = getMedalsPlural(22);
    expect(count).toBe('медали');
  });

  it('Возвращает верное окончание в родительном падеже при отправке числа 5', () => {
    const count = getMedalsPlural(108);
    expect(count).toBe('медалей');
  });
});
