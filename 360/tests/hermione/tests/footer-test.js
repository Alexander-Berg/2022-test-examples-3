const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const CommonObjects = require('../page-objects/client-footer').common;

const footerLinks = [
    {
        user: 'yndx-ufo-test-158',
        object: CommonObjects.footerHelpAndSupportLink(),
        objectMobile: CommonObjects.footerSupportLink(),
        link: 'yandex.ru/support/disk/',
        linkMobile: 'yandex.ru/support/disk-mobile/',
        testpalmIds: [705, 4865],
        description: 'справку и поддержку'
    },
    {
        user: 'yndx-ufo-test-yandex',
        object: CommonObjects.footerHelpAndSupportLink(),
        objectMobile: CommonObjects.footerSupportLink(),
        link: 'yandex.ru/support/disk/',
        linkMobile: 'yandex.ru/support/disk-mobile/',
        testpalmIds: [5487, 5484],
        description: 'справку и поддержку для яндексоидов'
    },
    {
        onlyDesktop: true,
        user: 'yndx-ufo-test-yandex',
        object: CommonObjects.footerFeedbackLink(),
        link: 'yandex.ru/support/disk/zout_yandex.html',
        testpalmIds: [5488],
        description: 'обратную связь для яндексоидов'
    },
    {
        onlyDesktop: true,
        user: 'yndx-ufo-test-158',
        object: CommonObjects.footerBlogLink(),
        link: 'yandex.ru/blog/disk',
        testpalmIds: [4869],
        description: 'блог'
    },
    {
        onlyDesktop: true,
        user: 'yndx-ufo-test-158',
        object: CommonObjects.footerDevsLink(),
        link: 'yandex.ru/dev',
        testpalmIds: [4866],
        description: 'раздел разработчикам'
    },
    {
        onlyDesktop: true,
        user: 'yndx-ufo-test-158',
        object: CommonObjects.footerRulesLink(),
        link: 'yandex.ru/legal/disk_termsofuse/',
        testpalmIds: [4867],
        description: 'условия использования'
    },
    {
        onlyDesktop: true,
        user: 'yndx-ufo-test-158',
        object: CommonObjects.footerResearchesLink(),
        link: ['yandex.ru/jobs/', '/usability'],
        testpalmIds: [4868],
        description: 'исследования'
    }
];

const doTest = ({ object, objectMobile, link, linkMobile, testpalmIds, description, onlyDesktop, user }) => {
    describe('Футер страницы -> ', () => {
        onlyDesktop && hermione.only.in(clientDesktopBrowsersList);
        it(`diskclient-${testpalmIds.join(', ')}: ссылка на ${description}`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = `diskclient-${isMobile ? testpalmIds[1] : testpalmIds[0]}`;
            await bro.yaClientLoginFast(user);
            await bro.yaWaitForVisible(CommonObjects.footer());
            await bro.yaClickAndAssertNewTabUrl(isMobile && objectMobile ? objectMobile : object,
                { linkShouldContain: isMobile && linkMobile ? linkMobile : link });
        });
    });
};

footerLinks.forEach(doTest);
