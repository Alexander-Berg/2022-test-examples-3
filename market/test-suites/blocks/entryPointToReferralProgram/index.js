import {makeSuite, makeCase} from 'ginny';

import ReferralLandingLink from '@self/root/src/widgets/parts/ReferralLandingLink/__pageObject';
import prepareState from './helpers/prepareState';

const LINK_HREF = 'https://market.yandex.ru/my/referral';


const createStory = shouldShowLink => {
    const common = {
        async beforeEach() {
            this.setPageObjects({
                referralLandingLink: () => this.createPageObject(ReferralLandingLink),
            });

            await prepareState.call(this);
        },
    };

    if (shouldShowLink) {
        return {
            ...common,
            'Ссылка,': {
                'отображается на странице.': makeCase({
                    async test() {
                        await this.referralLandingLink.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Ссылка должна отображаться на странице'
                            );
                    },
                }),
                'имеет корректный текст.': makeCase({
                    async test() {
                        await this.referralLandingLink.getText()
                            .should.eventually.to.be.equal(
                                this.params.linkLabel,
                                `Ссылка должна содержать текст ${this.params.linkLabel}`
                            );
                    },
                }),
                'содержит правильный url.': makeCase({
                    async test() {
                        await this.referralLandingLink.getLinkHref()
                            .should.eventually.be.link(LINK_HREF, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        };
    }
    return {
        ...common,
        'Ссылка,': {
            'не отображается на странице.': makeCase({
                async test() {
                    await this.referralLandingLink.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Ссылка не должна отображаться на странице'
                        );
                },
            }),
        },
    };
};

/**
 * Конструктор тестов ссылки на реферальную программу.
 * Является общим генератором набора тестов, для различных кейсов.
 *
 * @param {boolean} shouldShowLink указывает должна ли отображаться ссылка
 **/

export default ({shouldShowLink} = {shouldShowLink: true}) => makeSuite('Точка входа в реферальную программу.', {
    feature: 'Реферальная программа',
    environment: 'kadavr',
    issue: 'MARKETFRONT-50645',
    params: {
        specialPrepareState: 'Функция доопределяющая состояние приложения под конкретный кейс',
        isYaPlus: 'Указывает является ли пользователь плюсовиком',
        isGotFullReward: 'Указывает достиг ли пользователь максимального количества баллов в акции',
        linkLabel: 'Текст ссылки, по которой осуществляется переход на лэндинг реферальной программы',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: createStory(shouldShowLink),
});
