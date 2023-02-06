import React from 'react';
import { render } from '@testing-library/react';
import { OebsEditor as RawOebsEditor } from '~/src/features/Service/components/OebsEditor/OebsEditor';
import { OebsEditor as OebsEditorPO } from '~/test/features/desktop/service/oebs-editor.po';
import { getOebsAgreementMock } from '~/test/jest/mocks/data/oebs-agreement';
import { getAbcContextMock } from '~/test/jest/mocks/data/common';
import { withContext } from '~/src/common/hoc';

const OebsEditor = withContext(RawOebsEditor, getAbcContextMock());

describe('OebsEditor', () => {
    const baseProps = {
        oebsFlags: {
            useForHr: true,
            useForProcurement: false,
            useForRevenue: true,
            useForHardware: true,
            useForGroup: true
        },
        initialOebsFlags: {
            useForHr: true,
            useForProcurement: false,
            useForRevenue: true,
            useForHardware: true,
            useForGroup: true
        },
        onFieldChange: jest.fn(),
        onSubmit: jest.fn(),
    };

    it('should render simple component with permission to edit', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    {...baseProps}
                />,
            ),
        );

        const useForRevenueCheckbox = wrapper.oebsUseForRevenueCheckbox;
        const useForGroupCheckbox = wrapper.oebsUseForGroupCheckbox;
        const useForHrCheckbox = wrapper.oebsUseForHrCheckbox;
        const useForHardwareCheckbox = wrapper.oebsUseForHardwareCheckbox;
        const useForProcurementCheckbox = wrapper.oebsUseForProcurementCheckbox;

        expect(useForRevenueCheckbox?.isDisabled).toBeTruthy();
        expect(useForGroupCheckbox?.isDisabled).toBeFalsy();
        expect(useForHrCheckbox?.isDisabled).toBeFalsy();
        expect(useForHardwareCheckbox?.isDisabled).toBeFalsy();
        expect(useForProcurementCheckbox?.isDisabled).toBeFalsy();

        expect(wrapper.submitButton?.isDisabled).toBeTruthy();
        expect(wrapper.infoMessage?.container).toBeUndefined();
        expect(wrapper.errorMessage?.container).toBeUndefined();
    });

    it('should disable controls and show message without permission to edit', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags={false}
                    {...baseProps}
                />,
            ),
        );

        wrapper.checkboxes.forEach(checkbox => {
            expect(checkbox.isDisabled).toBeTruthy();
        });

        expect(wrapper.submitButton?.isDisabled).toBeTruthy();
        expect(wrapper.infoMessage?.container).toBeInTheDocument();
        expect(wrapper.errorMessage?.container).toBeUndefined();
    });

    it('should disable controls during submitting the form', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    formSubmitInProgress
                    {...baseProps}
                />,
            ),
        );

        wrapper.checkboxes.forEach(checkbox => {
            expect(checkbox.isDisabled).toBeTruthy();
        });

        expect(wrapper.submitButton?.isInProgress).toBeTruthy();
    });

    it('should disable controls with active agreement', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    formSubmitInProgress
                    oebsAgreement={getOebsAgreementMock(1, { state: 'approving' })}
                    {...baseProps}
                />,
            ),
        );

        wrapper.checkboxes.forEach(checkbox => {
            expect(checkbox.isDisabled).toBeTruthy();
        });

        expect(wrapper.submitButton?.isDisabled).toBeTruthy();
        expect(wrapper.infoMessage?.container).toBeInTheDocument();
    });

    it('should not disable controls with closed agreement', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    oebsAgreement={getOebsAgreementMock(1, { state: 'applied' })}
                    {...baseProps}
                />,
            ),
        );

        const useForRevenueCheckbox = wrapper.oebsUseForRevenueCheckbox;
        const useForGroupCheckbox = wrapper.oebsUseForGroupCheckbox;
        const useForHrCheckbox = wrapper.oebsUseForHrCheckbox;
        const useForHardwareCheckbox = wrapper.oebsUseForHardwareCheckbox;
        const useForProcurementCheckbox = wrapper.oebsUseForProcurementCheckbox;

        expect(useForRevenueCheckbox?.isDisabled).toBeTruthy();
        expect(useForGroupCheckbox?.isDisabled).toBeFalsy();
        expect(useForHrCheckbox?.isDisabled).toBeFalsy();
        expect(useForHardwareCheckbox?.isDisabled).toBeFalsy();
        expect(useForProcurementCheckbox?.isDisabled).toBeFalsy();

        expect(wrapper.infoMessage?.container).toBeUndefined();
    });

    it('should display passed error message', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    formSubmitError={{ status: 404, data: { detail: '' } }}
                    {...baseProps}
                />,
            ),
        );

        expect(wrapper.errorMessage?.container).toBeInTheDocument();
    });

    it('should call onSubmit function after button click', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    formChanged
                    {...baseProps}
                />,
            ),
        );

        wrapper.submitButton?.click();
        expect(baseProps.onSubmit).toBeCalledTimes(1);
    });

    it('should call onFieldChange function after checkbox click', () => {
        const wrapper = new OebsEditorPO(
            render(
                <OebsEditor
                    canEditOebsFlags
                    {...baseProps}
                />,
            ),
        );

        wrapper.oebsUseForProcurementCheckbox?.click();

        expect(baseProps.onFieldChange).toBeCalledTimes(1);
        expect(baseProps.onFieldChange).toBeCalledWith('useForProcurement', true);
    });
});
