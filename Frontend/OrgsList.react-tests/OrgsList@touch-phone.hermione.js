'use strict';

const componentName = 'OrgsList';
const componentSelector = `.${componentName}`;

specs({
    feature: 'Компоненты: OrgsList',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenPlatformedComponent('OrgsList', 'Default');
        await this.browser.assertView('plain', componentSelector);
    });
});
