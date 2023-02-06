import 'hermione';
import {expect} from 'chai';

import {login, waitForReactRootLoaded} from '../../helpers';
import {TIMEOUT_MS, CHECK_INTERVAL} from '../../constants';

const PAGE_URL = '/survey/admin';

// TODO: переписать на скрипт
const BRAND_GID = 'brand@204037684';
const SERVICE_GID = 'service@238333785';
const CHANNEL_GID = 'channel@204054086';
const TEAM_GID = 'team@204450484';

type GidParam = typeof BRAND_GID | typeof SERVICE_GID | typeof CHANNEL_GID | typeof TEAM_GID;
type NodeProps = {
    props: {
        treeItem: {
            gid: GidParam;
        };
    };
};

/**
 * Каждый опрос называем с текущей датой и временем,
 * чтобы можно было убедиться в его корректном создании
 */
const getSurveyTitle = (): string => `Автотестовый опрос ${new Date().toLocaleString('ru')}`;

/**
 * Фунция создания пропсов с gid
 * для использования их в методе react$
 *
 * @param gid
 */
const getNodeProps = (gid: GidParam): NodeProps => ({
    props: {
        treeItem: {
            gid,
        },
    },
});

/**
 * Выбирает в поле с дропдауном ноду сущности с нужным gid
 *
 * @param context
 * @param attribute
 * @param gid
 */
const fillField = async (context, attribute, gid): Promise<void> => {
    const attributeLabel = await context.browser.$(`[data-ow-test-attribute-container="${attribute}"]`).$('label');
    const attributeInput = await context.browser.$(`[data-ow-test-attribute-container="${attribute}"]`).$('input');
    const modalTitle = await context.browser.react$('ModalTitle');

    await attributeLabel.click();
    await attributeInput.setValue('Автотест');
    const noData = await context.browser.$(`[data-ow-test-popup="${attribute}"]`).$('div');

    await context.browser.waitUntil(async () => (await noData.getText()) === 'Данные отсутствуют', {
        timeout: TIMEOUT_MS,
        interval: CHECK_INTERVAL,
        timeoutMsg: 'Не дождались скрытия надписи',
        reverse: true,
    });

    const attributeNode = await context.browser.react$('Node', getNodeProps(gid));

    await attributeNode.waitForDisplayed({
        timeout: TIMEOUT_MS,
        interval: CHECK_INTERVAL,
        timeoutMsg: 'Не дождались появления элемента списка',
    });

    await (await attributeNode.$('label')).click();
    await modalTitle.click({x: 0, y: 0});
};

/**
 * План теста:
 * 0. Дождаться загрузки реакта и страницы
 * 1. Нажать на кнопку "Создать опрос", дождаться открытия модалки
 * 2. Заполнить в модалке поля тестовыми данными, создать опрос
 * 3. Проверить создание опроса
 * 4. Поместить опрос в архив
 * 5. Убедиться, что опрос в архиве
 */
describe('ocrm-822: Создание и перемещение опроса в архив', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('должно работать корректно', async function() {
        /** ============== 0 ============== */
        await waitForReactRootLoaded(this.browser);

        const pageHeader = await this.browser.react$('PageHeader');

        await pageHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: 'Не дождались появления заголовка',
        });

        const pageTitle = await pageHeader.$('h1');
        const createButton = await pageHeader.$('button');

        const pageTitleText = await pageTitle.getText();

        expect(pageTitleText).to.equal('Редактирование опросов', 'Текст заголовка некорректный');

        /** ============== 1 ============== */
        await createButton.click();
        await (await this.browser.$('[data-ow-test-attribute-container="title"]')).waitForDisplayed();

        /** ============== 2 ============== */
        const titleField = await this.browser.$('[data-ow-test-attribute-container="title"]').$('input');
        const surveyTitle = getSurveyTitle();

        await titleField.setValue(surveyTitle);

        await fillField(this, 'brands', BRAND_GID);
        await fillField(this, 'services', SERVICE_GID);
        await fillField(this, 'channels', CHANNEL_GID);
        await fillField(this, 'teams', TEAM_GID);

        const confirmButton = await this.browser.react$('ModalControls').$('button=Создать');

        await confirmButton.click({x: 0, y: 0});

        /** ============== 3 ============== */
        const surveyHeader = await this.browser.react$('SurveyHeader');

        await this.browser.waitUntil(
            async () => {
                if (await surveyHeader.isDisplayed()) {
                    return (await surveyHeader.getText()) === surveyTitle;
                }

                return false;
            },
            {
                timeout: TIMEOUT_MS,
                interval: CHECK_INTERVAL,
                timeoutMsg: `Не дождались создания опроса: ${surveyTitle}`,
            }
        );

        /** ============== 4 ============== */
        const placeToArchiveButton = this.browser.react$('SurveyToolbar').$('button=Поместить в архив');

        await placeToArchiveButton.click();

        await this.browser.react$('ModalControls').waitForDisplayed();

        const sureButton = this.browser.react$('ModalControls').$('button=Да');

        await sureButton.click();

        /** ============== 5 ============== */
        const alert = this.browser.react$('SurveyEditForm').react$('Alert');

        await alert.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: `Не дождались появления плашки с информацией, что опрос перемещён в архив`,
        });

        expect(await alert.getText()).to.equal(
            'Данный опрос находится в архиве. Извлеките его из архива для использования.',
            `Опрос не перемещён в архив: ${surveyTitle}`
        );
    });
});
