const forms = require('../services/forms');

it('forms', () => {
    const data = forms(['test']);

    expect(data.test.length).toEqual(2);
    expect(data.test.includes('forms.yandex.ru')).toBeTruthy();
    expect(data.test.includes('forms.yandex.%tld%')).toBeTruthy();
});
