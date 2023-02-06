import '../noscript';
import React from 'react';
import { AddToAlbumBar } from '../../../components/redux/components/add-to-album-bar';
import pageHelper from '../../../components/helpers/page';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';
import { shallow, mount } from 'enzyme';

describe('AddToAlbumBar', () => {
    it('should be correctly rendered for new album', () => {
        const props = {
            selected: [],
            totalSelected: 0,
            missingSelected: 0,
            newAlbumTitle: 'new album',
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        expect(wrapper).toMatchSnapshot();
    });
    it('should be correctly rendered for existing album', () => {
        const props = {
            targetAlbumId: 'a',
            selected: [],
            totalSelected: 0,
            missingSelected: 0,
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        expect(wrapper).toMatchSnapshot();
    });
    it('should correctly render selected items\' count for new album', () => {
        const props = {
            selected: ['a'],
            totalSelected: 1,
            missingSelected: 0,
            newAlbumTitle: 'new album',
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        expect(wrapper).toMatchSnapshot();
    });
    it('should correctly render selected items\' count for existing album', () => {
        const props = {
            targetAlbumId: 'a',
            selected: ['a'],
            totalSelected: 1,
            missingSelected: 0,
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        expect(wrapper).toMatchSnapshot();
    });
    it('should render only digits on mobile devices', () => {
        const props = {
            targetAlbumId: 'a',
            selected: ['a', 'b'],
            totalSelected: 2,
            missingSelected: 0,
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        expect(wrapper).toMatchSnapshot();
    });
    it('should disable add button for existing album if no photos are selected', () => {
        const props = {
            targetAlbumId: 'a',
            selected: [],
            totalSelected: 0,
            missingSelected: 0,
            isCreatingAlbum: false
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        const saveButton = wrapper.find(Button).first();

        expect(saveButton.prop('disabled')).toBe(true);
    });
    it('should add selected photos to existing album', () => {
        const addToAlbum = jest.fn(() => {});
        const createAlbum = jest.fn(() => {});

        const props = {
            addToAlbum, createAlbum,
            targetAlbumId: 'a',
            selected: ['a', 'b'],
            totalSelected: 2,
            missingSelected: 0
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        const saveButton = wrapper.find(Button).first();

        saveButton.simulate('click');

        expect(addToAlbum).toHaveBeenCalledWith(['a', 'b'], 'a', { shouldRedirectToAlbum: true });
        expect(createAlbum).not.toHaveBeenCalled();
    });
    it('should create new empty album', () => {
        const addToAlbum = jest.fn(() => {});
        const createAlbum = jest.fn(() => {});

        const props = {
            addToAlbum, createAlbum,
            newAlbumTitle: 'a',
            selected: [],
            totalSelected: 0,
            missingSelected: 0
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        const saveButton = wrapper.find(Button).first();

        saveButton.simulate('click');

        expect(createAlbum).toHaveBeenCalledWith([], 'a');
        expect(addToAlbum).not.toHaveBeenCalled();
    });
    it('should create new album with selected photos', () => {
        const addToAlbum = jest.fn(() => {});
        const createAlbum = jest.fn(() => {});

        const props = {
            addToAlbum, createAlbum,
            newAlbumTitle: 'a',
            selected: ['a', 'b'],
            totalSelected: 2,
            missingSelected: 0
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);
        const saveButton = wrapper.find(Button).first();

        saveButton.simulate('click');

        expect(createAlbum).toHaveBeenCalledWith(['a', 'b'], 'a');
        expect(addToAlbum).not.toHaveBeenCalled();
    });
    it('should not close if checkbox is clicked', () => {
        const deselectAllStatesContext = jest.fn(() => {});

        const props = {
            deselectAllStatesContext,
            newAlbumTitle: 'a',
            selected: ['a'],
            totalSelected: 1,
            missingSelected: 0
        };

        const wrapper = mount(<AddToAlbumBar {...props} />);
        const checkbox = wrapper.find('.lite-checkbox__control');
        const onClose = jest.spyOn(wrapper.instance(), '_onClose');

        checkbox.simulate('change');

        expect(deselectAllStatesContext).toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });
    it('should correctly close', () => {
        const deselectAllStatesContext = jest.fn(() => {});
        const setAlbumsScroll = jest.fn(() => {});

        pageHelper.go = jest.fn(() => {});

        const props = {
            setAlbumsScroll,
            deselectAllStatesContext,
            returnPath: '/client/disk/folder',
            newAlbumTitle: 'a',
            selected: ['a'],
            totalSelected: 1,
            missingSelected: 0,
        };

        const wrapper = mount(<AddToAlbumBar {...props} />);
        const closeButton = wrapper
            .find(Button)
            .findWhere((button) => button.prop('className')?.toString() === 'resources-action-bar__close')
            .first();

        closeButton.simulate('click');

        expect(deselectAllStatesContext).toHaveBeenCalled();
        expect(setAlbumsScroll).toHaveBeenCalled();
        expect(pageHelper.go).toHaveBeenCalledWith('/client/disk/folder');
    });
    it('should reset new album title on unmount', () => {
        const setNewAlbumTitle = jest.fn(() => {});

        const props = {
            setNewAlbumTitle,
            newAlbumTitle: 'a',
            selected: [],
            totalSelected: 0,
            missingSelected: 0,
        };

        const wrapper = shallow(<AddToAlbumBar {...props} />);

        wrapper.unmount();

        expect(setNewAlbumTitle).toHaveBeenCalledWith(null);
    });
});
