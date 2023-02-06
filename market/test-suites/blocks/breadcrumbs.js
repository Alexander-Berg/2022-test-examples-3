import {makeCase, makeSuite} from 'ginny';
import {pluralize} from '@self/project/src/helpers/string';

/**
 * Тест на блок breadcrumbs
 * (не путать с n-breadcrumbs)
 * @param {PageObject.Breadcrumbs}
 */
export default makeSuite('Хлебные крошки.', {
    story: {
        'Всегда': { // eslint-disable-line quote-props
            'содержат ожидаемые ссылки': makeCase({
                params: {
                    links: 'Ожидаемые ссылки',
                },
                async test() {
                    const isExisting = await this.breadcrumbs.isExisting();
                    await this.expect(isExisting)
                        .to.equal(true, 'Хлебные крошки присутствуют на странице');

                    const breadcrumbsCount = await this.breadcrumbs.getCrumbsCount();
                    await this.expect(breadcrumbsCount).to.equal(
                        this.params.links.length,
                        `На странице присутствует ${this.params.links.length} ${
                            pluralize(
                                this.params.links.length,
                                'хлебная крошка',
                                'хлебные крошки',
                                'хлебных крошек'
                            )
                        }`
                    );

                    await Promise.all(this.params.links.map(async (linkPathname, index) => {
                        if (linkPathname == null) {
                            const crumbHref = this.breadcrumbs.getCrumbLinkHref(index + 1);
                            await this.expect(crumbHref).to.be.equal('', 'Ожидается элемент без сслыки в "href"');
                            return;
                        }
                        const crumbURL = this.breadcrumbs.getCrumbLinkUrl(index + 1);
                        await this.expect(crumbURL).to.be.link(linkPathname, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    }));
                },
            }),
        },
    },
});
