import { render } from './oebs-editor.po';

import { getFullAccessPermissions, getServicesViewerPermissions } from '~/test/jest/mocks/data/common';
import { getServiceStoreMock } from '~/test/jest/mocks/data/service';

describe('Редактирование OEBS-флагов', () => {
    describe('Положительные', () => {
        it('Значения флагов правильно проставляются в чекбоксах', () => {
            const oebsEditor = render({
                granularPermissions: getFullAccessPermissions(),
                service: getServiceStoreMock(123, {
                    useForHr: true,
                    useForProcurement: false,
                    useForRevenue: true,
                }),
            });

            expect(oebsEditor.oebsUseForHrCheckbox?.isChecked).toBeTruthy();
            expect(oebsEditor.oebsUseForProcurementCheckbox?.isChecked).toBeFalsy();
            expect(oebsEditor.oebsUseForRevenueCheckbox?.isChecked).toBeTruthy();
        });

        it('Владелец сервиса может редактировать флаги', () => {
            const oebsEditor = render({
                granularPermissions: getFullAccessPermissions(),
                service: getServiceStoreMock(123, {
                    useForHr: true,
                    useForProcurement: false,
                    useForRevenue: true,
                }),
            });

            expect(oebsEditor.oebsUseForHrCheckbox?.isDisabled).toBeFalsy();
            expect(oebsEditor.oebsUseForProcurementCheckbox?.isDisabled).toBeFalsy();
            expect(oebsEditor.oebsUseForRevenueCheckbox?.isDisabled).toBeFalsy();

            expect(oebsEditor.infoMessage?.container).toBeUndefined();
        });

        it('Проверка возможных сочетаний значений чекбоксов', () => {
            const oebsEditor = render({
                granularPermissions: getFullAccessPermissions(),
                service: getServiceStoreMock(123, {
                    useForHr: false,
                    useForProcurement: false,
                    useForRevenue: false,
                }),
            });

            expect(oebsEditor.areFormValuesEqualTo(false, false, false)).toBeTruthy();

            oebsEditor.oebsUseForRevenueCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(false, false, true)).toBeTruthy();

            oebsEditor.oebsUseForHrCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(true, false, true)).toBeTruthy();

            oebsEditor.oebsUseForProcurementCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(true, true, true)).toBeTruthy();

            oebsEditor.oebsUseForProcurementCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(true, false, true)).toBeTruthy();

            oebsEditor.oebsUseForRevenueCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(false, false, false)).toBeTruthy();

            oebsEditor.oebsUseForRevenueCheckbox?.click();
            oebsEditor.oebsUseForProcurementCheckbox?.click();
            oebsEditor.oebsUseForHrCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(true, true, true)).toBeTruthy();
            oebsEditor.oebsUseForRevenueCheckbox?.click();
            expect(oebsEditor.areFormValuesEqualTo(false, false, false)).toBeTruthy();
        });
    });

    describe('Отрицательные', () => {
        it('Обычный участник сервиса с полной ролью не может редактировать флаги', () => {
            const oebsEditor = render({
                granularPermissions: getFullAccessPermissions(),
                service: getServiceStoreMock(123, {
                    permissions: [],
                    useForHr: true,
                    useForProcurement: false,
                    useForRevenue: true,
                }),
            });

            expect(oebsEditor.oebsUseForHrCheckbox?.isDisabled).toBeTruthy();
            expect(oebsEditor.oebsUseForProcurementCheckbox?.isDisabled).toBeTruthy();
            expect(oebsEditor.oebsUseForRevenueCheckbox?.isDisabled).toBeTruthy();

            expect(oebsEditor.infoMessage?.container).toBeInTheDocument();
        });

        it('Пользователь с ограниченной ролью не может редактировать флаги', () => {
            const oebsEditor = render({
                granularPermissions: getServicesViewerPermissions(),
                service: getServiceStoreMock(123, {
                    useForHr: true,
                    useForProcurement: false,
                    useForRevenue: true,
                }),
            });

            expect(oebsEditor.oebsUseForHrCheckbox?.isDisabled).toBeTruthy();
            expect(oebsEditor.oebsUseForProcurementCheckbox?.isDisabled).toBeTruthy();
            expect(oebsEditor.oebsUseForRevenueCheckbox?.isDisabled).toBeTruthy();

            expect(oebsEditor.infoMessage?.container).toBeInTheDocument();
        });
    });
});
