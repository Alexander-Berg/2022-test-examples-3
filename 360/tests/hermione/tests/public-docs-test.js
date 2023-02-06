const pubDocs = require('../config/index').publicDocs;
const PageObjects = require('../page-objects/public');

const doTest = ({ type, name, url, diskpublic }) => {
    describe(`Паблик документа ${type} -> `, () => {
        it(`${diskpublic} Просмотр документа ${type} в неавторизованном состоянии`, async function() {
            const bro = this.browser;
            await bro.url(url);
            await bro.yaWaitForVisibleDocPreview(type);
            await bro.yaAssertFileName(name);
            const isMobile = await bro.yaIsMobile();
            await bro.yaClickAndAssertNewTabUrl(
                PageObjects.docPreview(), {
                    linkShouldContain: isMobile ? 'docviewer' : '/docs/view?url=ya-disk-public',
                    message: type + ' - Не открылся DV при клике на превью документа'
                }
            );
        });
    });
};

pubDocs.forEach(doTest);
