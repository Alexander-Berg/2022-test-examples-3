import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Headline} headline
 */
export default makeSuite('CMS шапка.', {
    environment: 'testing',
    story: {
        'По умолчанию': {
            'ссылка содержит слаг':
                makeCase({
                    id: 'm-touch-2655',
                    issue: 'MOBMARKET-11479',
                    async test() {
                        const link = await this.headline.getLink();

                        return this.expect(link, 'Ссылка должна содержать слаг')
                            .to.be.link({
                                pathname: '^\\/[\\w]+--[\\w-]+(/)?',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
        },
    },
});
