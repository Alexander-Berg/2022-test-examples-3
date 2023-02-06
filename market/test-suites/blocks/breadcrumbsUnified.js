import {makeCase, makeSuite} from 'ginny';
import {pluralize} from '@self/project/src/helpers/string';

/**
 * Тест на блок breadcrumbsUnified
 * (не путать с n-breadcrumbs)
 * @param {PageObject.BreadcrumbsUnified}
 */
export default makeSuite('Хлебные крошки.', {
    story: {
        'Всегда': { // eslint-disable-line quote-props
            'содержат ожидаемые ссылки': makeCase({
                params: {
                    links: 'Ожидаемые ссылки',
                },
                async test() {
                    const isExisting = await this.breadcrumbsUnified.isExisting();
                    await this.expect(isExisting)
                        .to.equal(true, 'Хлебные крошки присутствуют на странице');

                    const breadcrumbsCount = await this.breadcrumbsUnified.getCrumbsCount();
                    await this.expect(breadcrumbsCount).to.equal(
                        this.params.links.length,
                        `На странице присутствует ${this.params.links.length} ${
                            pluralize(
                                this.params.links.length,
                                'хлебная крошка',
                                'хлебные крошки',
                                'хлебных крошек'
                            )}`
                    );

                    await Promise.all(this.params.links.map(async (linkPathname, index) => {
                        if (typeof linkPathname === 'string') {
                            const crumbText = this.breadcrumbsUnified.getCrumbText(index + 1, false);
                            await this.expect(crumbText).to.be.equal(
                                linkPathname,
                                'Ожидается, что текст соответствует переданому'
                            );
                            return;
                        }
                        const crumbURL = await this.breadcrumbsUnified.getCrumbLinkUrl(index + 1);
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
