const { baseConfig } = require('./data');

jest.mock('../../../pages/all/all.js', () => ({
    'Auth@desktop': 'authDesktopJSContent',
    'Auth@phone': 'authPhoneJSContent',
    CardSport: 'cardSportJSContent',
    'CardSport@desktop': 'cardSportDesktopJSContent',
    Comments: 'commentsJSContent',
}));

jest.mock('../../../pages/all/all.css', () => ({
    'Auth@desktop': 'authDesktopCSSContent',
    'Auth@phone': 'authPhoneCSSContent',
    CardSport: 'cardSportCSSContent',
    'CardSport@desktop': 'cardSportDesktopCSSContent',
    Comments: 'commentsCSSContent',
}));

jest.mock('../../../pages/all/all.bem.js', () => ({
    desktop: {
        accordion: 'accordionDesktopJSContent',
    },
    'touch-phone': {
        accordion: 'accordionTouchJSContent',
    },
}));

jest.mock('../../../pages/all/all.bem.css', () => ({
    desktop: {
        accordion: 'accordionDesktopCSSContent',
    },
    'touch-phone': {
        accordion: 'accordionTouchCSSContent',
    },
}));

// делаем небольшой трюк: устанавливаем конфиг до запроса assets;
// это нужно потому, что в ряде модулей он статичен и не меняется во время запроса
const runtimeConfig = require('../../../core/assets/runtimeConfig');
runtimeConfig.setConfig(baseConfig);

// получаем assets после установки конфига
const assets = require('../../../core/assets/assets');

describe('AssetsApi', () => {
    afterEach(() => jest.restoreAllMocks());

    describe('bundlesContent', () => {
        describe('desktop', () => {
            beforeEach(() => assets.setPlatform('desktop'));

            it('Возвращает правильный js контент для bem файла', () => {
                expect(assets.getBemBlockJs('accordion')).toEqual('accordionDesktopJSContent');
            });

            it('Возвращает правильный css контент для bem файла', () => {
                expect(assets.getBemBlockCss('accordion')).toEqual('accordionDesktopCSSContent');
            });

            it('Возвращает правильную ссылку на css бандл для bem', () => {
                expect(assets.getBemBlockCssPath('accordion'))
                    .toEqual('/turbo-static-path/_accordion_desktop_hash.css');
            });

            it('Возвращает правильную ссылку на js бандл для bem', () => {
                expect(assets.getBemBlockJsPath('accordion'))
                    .toEqual('/turbo-static-path/_accordion_desktop_hash.js');
            });

            it('Возвращает правильный js контент react файла', () => {
                expect(assets.getReactBlockJs('Auth')).toEqual('authDesktopJSContent');
            });

            it('Возвращает правильный css контент react файла', () => {
                expect(assets.getReactBlockCss('Auth')).toEqual('authDesktopCSSContent');
            });

            it('Возвращает правильную ссылку на css бандл для react', () => {
                expect(assets.getReactBlockCssPath('Auth'))
                    .toEqual('/turbo-static-path/auth@desktop_hash.css');
            });

            it('Возвращает правильную ссылку на js бандл для react', () => {
                expect(assets.getReactBlockJsPath('Auth'))
                    .toEqual('/turbo-static-path/auth@desktop_hash.js');
            });

            describe('При наличии общего и платформенного бандла', () => {
                it('Возвращает правильный js контент react файла', () => {
                    expect(assets.getReactBlockJs('CardSport')).toEqual('cardSportDesktopJSContent');
                });

                it('Возвращает правильный css контент react файла', () => {
                    expect(assets.getReactBlockCss('CardSport')).toEqual('cardSportDesktopCSSContent');
                });

                it('Возвращает правильную ссылку на css бандл для react', () => {
                    expect(assets.getReactBlockCssPath('CardSport'))
                        .toEqual('/turbo-static-path/card-sport@desktop_hash.css');
                });

                it('Возвращает правильную ссылку на js бандл для react', () => {
                    expect(assets.getReactBlockJsPath('CardSport'))
                        .toEqual('/turbo-static-path/card-sport@desktop_hash.js');
                });
            });
        });

        describe('touch-phone', () => {
            beforeEach(() => assets.setPlatform('touch-phone'));

            it('Возвращает правильный js контент для bem файла', () => {
                expect(assets.getBemBlockJs('accordion')).toEqual('accordionTouchJSContent');
            });

            it('Возвращает правильный css контент bem файла', () => {
                expect(assets.getBemBlockCss('accordion')).toEqual('accordionTouchCSSContent');
            });

            it('Возвращает правильную ссылку на css бандл для bem', () => {
                expect(assets.getBemBlockCssPath('accordion'))
                    .toEqual('/turbo-static-path/_accordion_touch_hash.css');
            });

            it('Возвращает правильную ссылку на js бандл для bem', () => {
                expect(assets.getBemBlockJsPath('accordion'))
                    .toEqual('/turbo-static-path/_accordion_touch_hash.js');
            });

            it('Возвращает правильный js контент react файла', () => {
                expect(assets.getReactBlockJs('Auth')).toEqual('authPhoneJSContent');
            });

            it('Возвращает правильный css контент react файла', () => {
                expect(assets.getReactBlockCss('Auth')).toEqual('authPhoneCSSContent');
            });

            it('Возвращает правильную ссылку на css бандл для react', () => {
                expect(assets.getReactBlockCssPath('Auth'))
                    .toEqual('/turbo-static-path/auth@phone_hash.css');
            });

            it('Возвращает правильную ссылку на js бандл для react', () => {
                expect(assets.getReactBlockJsPath('Auth'))
                    .toEqual('/turbo-static-path/auth@phone_hash.js');
            });
        });

        describe('common', () => {
            it('Возвращает правильный js контент react файла', () => {
                expect(assets.getReactBlockJs('Comments')).toEqual('commentsJSContent');
            });

            it('Возвращает правильный css контент react файла', () => {
                expect(assets.getReactBlockCss('Comments')).toEqual('commentsCSSContent');
            });

            it('Возвращает правильную ссылку на css бандл для react', () => {
                expect(assets.getReactBlockCssPath('Comments'))
                    .toEqual('/turbo-static-path/comments_hash.css');
            });

            it('Возвращает правильную ссылку на js бандл для react', () => {
                expect(assets.getReactBlockJsPath('Comments'))
                    .toEqual('/turbo-static-path/comments_hash.js');
            });
        });
    });
});
