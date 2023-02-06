import '../../noscript';
import React from 'react';
import { mount } from 'enzyme';

import { AlbumTitleDialog } from '../../../../components/redux/components/dialogs/album-title';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';

const clickSubmit = (wrapper) => {
    const button = wrapper
        .find(Button)
        .findWhere((button) => button.prop('className')?.toString().includes('confirmation-dialog__button_submit'))
        .first();

    button.simulate('click');
};

describe('AlbumTitleDialog', () => {
    afterEach(() => {
        // clean up DOM after each test
        // required because dialog modal appends extra div
        // on each mount
        const node = global.document.body;
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    });
    it('should render', () => {
        const wrapper = mount(<AlbumTitleDialog visible={false} isIosSafari={false} />);

        expect(wrapper).toMatchSnapshot();
    });
    it('should disable button and show error if title is empty', () => {
        const onSubmit = jest.fn(() => {});

        const wrapper = mount(
            <AlbumTitleDialog
                onSubmit={onSubmit}
                visible={true}
                isIosSafari={false}
            />
        );

        const instance = wrapper.instance();

        const getErrorText = jest.spyOn(instance, '_getErrorText');
        const isSubmitDisabled = jest.spyOn(instance, '_isSubmitDisabled');

        wrapper.setState({ title: '' });

        expect(getErrorText.mock.results[0].value).toBe('Название альбома не может быть пустым.');
        expect(isSubmitDisabled.mock.results[0].value).toBe(true);

        clickSubmit(wrapper);

        expect(onSubmit).not.toHaveBeenCalled();
    });
    it('should submit if title is ok', () => {
        const onSubmit = jest.fn(() => {});

        const wrapper = mount(
            <AlbumTitleDialog
                onSubmit={onSubmit}
                visible={true}
                isIosSafari={false}
            />
        );

        clickSubmit(wrapper);

        expect(onSubmit).toHaveBeenCalledWith('Новый альбом');
    });
});
