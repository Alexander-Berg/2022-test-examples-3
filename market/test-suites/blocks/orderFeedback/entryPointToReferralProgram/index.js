import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import entryPointToReferralProgram from '@self/root/src/spec/hermione/test-suites/blocks/entryPointToReferralProgram';

import {ORDER_ID, setupFeedback, setupTest as _st} from '../utils/common';

const setupTest = async (ctx, {pageParams = {}, feedback = {}, ...other} = {}) => {
    await setupFeedback(ctx, feedback);
    await _st(ctx, {
        pageParams: {
            orderId: ORDER_ID,
            grade: 4,
            ...pageParams,
        },
        ...other,
    });
};

async function prepareStateForCheckEntryPointToReferralProgram() {
    await setupTest(this);

    if (typeof this.ratingControl.clickOnGrade === 'function') {
        await this.ratingControl.clickOnGrade('5');
    } else if (typeof this.ratingControl.setRating === 'function') {
        await this.ratingControl.setRating(5);
    }

    await this.browser.allure.runStep('Отправляем результат заполнения формы', () =>
        this.submitButton.click()
    );
}

export default makeSuite('Точка входа в реферальную программу.', {
    story: mergeSuites(
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Пользователь не достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4816',
            },
            params: {
                linkLabel: 'Получить 300 баллов за друга',
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgram,
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Пользователь достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4817',
            },
            params: {
                linkLabel: 'Рекомендовать Маркет друзьям',
                isGotFullReward: true,
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgram,
            },
        })
    ),
});
