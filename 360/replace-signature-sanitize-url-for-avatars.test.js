'use strict';

const replaceSignatureSanitizeUrlForAvatars = require('./replace-signature-sanitize-url-for-avatars');

let core;

beforeEach(() => {
    core = {
        console: {
            error: jest.fn()
        }
    };
});

test('должен развернуть ссылки на картинки из ресайзера, если она из аватарницы и не разворачивать, ' +
    'если они не из аватарницы', () => {
    const sign = {
        text: '<div>--&nbsp;</div><div>' +
            '<img src="https://resize.yandex.net/mailservice?' +
            'url=https%3A%2F%2Favatars.mdst.yandex.net%2Fget-yapic%2F' +
            '1824%2F22b019c9170c71e1213f27d594e382361485917831%2Forig%3F' +
            'yandex_class%3Dyandex_new_inline_el_2%402x.png&amp;' +
            'proxy=yes&amp;key=391ca099d720e5dd1cdecad63c53ca5d">' +
            '<img src="https://resize.yandex.net/mailservice?' +
            'url=https%3A%2F%2Favatars.mdst.yandex.net%2Fget-yapic%2F' +
            '1450%2Fcd6c15b5baa64b0fb722144c49fb1a2f1485917834%2F' +
            'orig%3Fyandex_class%3Dyandex_new_inline_el_2%402x.png&amp;' +
            'proxy=yes&amp;key=7943c71d730bf3dbe353dabd049167a6">' +
            '<img src="https://resize.yandex.net/mailservice?url=https%3A%2F%2F' +
            'avatars.mdst.yandex.net%2Fget-yapic%2F1824%2F7e59832d0601281864f498c2364f907f1485917836%2F' +
            'orig%3Fyandex_class%3Dyandex_new_inline_el_2%402x.png&amp;proxy=yes&amp;' +
            'key=2da2d078066acb7fa999beed97e023ff">' +
            '<img src="https://resize.yandex.net/mailservice?url=http%3A%2F%2F' +
            'picfun.ru%2Fwp-content%2Fuploads%2FHTxyUcwXfw.jpg&amp;proxy=yes&amp;' +
            'key=272eee0294a3aaba00948158ca7dad27"></div>'
    };

    const resultSignText = '<div>--&nbsp;</div><div>' +
        '<img src="https://avatars.mdst.yandex.net/get-yapic/1824/' +
        '22b019c9170c71e1213f27d594e382361485917831/orig?' +
        'yandex_class=yandex_new_inline_el_2@2x.png">' +
        '<img src="https://avatars.mdst.yandex.net/get-yapic/1450/' +
        'cd6c15b5baa64b0fb722144c49fb1a2f1485917834/orig?' +
        'yandex_class=yandex_new_inline_el_2@2x.png">' +
        '<img src="https://avatars.mdst.yandex.net/get-yapic/1824/' +
        '7e59832d0601281864f498c2364f907f1485917836/orig?yandex_class=yandex_new_inline_el_2@2x.png">' +
        '<img src="https://resize.yandex.net/mailservice?url=http%3A%2F%2Fpicfun.ru%2F' +
        'wp-content%2Fuploads%2FHTxyUcwXfw.jpg&amp;proxy=yes&amp;key=272eee0294a3aaba00948158ca7dad27">' +
        '</div>';

    replaceSignatureSanitizeUrlForAvatars(core, sign);

    expect(sign.text).toEqual(resultSignText);
});

test('должен не разворачивать картинку, если она не правильно закодирована и залогировать ошибку', () => {
    const initialSignText = (
        '<div>--&nbsp;</div><div>' +
        '<img src="https://resize.yandex.net/mailservice?' +
        'url=https%3A%2F%2Favatars.mdst.yandex.net%2Fget-yapic%2F' +
        '1824%2F22b019c9170c71e1213f27d594e382361485917831%2Forig%3F' +
        'yandex_class%3DR_OTX%%60XDIIE%7D6%7B@LFYCN8E.jpg&amp;' +
        'proxy=yes&amp;key=391ca099d720e5dd1cdecad63c53ca5d">' +
        '<img src="https://resize.yandex.net/mailservice?url=http%3A%2F%2F' +
        'picfun.ru%2Fwp-content%2Fuploads%2FHTxyUcwXfw.jpg&amp;proxy=yes&amp;' +
        'key=272eee0294a3aaba00948158ca7dad27"></div>'
    );
    const sign = { text: initialSignText };

    replaceSignatureSanitizeUrlForAvatars(core, sign);

    expect(sign.text).toEqual(initialSignText);

    expect(core.console.error).toBeCalledTimes(1);
    expect(core.console.error).toBeCalledWith('SIGNATURE_IMAGE_LINK_DECODE_ERROR', {
        message: 'URI malformed',
        link: 'https://resize.yandex.net/mailservice?' +
            'url=https%3A%2F%2Favatars.mdst.yandex.net%2Fget-yapic%2F' +
            '1824%2F22b019c9170c71e1213f27d594e382361485917831%2Forig%3F' +
            'yandex_class%3DR_OTX%%60XDIIE%7D6%7B@LFYCN8E.jpg&amp;' +
            'proxy=yes&amp;key=391ca099d720e5dd1cdecad63c53ca5d'
    });
});
