import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import EFIMExpired from '@self/root/src/components/EFIMExpired/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';

import additionalInfo from './additionalInfo';

const END_ACTION_TEXT = 'Акция закончилась, но скоро вернётся с новыми купонами';

module.exports = makeSuite('Блок конца акции.', {
    id: 'bluemarket-3381',
    issue: 'BLUEMARKET-6631',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    efimExpired: () => this.createPageObject(EFIMExpired, {parent: this.efimCoinBlock}),
                    efimExpiredTitle: () => this.createPageObject(Title, {parent: this.efimExpired}),
                });
            },
        },
        makeSuite('По умолчанию', {
            story: {
                'Блок конца акции отображается и содержит ожидаемый текст': makeCase({
                    async test() {
                        await this.efimExpired.isExisting()
                            .should.eventually.be.equal(
                                true,
                                'Блок с концом акции должен отображаться'
                            );
                        return this.efimExpiredTitle.getTitle()
                            .should.eventually.be.equal(
                                END_ACTION_TEXT,
                                `Текст в блоке конца акции должен содержать ${END_ACTION_TEXT}`
                            );
                    },
                }),
            },
        }),

        prepareSuite(additionalInfo)
    ),
});
