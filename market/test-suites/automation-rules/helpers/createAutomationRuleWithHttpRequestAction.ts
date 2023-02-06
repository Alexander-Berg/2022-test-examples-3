import {TabsWrapper} from '../../../../src/controls/TabsControl/__pageObject__';
import {login} from '../../../helpers';
import ContentWithLabel from '../../../page-objects/contentWithLabel';
import Button from '../../../page-objects/button';
import Autocomplete from '../../../page-objects/autocomplete';
import Dropdown from '../../../page-objects/dropdown';
import {fillRequiredData} from './fillRequiredData';

/** Url создания правила автоматизации */
const PAGE_URL = '/entity/automationRule$action/automationRule$schedule/create';

type AuthorizationType = 'safetyTvm' | 'noAuth';

const addTVMAuthorization = async (ctx): Promise<void> => {
    const authorization = new Dropdown(ctx.browser, 'body', '[data-ow-test-http-request-parameter="authorization"]');

    const tvmService = new Autocomplete(ctx.browser, 'body', '[data-ow-test-http-request-parameter="tvm-service"]');

    await authorization.isDisplayed();
    await authorization.selectItem('[data-ow-test-select-option="TVM"]');

    await tvmService.isDisplayed();
    await tvmService.selectSingleItem('test');
};

const addAuthorization = async (ctx, authorizationType: AuthorizationType): Promise<void> => {
    switch (authorizationType) {
        case 'safetyTvm':
            await addTVMAuthorization(ctx);
            break;

        case 'noAuth':
            break;

        default:
            break;
    }
};

export const fillAutomationRuleWithHttpRequestAction = async (
    ctx,
    requestUrl: string = 'https://test',
    authorizationType: AuthorizationType = 'noAuth'
): Promise<void> => {
    await login(PAGE_URL, ctx);

    await fillRequiredData(ctx);

    const tabBar = new TabsWrapper(ctx.browser, 'body', '[data-ow-test-attribute-container="tabsWrapper"]');
    const addNewRuleButton = new Button(ctx.browser, 'body', '[data-ow-test-add-new-rule]');
    const addNewAction = new Dropdown(ctx.browser, 'body', '[data-ow-test-add-button-dropdown="Добавить действие"]');
    const url = new ContentWithLabel(ctx.browser, 'body', '[data-ow-test-http-request-parameter="url"]');

    await tabBar.isDisplayed();
    await tabBar.clickTab('Конфигурация правила');

    await addNewRuleButton.isDisplayed();
    await addNewRuleButton.clickButton();

    await addNewAction.isDisplayed();
    await addNewAction.selectItem('[data-ow-test-action="HTTP запрос"]');

    await url.isDisplayed();
    await url.setValue(requestUrl);

    await addAuthorization(ctx, authorizationType);
};

export const createAutomationRuleWithHttpRequestAction = async (
    ctx,
    requestUrl: string = 'https://test',
    authorizationType: AuthorizationType = 'noAuth',
    withProcessRedirect: boolean = false
): Promise<void> => {
    const editProcessRedirect = new ContentWithLabel(ctx.browser, 'body', '[data-ow-test-checkbox]');
    const saveButton = new Button(ctx.browser, 'body', '[data-ow-test-modal-controls="save"]');
    const addAutomationRule = new Button(ctx.browser, 'body', '[data-ow-test-save-button="automationRule$schedule"]');

    await fillAutomationRuleWithHttpRequestAction(ctx, requestUrl, authorizationType);

    if (withProcessRedirect) {
        await editProcessRedirect.click();
    }

    await saveButton.clickButton();

    await addAutomationRule.waitForEnable();
    await addAutomationRule.clickButton();
};
