import React from 'react';
import { mount, render } from 'enzyme';

import ConfirmDialog from '../../../../../lib/components/dialogs/confirm';

const defaultConfirmProps = {
    submitButtonText: 'submit text',
    cancelButtonText: 'cancel text'
};

describe('Диалог подтверждения', () => {
    describe('рендеринг', () => {
        it('с одной кнопкой', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с двумя кнопками', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с тремя кнопками', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                    extraButtonText="extra text"
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с двумя кнопками, кнопка отмены - активная', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                    actionButton="cancel"
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с тремя кнопками, дополнительная кнопка - активная', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                    extraButtonText="extra text"
                    actionButton="extra"
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с двумя кнопками, progress=true', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                    progress
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
        it('с двумя кнопками, submitDisabled=true', () => {
            const component = render(
                <ConfirmDialog
                    title="some title"
                    submitButtonText={defaultConfirmProps.submitButtonText}
                    cancelButtonText={defaultConfirmProps.cancelButtonText}
                    submitDisabled
                >
                    inner text
                </ConfirmDialog>
            );
            expect(component).toMatchSnapshot();
        });
    });

    describe('фокус', () => {
        const originalDocumentActiveElementBlur = document.activeElement.blur;
        beforeEach(() => {
            document.activeElement.blur = jest.fn();
        });
        afterEach(() => {
            document.activeElement.blur = originalDocumentActiveElementBlur;
        });

        it('Должен ставить фокус на кнопку при рендере сразу видимым', (done) => {
            expect(document.activeElement.blur).not.toBeCalled();
            const wrapper = mount(
                <ConfirmDialog
                    {...defaultConfirmProps}
                    visible
                />
            );

            const submitControlMock = {
                focus: jest.fn()
            };
            const cancelControlMock = {
                focus: jest.fn()
            };
            wrapper.find(ConfirmDialog).instance()._submitRef = submitControlMock;
            wrapper.find(ConfirmDialog).instance()._cancelRef = cancelControlMock;

            setTimeout(() => {
                expect(submitControlMock.focus).toBeCalled();
                expect(cancelControlMock.focus).not.toBeCalled();
                done();
            }, 20);
        });
        it('Должен ставить фокус на кнопку при становлении видимым', (done) => {
            const wrapper = mount(
                <ConfirmDialog
                    {...defaultConfirmProps}
                    visible={false}
                />
            );
            const submitControlMock = {
                focus: jest.fn()
            };
            const cancelControlMock = {
                focus: jest.fn()
            };
            wrapper.find(ConfirmDialog).instance()._submitRef = submitControlMock;
            wrapper.find(ConfirmDialog).instance()._cancelRef = cancelControlMock;

            expect(document.activeElement.blur).not.toBeCalled();
            expect(submitControlMock.focus).not.toBeCalled();
            expect(cancelControlMock.focus).not.toBeCalled();

            wrapper.setProps({ visible: true });
            setTimeout(() => {
                expect(submitControlMock.focus).toBeCalled();
                expect(cancelControlMock.focus).not.toBeCalled();
                done();
            }, 20);
        });
        it('Не должен ставить фокус на кнопку с опцией `autofocusButton: false`', (done) => {
            expect(document.activeElement.blur).not.toBeCalled();
            const wrapper = mount(
                <ConfirmDialog
                    {...defaultConfirmProps}
                    visible
                    autofocusButton={false}
                />
            );

            const submitControlMock = {
                focus: jest.fn()
            };
            const cancelControlMock = {
                focus: jest.fn()
            };
            wrapper.find(ConfirmDialog).instance()._submitRef = submitControlMock;
            wrapper.find(ConfirmDialog).instance()._cancelRef = cancelControlMock;

            setTimeout(() => {
                expect(submitControlMock.focus).not.toBeCalled();
                expect(cancelControlMock.focus).not.toBeCalled();
                done();
            }, 50);
        });
    });
});
