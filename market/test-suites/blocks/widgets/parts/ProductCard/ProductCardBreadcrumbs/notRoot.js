import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.BreadcrumbsUnified} breadcrumbsUnified
 */
export default makeSuite('Хлебные крошки. Не корневая категория.', {
    story: {
        'По-умолчанию': {
            'содержит правильный текст родителя': makeCase({
                params: {
                    name: 'Название родительской категории',
                },
                id: 'm-touch-1175',
                issue: 'MOBMARKET-4915',
                test() {
                    return this.breadcrumbsUnified
                        .getCrumbText(1, true)
                        .should.eventually.be.equal(this.params.name);
                },
            }),

            'содержит правильную ссылку на родителя': makeCase({
                params: {
                    nid: 'nid страницы, для генерации страницы каталога',
                    hid: 'hid страницы, для генерации страницы каталога',
                    slug: 'slug страницы, для генерации страницы каталога',
                    track: 'Значение query-параметра track',
                },
                id: 'm-touch-1175',
                issue: 'MOBMARKET-4915',
                test() {
                    return Promise
                        .all([
                            this.browser.yaBuildURL('touch:catalog', {
                                nid: this.params.nid,
                                hid: this.params.hid,
                                slug: this.params.slug,
                                track: this.params.track,
                            }),
                            this.breadcrumbsUnified.getCrumbLinkHref(1),
                        ])
                        .then(([expectedPath, breadcrumbsLink]) => this.expect(breadcrumbsLink)
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            }));
                },
            }),
        },
    },
});
