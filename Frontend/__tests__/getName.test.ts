import { getName } from '../getName';

describe('getName', () => {
  it('Возвращает nameShort.ru, если есть подходящее поле', () => {
    const name = getName(
      {
        ru: 'nameRu',
        en: 'nameEn',
      }, {
        ru: 'shortNameRu',
        en: 'shortNameEn',
      },
    );
    expect(name).toBe('shortNameRu');
  });

  it('Возвращает name.ru, если нет более подходящего поля', () => {
    const name = getName({
      ru: 'nameRu',
      en: 'nameEn',
    });
    expect(name).toBe('nameRu');
  });

  it('Возвращает nameShort.en, если нет более подходящего поля', () => {
    const name = getName({
      ru: '',
      en: 'nameEn',
    }, {
      ru: '',
      en: 'shortNameEn',
    });
    expect(name).toBe('shortNameEn');
  });

  it('Возвращает name.en, если нет более подходящего поля', () => {
    const name = getName({
      ru: '',
      en: 'nameEn',
    });
    expect(name).toBe('nameEn');
  });

  it('Возвращает пустую строку, если переводов нет', () => {
    const name = getName();
    expect(name).toBe('');
  });
});
