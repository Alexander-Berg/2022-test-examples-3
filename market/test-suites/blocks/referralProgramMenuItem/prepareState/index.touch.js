import {commonPrepareState} from './';

async function prepareState(params) {
    await commonPrepareState.call(this, params);

    await this.browser.yaOpenPage('touch:index');

    await this.header.clickMenuTrigger()
        .then(() => this.sideMenu.waitForVisible());

    // небольшая задержка, что пункт меню успел от рендериться без нее иногда тесты падают
    await this.browser.yaDelay(150);
}

export default prepareState;
