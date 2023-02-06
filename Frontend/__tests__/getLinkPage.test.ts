import { getLinkPage } from 'sport/lib/contexts/helpers/getLinkPage';
import { ELinkPageType } from 'sport/lib/contexts/helpers/ELinkPageType';

describe('getLinkPage', () => {
  it('Возвращает external, если была передана ссылка на внешний ресурс', ()=> {
    const href = 'https://aif.ru';
    const result = getLinkPage(href);
    expect(result).toBe(result);
  });

  it('Возвращает external, если была передана некоректная ссылка', () => {
    const href = 'https:/yandex.ru';
    const result = getLinkPage(href);
    expect(result).toBe(ELinkPageType.EXTERNAL);
  });

  it('Возвращает index, eсли была передана главная страница спорта', () => {
    const href = 'https://yandex.ru/sport/';
    const result = getLinkPage(href);
    expect(result).toBe(result);
  });

  it('Возвращает rubric, если была передана ссылка на страницу рубрики', () => {
    const href = 'https://yandex.ru/sport/rubric/hockey';
    const result = getLinkPage(href);
    expect(result).toBe(ELinkPageType.RUBRIC);
  });

  it('Возвращает story, если была передана ссылка на сюжет', () => {
    const href = 'https://yandex.ru/sport/story/a--4dd4423a25c862b7226689045a6dd3e7?lang=ru&wan=1&stid=B3O2HXanZmaMAR60j99e&t=1650354004&persistent_id=192631682&story=609265d5-0711-5edb-86b4-7f51d1b65856';
    const result = getLinkPage(href);
    expect(result).toBe(ELinkPageType.STORY);
  });

  it('Возвращает external, если была передана пустая ссылка', () => {
    const href = '';
    const result = getLinkPage(href);
    expect(result).toBe(ELinkPageType.EXTERNAL);
  });
});
