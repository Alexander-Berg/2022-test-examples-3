import {scrollToElement, runIndependentSteps} from 'spec/utils';
import {VALIDATION} from 'shared/pages/SupplierJurInfoNext/validation';

import type {Ctx} from '../..';
import {
    REGULAR_VALIDATION_CASES_NAMES,
    getFullFieldName,
    getStringWithPattern,
    getValidationIndependentSteps,
    orgInfoFieldLabels,
} from '../../utils';

const sectionName = 'Основная информация об организации';

export default async (ctx: Ctx) => {
    await ctx.step(`Ждём готовность формы и переходим к блоку "${sectionName}"`, async () => {
        await ctx.app.editableForm.waitForAvailable();
        await scrollToElement(ctx, ctx.app.editableForm.orgInfoForm.root);
    });

    await runIndependentSteps(ctx, [
        // orgInfo inn
        ...getValidationIndependentSteps({
            ctx,
            fieldName: getFullFieldName(sectionName, orgInfoFieldLabels.inn),
            setValue: name => ctx.app.editableForm.orgInfoForm.setInn(name),
            getErrorKey: () => ctx.app.editableForm.orgInfoForm.inn.getErrorKey(),
            cases: [
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.EMPTY,
                    value: '',
                    afterSetValue: () => ctx.app.editableForm.orgInfoForm.inn.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.inn.required',
                },
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.WRONG_CHARS,
                    value: 'FOOBAR',
                    afterSetValue: () => ctx.app.editableForm.orgInfoForm.inn.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.inn.required',
                },
                {
                    caseName: `${REGULAR_VALIDATION_CASES_NAMES.TOO_SHORT}`,
                    value: getStringWithPattern(VALIDATION.ORG_INFO.INN.IP_LENGTH - 5, '11111', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.inn.innWrongLength',
                },
                {
                    caseName: `${REGULAR_VALIDATION_CASES_NAMES.INVALID}`,
                    value: getStringWithPattern(VALIDATION.ORG_INFO.INN.IP_LENGTH, '11111', ''),
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.inn.wrongChecksum',
                },
            ],
        }),

        // orgInfo jurAddress
        ...getValidationIndependentSteps({
            ctx,
            fieldName: getFullFieldName(sectionName, orgInfoFieldLabels.jurAddress),
            setValue: name => ctx.app.editableForm.orgInfoForm.setJurAddress(name),
            getErrorKey: () => ctx.app.editableForm.orgInfoForm.jurAddress.getErrorKey(),
            cases: [
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.EMPTY,
                    value: '',
                    afterSetValue: () => ctx.app.editableForm.orgInfoForm.jurAddress.blur(),
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.jurAddress.required',
                },
            ],
        }),

        // orgInfo actualAddress
        ...getValidationIndependentSteps({
            ctx,
            fieldName: getFullFieldName(sectionName, orgInfoFieldLabels.actualAddress),
            setValue: name => ctx.app.editableForm.orgInfoForm.setActualAddress(name),
            getErrorKey: () => ctx.app.editableForm.orgInfoForm.actualAddress.getErrorKey(),
            cases: [
                {
                    caseName: REGULAR_VALIDATION_CASES_NAMES.EMPTY,
                    value: '',
                    afterSetValue: async () => {
                        await ctx.app.editableForm.orgInfoForm.actualAddress.trigger.click();
                        return ctx.app.editableForm.orgInfoForm.actualAddress.blur();
                    },
                    errorKey: 'pages.supplier-jur-info-next:validation.orgInfo.actualAddress.required',
                },
            ],
        }),
    ]);
};
