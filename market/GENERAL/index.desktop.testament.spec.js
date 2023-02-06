// @flow
// flowlint-next-line untyped-import: off
import {waitFor} from '@testing-library/dom';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirror} from '@self/platform/helpers/testament';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {ADULT_SETTINGS} from '@self/root/src/entities/userSettings';
// flowlint-next-line untyped-import: off
import AdultConfirmationPO from '../components/AdultWarning/__pageObject';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function initContext(
    adultSetting: ?$Keys<typeof ADULT_SETTINGS> = null,
    exps: { [string]: boolean } = {},
    // $FlowFixMe
    settings: { [string]: any } = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {};

    if (adultSetting) {
        cookie[COOKIE_NAME.ADULT] = `${UID}:${yandexuid}:${adultSetting}`;
    }

    await mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            settings,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirror(__filename, jest);
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.runCode(() => {
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter();
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

describe('base', () => {
    describe('adult', () => {
        test('виджет не должен отображаться', async () => {
            await initContext(ADULT_SETTINGS.ADULT);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {});

            expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
        });
    });

    describe('child', () => {
        test('виджет не должен отображаться', async () => {
            await initContext(ADULT_SETTINGS.CHILD);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {});

            expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
        });
    });

    describe('empty', () => {
        beforeEach(() => initContext());
        test('виджет должен отображаться', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, {});

            expect(container.querySelector(AdultConfirmationPO.root)).not.toBeNull();
        });

        test('при нажатии на кнопку «Да» страница перезагружается', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, {});

            window.location.reload.mockClear();
            container.querySelector(AdultConfirmationPO.buttonAccept).click();
            await waitFor(() => {
                expect(window.location.reload).toHaveBeenCalled();
            });
        });

        describe('при нажатии на кнопку «Нет»', () => {
            test('виджет не должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    redirectToMainOnCancel: false,
                });

                expect(container.querySelector(AdultConfirmationPO.root)).not.toBeNull();
                container.querySelector(AdultConfirmationPO.buttonDecline).click();
                expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
            });

            test('если параметр redirectToMainOnCancel = false, не редиректит на главную', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    redirectToMainOnCancel: false,
                });

                window.location.assign.mockClear();
                container.querySelector(AdultConfirmationPO.buttonDecline).click();
                await waitFor(() => {
                    expect(window.location.assign).not.toHaveBeenCalled();
                });
            });

            test('если параметр redirectToMainOnCancel = true, редиректит на главную', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    redirectToMainOnCancel: true,
                });

                window.location.assign.mockClear();
                container.querySelector(AdultConfirmationPO.buttonDecline).click();
                await waitFor(() => {
                    expect(window.location.assign).toHaveBeenCalled();
                });
            });
        });
    });

    test('с включенным family фильтром виджет не должен отображаться', async () => {
        await initContext(null, {}, {family: true});
        const {container} = await apiaryLayer.mountWidget(widgetPath, {});

        expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
    });
});

describe('experiments', function () {
    describe('all_search_adult_blur', () => {
        const exps = {'all_search_adult-blur': true};

        describe('adult', () => {
            test('если параметр forceShow = true, виджет не должен отображаться', async () => {
                await initContext(ADULT_SETTINGS.ADULT, exps);
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    forceShow: true,
                });

                expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
            });
        });

        describe('child', () => {
            test('если параметр forceShow = true, виджет должен отображаться', async () => {
                await initContext(ADULT_SETTINGS.CHILD, exps);
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    forceShow: true,
                });

                expect(container.querySelector(AdultConfirmationPO.root)).not.toBeNull();
            });
        });

        describe('empty', () => {
            beforeEach(() => initContext(null, exps));

            test('виджет должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                expect(container.querySelector(AdultConfirmationPO.root)).not.toBeNull();
            });

            test('если параметр forceHide = true, виджет не должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    forceHide: true,
                });

                expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();
            });

            describe('при нажатии на кнопку «Да»', () => {
                test('если параметр shouldReloadOnConfirm = null, страница не перезагружается, виджет скрывается', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, {
                        shouldReloadOnConfirm: null,
                    });

                    window.location.reload.mockClear();
                    container.querySelector(AdultConfirmationPO.buttonAccept).click();
                    expect(container.querySelector(AdultConfirmationPO.root)).toBeNull();

                    await waitFor(() => {
                        expect(window.location.reload).not.toHaveBeenCalled();
                    });
                });

                test('если параметр shouldReloadOnConfirm не null, страница перезагружается', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, {
                        shouldReloadOnConfirm: true,
                    });

                    window.location.reload.mockClear();
                    container.querySelector(AdultConfirmationPO.buttonAccept).click();

                    await waitFor(() => {
                        expect(window.location.reload).toHaveBeenCalled();
                    });
                });
            });
        });
    });
});
