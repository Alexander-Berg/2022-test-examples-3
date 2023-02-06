import {makeCase} from 'ginny';
import {isNil} from 'ambar';

import {NOT_VIEWED} from '@self/root/src/constants/a11y';

const TITLE = 'Приглашайте друзей';

/**
 * Конструктор тестов контента плашки.
 * Является общим генератором набора тестов, для различных кейсов.
 * @param {boolean} shouldBeShown идентификатор отображения плашки
 * @param {string} subTitle текст плашки
 **/
const makeCases = ({
    shouldBeShown,
    subTitle,
}) => {
    const resultCases = {};

    if (shouldBeShown) {
        resultCases['Плашка отображается.'] = makeCase({
            async test() {
                await this.referralProgramMenuItem.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Плашка должна отображаться'
                    );
            },
        });

        resultCases['Заголовок присутствует в плашке.'] = makeCase({
            async test() {
                await this.referralProgramMenuItem.isPrimaryTextVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Заголовок должен содержаться в плашке'
                    );
            },
        });

        if (isNil(subTitle)) {
            resultCases[
                'Заголовок содержит корректный текст'
                + 'и кружок уведомления подписан для незрячего'
            ] = makeCase({
                async test() {
                    const titleWithNotification = `${TITLE}\n${NOT_VIEWED}`;

                    await this.referralProgramMenuItem.getPrimaryText()
                        .should.eventually.to.be.equal(
                            titleWithNotification,
                            `Текст заголовка должен содержать ${titleWithNotification}`
                        );
                },
            });

            resultCases['Подзаголовок отсутствует в плашке.'] = makeCase({
                async test() {
                    await this.referralProgramMenuItem.isSecondaryTextVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Подзаголовок должен отсутствовать в плашке'
                        );
                },
            });
        } else {
            resultCases['Заголовок содержит корректный текст.'] = makeCase({
                async test() {
                    await this.referralProgramMenuItem.getPrimaryText()
                        .should.eventually.to.be.equal(
                            TITLE,
                            `Текст заголовка должен содержать ${TITLE}`
                        );
                },
            });

            resultCases['Подзаголовок содержит корректный текст.'] = makeCase({
                async test() {
                    await this.referralProgramMenuItem.isSecondaryTextVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Подзаголовок должен содержаться в плашке'
                        );

                    await this.referralProgramMenuItem.getSecondaryText()
                        .should.eventually.to.be.equal(
                            subTitle,
                            `Текст подзаголовка должен содержать ${subTitle}`
                        );
                },
            });
        }

        resultCases['Ссылка содержит корректный url.'] = makeCase({
            async test() {
                await this.menuItemLink.getHref()
                    .should.eventually.to.be.link({
                        pathname: '/my/referral',
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
            },
        });
    } else {
        resultCases['Плашка не отображается.'] = makeCase({
            async test() {
                await this.referralProgramMenuItem.isVisible()
                    .should.eventually.to.be.equal(
                        false,
                        'Плашка не должна отображаться'
                    );
            },
        });
    }

    return resultCases;
};

export default makeCases;
