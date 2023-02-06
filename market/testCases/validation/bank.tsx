import {scrollToElement, runIndependentSteps} from 'spec/utils';
import {VALIDATION} from 'shared/pages/SupplierJurInfoNext/validation';

import type {Ctx} from '../..';
import {
    REGULAR_VALIDATION_CASES_NAMES,
    bankLabels,
    getFullFieldName,
    getStringWithPattern,
    getValidationIndependentSteps,
} from '../../utils';

const sectionName = 'Банковские данные';

export default async (ctx: Ctx) => {
    await ctx.step(`Ждём готовность формы и переходим к блоку "${sectionName}"`, async () => {
        await ctx.app.editableForm.waitForAvailable();
        await scrollToElement(ctx, ctx.app.editableForm.bankForm.root);
    });

    await runIndependentSteps(ctx, [
        // bank accountNumber
        ...getValidationIndependentSteps({
            ctx,
            fieldName: getFullFieldName(sectionName, bankLabels.accountNumber),
            setValue: accountNumber => ctx.app.editableForm.bankForm.setAccountNumber(accountNumber),
            getErrorKey: () => ctx.app.editableForm.bankForm.accountNumber.getErrorKey(),
            cases: [
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.EMPTY,
                    value: '',
                    afterSetValue: () => ctx.app.editableForm.bankForm.accountNumber.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.accountNumber.required',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.WRONG_CHARS,
                    value: 'FOOBAR',
                    afterSetValue: () => ctx.app.editableForm.bankForm.accountNumber.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.accountNumber.required',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.TOO_SHORT,
                    value: getStringWithPattern(VALIDATION.BANK.ACCOUNT_NUMBER.LENGTH - 5, '22222', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.accountNumber.wrongLength',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.INVALID,
                    value: getStringWithPattern(VALIDATION.BANK.ACCOUNT_NUMBER.LENGTH, '22222', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.accountNumber.wrongChecksum',
                    // для проверки чексуммы нужен бик
                    afterSetValue: () => ctx.app.editableForm.bankForm.setBik('044525225'),
                },
            ],
        }),

        // bank bik
        ...getValidationIndependentSteps({
            ctx,
            fieldName: getFullFieldName(sectionName, bankLabels.bik),
            setValue: bik => ctx.app.editableForm.bankForm.setBik(bik),
            getErrorKey: () => ctx.app.editableForm.bankForm.bik.getErrorKey(),
            cases: [
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.EMPTY,
                    value: '',
                    afterSetValue: () => ctx.app.editableForm.bankForm.bik.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.bik.required',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.WRONG_CHARS,
                    value: 'FOOBAR',
                    afterSetValue: () => ctx.app.editableForm.bankForm.bik.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.bik.required',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.TOO_SHORT,
                    value: getStringWithPattern(VALIDATION.BANK.BIK.LENGTH - 5, '22222', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.bik.wrongLength',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.NOT_FOUND,
                    value: getStringWithPattern(VALIDATION.BANK.BIK.LENGTH, '22222', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.bank.bik.bikNotFound',
                    afterSetValue: () => new Promise(resolve => setTimeout(resolve, 1500)),
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.VALID,
                    value: '044525728',
                    errorKey: null,
                    afterSetValue: () => new Promise(resolve => setTimeout(resolve, 1500)),
                },
            ],
        }),
    ]);
};
