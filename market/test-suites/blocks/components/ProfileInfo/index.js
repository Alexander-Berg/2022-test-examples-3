import {makeSuite, makeCase} from 'ginny';

// page-objects
import MyContentPage from '@self/platform/spec/page-objects/widgets/pages/MyContentPage';

/**
 * @param {PageObject.MyContentPage} myContentPage
 */
export default makeSuite('Шапка профиля', {
    story: {
        beforeEach() {
            this.setPageObjects({
                myContentPage: () => this.createPageObject(MyContentPage),
            });
        },
        'по умолчанию': {
            'отображается корректно': makeCase({
                id: 'm-touch-3617',
                async test() {
                    await this.expect(this.myContentPage.isProfileInfoVisible())
                        .to.be.equal(true, 'Информация о пользователе отображается');

                    await this.expect(this.myContentPage.isExternalActivityStatVisible())
                        .to.be.equal(true, 'Статистика активности отображается');

                    await this.expect(this.myContentPage.isExpertiseLinkVisible())
                        .to.be.equal(true, 'Экспертиза пользователя отображается');

                    const expertiseUrl = await this.myContentPage.getExpertiseLinkUrl();
                    await this.expect(expertiseUrl).to.be.link({
                        pathname: '/my/expertise',
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
