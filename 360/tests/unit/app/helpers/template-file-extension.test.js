import { getFileExtensions } from '../../../../app/helpers/template-file-extension';

it('app/helpers/template-file-extension', () => {
    expect(getFileExtensions({ lang: 'ru' })).toEqual('.ru');
    expect(getFileExtensions({ lang: 'en' })).toEqual('.en');
    expect(getFileExtensions({ lang: 'uk' })).toEqual('.uk');
    expect(getFileExtensions({ lang: 'tr' })).toEqual('.tr');

    expect(getFileExtensions({ lang: 'ru', isTouch: true })).toEqual('.touch.ru');
    expect(getFileExtensions({ lang: 'en', isTouch: true })).toEqual('.touch.en');
    expect(getFileExtensions({ lang: 'uk', isTouch: true })).toEqual('.touch.uk');
    expect(getFileExtensions({ lang: 'tr', isTouch: true })).toEqual('.touch.tr');

    expect(getFileExtensions({ lang: 'ru', isApp: true })).toEqual('.app.ru');
    expect(getFileExtensions({ lang: 'en', isApp: true })).toEqual('.app.en');
    expect(getFileExtensions({ lang: 'uk', isApp: true })).toEqual('.app.uk');
    expect(getFileExtensions({ lang: 'tr', isApp: true })).toEqual('.app.tr');
    expect(getFileExtensions({ lang: 'ru', isApp: 'да' })).toEqual('.app.ru');

    expect(getFileExtensions({ lang: 'ru', isApp: true, isTouch: true })).toEqual('.app.ru');
    expect(getFileExtensions({ lang: 'en', isApp: true, isTouch: true })).toEqual('.app.en');
    expect(getFileExtensions({ lang: 'uk', isApp: true, isTouch: true })).toEqual('.app.uk');
    expect(getFileExtensions({ lang: 'tr', isApp: true, isTouch: true })).toEqual('.app.tr');
    expect(getFileExtensions({ lang: 'ru', isApp: 'да', isTouch: 'да' })).toEqual('.app.ru');

    expect(() => getFileExtensions({ lang: 'kz' })).toThrowError('Incorrect lang. Got kz');
    expect(() => getFileExtensions({})).toThrowError('Incorrect lang. Got undefined');
});
