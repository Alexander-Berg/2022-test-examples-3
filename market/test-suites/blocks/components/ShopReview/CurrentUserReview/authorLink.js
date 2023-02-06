import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Header} header
 * @params {PageObject.UserExpertisePopup} userExpertisePopup
 */
export default makeSuite('Блок «Ваш отзыв». Ссылка автора', {
    feature: 'Блок отзыва',
    story: {
        'Автор отзыва.': {
            'При клике открывается попап экспертизы автора': makeCase({
                id: 'm-touch-2361',
                issue: 'MOBMARKET-9587',
                async test() {
                    await this.header.clickAuthorName();
                    const isVisible = await this.userExpertisePopup.isModalVisible();
                    await this.expect(isVisible).to.be.equal(true);
                },
            }),
            'Попап экспертизы содержит ссылку на профиль автора': makeCase({
                id: 'm-touch-2361',
                issue: 'MOBMARKET-9587',
                async test() {
                    await this.header.clickAuthorName();
                    const url = this.userExpertisePopup.getAuthorLinkUrl();
                    await this.expect(url).to.be.link({
                        pathname: '/user/[a-z0-9]{26,50}',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    });
                },
            }),
        },
    },
});
