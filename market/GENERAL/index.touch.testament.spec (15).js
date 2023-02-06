import {buildUrl} from '@self/project/src/utils/router';

import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {UTF_VACANCIES_PARAMS} from '@self/platform/components/Footer';

import Footer from '../__pageObject';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;


async function makeContext(user = {}) {
    return mandrelLayer.initContext({
        user,
    });
}

beforeAll(async () => {
    mockLocation();
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('Блок футер', () => {
    beforeEach(async () => {
        await makeContext();
    });
    test('Ссылка "Маркет нанимает" по умолчанию ведёт на страницу "Вакансии Яндекс.Маркет".', async () => {
        const {container} = await apiaryLayer.mountWidget(widgetPath);
        const actualUrl = container.querySelector(Footer.vacanciesLink).getAttribute('href');
        const expectedUrl = buildUrl('external:jobs-vacancies-dev', {
            from: 'footer',
            ...UTF_VACANCIES_PARAMS,
        });

        expect(actualUrl).toEqual(expectedUrl);
    });
    test('Ссылка на десктоп по умолчанию должна вести на такую же desktop-страницу.', async () => {
        const {container} = await apiaryLayer.mountWidget(widgetPath);
        const actualUrl = container.querySelector(Footer.desktopLink).getAttribute('href');

        expect(actualUrl).toEqual('//market.yandex.ru/?no-pda-redir=1&track=ftr_touch_to_desktop');
    });
    test('Статы индексации: по умолчанию должны быть данные о кол-ве магазинов, офферов и даты обновления.', async () => {
        const {container} = await apiaryLayer.mountWidget(widgetPath);
        const text = container.querySelector(Footer.stats).textContent;
        const template = /^\d+\u2009\d+\u2009\d+ предложени(?:е|й|я) от \d+?\u2009?\d+ магазин(?:ов|а)?\.Обновлено \d{2}\.\d{2}\.\d{4} в \d{2}:\d{2} по московскому времени\.$/;

        expect(text).toMatch(template);
    });
});
