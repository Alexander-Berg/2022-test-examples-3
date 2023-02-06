const { NAVIGATION } = require('../config').consts;
const pageObjects = require('../page-objects/client');

describe('CSP ->', () => {
    it('Должен блокировать инлайновый JS', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-555');
        await bro.url(NAVIGATION.disk.url);

        const rootSelector = pageObjects.app();

        await bro.execute((rootId) => {
            const script = document.createElement('script');
            script.innerHTML = `
                document.querySelector('${rootId}').remove();
                const newContent = document.createElement('h1');
                newContent.innerHTML = 'Inline script was executed<br/>It has removed your Disk, MOAHAHA';
                newContent.setAttribute('style', 'color: red;');
                document.body.appendChild(newContent);
            `;
            document.head.appendChild(script);
        }, rootSelector);

        await bro.yaWaitForVisible(rootSelector, 100);
    });
});
