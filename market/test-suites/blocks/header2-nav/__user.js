import {makeSuite, makeCase} from 'ginny';

const ORIGINAL_WINDOW_SIZE_KEY = Symbol('ORIGINAL_WINDOW_SIZE');

/**
 * Тесты на иконку пользователя в меню блока header2-nav
 * @param {PageObject.Header2Nav} headerNav
 */

export default makeSuite('Аватар пользователя', {
    environment: 'testing',
    story: {
        'По-умолчанию': {
            params: {
                windowWidth: 'Ширина окна',
                windowHeight: 'Высота окна',
            },

            async beforeEach() {
                const {windowWidth, windowHeight} = this.params;
                this[ORIGINAL_WINDOW_SIZE_KEY] = await this.browser.windowHandleSize();

                await this.browser.windowHandleSize({width: windowWidth, height: windowHeight});
            },

            виден: makeCase({
                async test() {
                    return this.headerNav.userIcon.isVisibleWithinViewport()
                        .should.eventually.equal(true, 'Аватар пользователя виден');
                },
            }),

            async afterEach() {
                await this.browser.windowHandleSize(this[ORIGINAL_WINDOW_SIZE_KEY].value);
            },
        },
    },
});
