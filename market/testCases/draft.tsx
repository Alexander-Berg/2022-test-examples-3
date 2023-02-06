import {reloadPage, scrollToElement, runIndependentSteps} from 'spec/utils';
import {TAX_SYSTEMS} from 'shared/pages/SupplierJurInfoNext';
import {separateAddressAndPostCode} from 'shared/pages/SupplierJurInfoNext/utils';

import type {Ctx} from '..';
import {
    getRandomNumber,
    getRandomAddress,
    getRandomFirstName,
    getRandomLastName,
    getRandomMiddleName,
    getRandomRequesterJurDocTypes,
    getRandomPosition,
    getRandomBankInfo,
    getRandomVat,
    getRandomEmail,
    getRandomSchedule,
    assertValues,
    orgInfoFieldLabels,
    requesterLabels,
    bankLabels,
    taxLabels,
    contactLabels,
    customSupportLabels,
    getRandomComment,
    complainsLabels,
} from '../utils';
import type {FormValues} from '../utils';

export default async (ctx: Ctx) => {
    await ctx.step('Ждём готовность формы', async () => {
        await ctx.app.editableForm.waitForAvailable();
    });

    await ctx.step('Выбираем ручное заполнение, а не Market ID', async () => {
        await ctx.app.editableForm.marketID.useNewData();
    });

    await ctx.step('Раскрываем блок "Привезти самому"', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.complainsForm.self.root);
        await ctx.app.editableForm.complainsForm.self.checkbox.setChecked(true);
        await ctx.app.editableForm.complainsForm.self.name.root.waitForVisible(5000);
    });

    const oldFormValues: FormValues = await ctx.app.editableForm.getValues();
    const generagedFormValues: FormValues = {};

    await ctx.step('Заполняем блок "Основная информация об организации" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.orgInfoForm.root);

        const {orgInfo = {}} = oldFormValues;

        const {address: actualAddress, postalCode: actualPostalCode} = separateAddressAndPostCode(
            getRandomAddress(true, orgInfo.actualAddress),
        );

        generagedFormValues.orgInfo = {
            inn: getRandomNumber(10, orgInfo.inn),
            jurAddress: getRandomAddress(false, orgInfo.jurAddress),
            actualAddress,
            actualPostalCode,
        };

        await ctx.app.editableForm.orgInfoForm.setValues(generagedFormValues.orgInfo);
    });

    await ctx.step('Заполняем блок "Данные заявителя" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.requesterForm.root);

        const {requester = {}} = oldFormValues;
        generagedFormValues.requester = {
            lastName: getRandomLastName(requester.lastName),
            firstName: getRandomFirstName(requester.firstName),
            middleName: getRandomMiddleName(requester.middleName),
            position: getRandomPosition(requester.position),
            documentType: getRandomRequesterJurDocTypes(requester.documentType),
        };

        await ctx.app.editableForm.requesterForm.setValues(generagedFormValues.requester);
    });

    await ctx.step('Заполняем блок "Банковские данные" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.bankForm.root);

        const {bank = {}} = oldFormValues;
        generagedFormValues.bank = {
            accountNumber: getRandomNumber(20, bank.accountNumber),
            ...getRandomBankInfo(),
        };

        await ctx.app.editableForm.bankForm.setValues(generagedFormValues.bank);
    });

    await ctx.step('Заполняем блок "Информация о налогообложении" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.taxForm.root);

        const {tax = {}} = oldFormValues;
        generagedFormValues.tax = {
            // Для OSN разрешён выбор НДС.
            system: TAX_SYSTEMS.OSN,
            vat: getRandomVat(tax.vat),
            valueFromPricelist: !tax.valueFromPricelist,
        };

        await ctx.app.editableForm.taxForm.setValues(generagedFormValues.tax);
    });

    await ctx.step('Заполняем блок "Взаиморасчёт и отчётность" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.accountantForm.root);

        const {accountant = {}} = oldFormValues;
        generagedFormValues.accountant = {
            lastName: getRandomLastName(accountant.lastName),
            firstName: getRandomFirstName(accountant.firstName),
            middleName: getRandomMiddleName(accountant.middleName),
            email: getRandomEmail(accountant.email),
            phoneCode: accountant.phoneCode,
            phoneNumber: getRandomNumber(10, accountant.phoneNumber),
            phoneAdditionalCode: getRandomNumber(3, accountant.phoneAdditionalCode),
        };

        await ctx.app.editableForm.setAccountantFormValues(generagedFormValues.accountant);
    });

    await ctx.step('Заполняем блок "Возвраты и претензии" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.complainsForm.root);

        const {complains = {}} = oldFormValues;
        const {address = '', postalCode = ''} = separateAddressAndPostCode(getRandomAddress(true));

        generagedFormValues.complains = {
            person: {
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                lastName: getRandomLastName(complains.person.lastName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                firstName: getRandomFirstName(complains.person.firstName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                middleName: getRandomMiddleName(complains.person.middleName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                email: getRandomEmail(complains.person.email),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneCode: complains.person.phoneCode,
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneNumber: getRandomNumber(10, complains.person.phoneNumber),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneAdditionalCode: getRandomNumber(3, complains.person.phoneAdditionalCode),
            },
            post: {
                address,
                postalCode,
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                companyName: getRandomFirstName(complains.person.firstName),
            },
            self: {
                address: getRandomAddress(true),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                lastName: getRandomLastName(complains.self.lastName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                firstName: getRandomFirstName(complains.self.firstName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                middleName: getRandomMiddleName(complains.self.middleName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneCode: complains.self.phoneCode,
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneNumber: getRandomNumber(10, complains.self.phoneNumber),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneAdditionalCode: getRandomNumber(3, complains.self.phoneAdditionalCode),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                jobPosition: getRandomFirstName(complains.self.firstName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                comment: getRandomComment(complains.self.comment),
            },
            carrier: {
                address: getRandomAddress(true),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                lastName: getRandomLastName(complains.carrier.lastName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                firstName: getRandomFirstName(complains.carrier.firstName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                middleName: getRandomMiddleName(complains.carrier.middleName),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneCode: complains.carrier.phoneCode,
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneNumber: getRandomNumber(10, complains.carrier.phoneNumber),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                phoneAdditionalCode: getRandomNumber(3, complains.carrier.phoneAdditionalCode),
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                jobPosition: getRandomFirstName(complains.carrier.firstName),
            },
        };

        await ctx.app.editableForm.setComplainsFormValues(generagedFormValues.complains);
    });

    await ctx.step('Заполняем блок "Общение с покупателями" случайными данными', async () => {
        await scrollToElement(ctx, ctx.app.editableForm.customerSupportForm.root);

        const {customers = {}} = oldFormValues;
        generagedFormValues.customers = {
            schedule: getRandomSchedule(customers.schedule),
            phoneCode: customers.phoneCode,
            phoneNumber: getRandomNumber(10, customers.phoneNumber),
            phoneAdditionalCode: getRandomNumber(3, customers.phoneAdditionalCode),
            address: getRandomAddress(false, customers.address),
        };

        await ctx.app.editableForm.customerSupportForm.setValues(generagedFormValues.customers);
    });

    await ctx.step('Ждём сохранения черновика, обновляем страницу и ждём готовность формы', async () => {
        await ctx.app.editableForm.waitForDraftSaved();
        await reloadPage(ctx);
        await ctx.app.editableForm.waitForAvailable();
    });

    await ctx.step('Проверяем, что в Market ID выбрано ручное заполнение', async () => {
        await ctx
            .expect(ctx.app.editableForm.marketID.isNewData())
            .equal(true, 'Выбрано "Использовать на Беру новые данные"');
    });

    let currentFormValues: FormValues;

    await ctx.step('Получаем текущие значения полей формы', async () => {
        currentFormValues = await ctx.app.editableForm.getValues();
    });

    await runIndependentSteps(ctx, [
        {
            name: 'Проверяем, что блок "Основная информация об организации" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.orgInfoForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: orgInfoFieldLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.orgInfo,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.orgInfo,
                });
            },
        },
        {
            name: 'Проверяем, что блок "Данные заявителя" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.requesterForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: requesterLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.requester,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.requester,
                });
            },
        },
        {
            name: 'Проверяем, что блок "Банковские данные" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.bankForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: bankLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.bank,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.bank,
                });
            },
        },
        {
            name: 'Проверяем, что блок "Информация о налогообложении" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.taxForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: taxLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.tax,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.tax,
                });
            },
        },
        {
            name: 'Проверяем, что блок "Взаиморасчёт и отчётность" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.accountantForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: contactLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.accountant,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.accountant,
                });
            },
        },
        {
            name: 'Проверяем, что блок "Возвраты и претензии" заполнен правильно',
            stepFn: async () => {
                await runIndependentSteps(ctx, [
                    {
                        name: 'Проверяем, что блок "Сотрудник" заполнен правильно',
                        stepFn: async () => {
                            await scrollToElement(ctx, ctx.app.editableForm.complainsForm.root);
                            await assertValues({
                                ctx,
                                labelsByNames: complainsLabels.person,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                currentValues: currentFormValues.complains?.person,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                expectedValues: generagedFormValues.complains?.person,
                            });
                        },
                    },
                    {
                        name: 'Проверяем, что блок "Почта" заполнен правильно',
                        stepFn: async () => {
                            await scrollToElement(ctx, ctx.app.editableForm.complainsForm.post.root);
                            await assertValues({
                                ctx,
                                labelsByNames: complainsLabels.post,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                currentValues: currentFormValues.complains?.post,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                expectedValues: generagedFormValues.complains?.post,
                            });
                        },
                    },
                    {
                        name: 'Проверяем, что блок "Курьером" заполнен правильно',
                        stepFn: async () => {
                            await scrollToElement(ctx, ctx.app.editableForm.complainsForm.carrier.root);
                            await assertValues({
                                ctx,
                                labelsByNames: complainsLabels.carrier,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                currentValues: currentFormValues.complains?.carrier,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                expectedValues: generagedFormValues.complains?.carrier,
                            });
                        },
                    },
                    {
                        name: 'Проверяем, что блок "Самопривоз" заполнен правильно',
                        stepFn: async () => {
                            await scrollToElement(ctx, ctx.app.editableForm.complainsForm.self.root);
                            await assertValues({
                                ctx,
                                labelsByNames: complainsLabels.self,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                currentValues: currentFormValues.complains?.self,
                                // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                                expectedValues: generagedFormValues.complains?.self,
                            });
                        },
                    },
                ]);
            },
        },
        {
            name: 'Проверяем, что блок "Общение с покупателями" заполнен правильно',
            stepFn: async () => {
                await scrollToElement(ctx, ctx.app.editableForm.customerSupportForm.root);
                await assertValues({
                    ctx,
                    labelsByNames: customSupportLabels,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    currentValues: currentFormValues.customers,
                    // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
                    expectedValues: generagedFormValues.customers,
                });
            },
        },
    ]);
};
