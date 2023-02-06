import { mount } from 'enzyme';
import React from 'react';

import { FileListView } from '../../ui/FileListView';
import FileChooserDialog, { IFileChooserDialogProps } from './index';

const component = (props) => mount(<FileChooserDialog {...props}/>);
const fileList = (props) => mount(<FileListView {...props}/>);
const createFile = () => new File([], 'file.bin', {
    lastModified: 1,
});

describe('FileChooserDialog', () => {
    it('Should work with valid props', () => {
        const MOCK_PROPS: IFileChooserDialogProps = {
            onClose: jest.fn(),
            error: '',
            upload: jest.fn(),
            isUploading: false,
            onlyPDF: false,
        };

        expect(component(MOCK_PROPS)).toMatchSnapshot();
    });

    it('Should work with empty props', () => {
        const MOCK_PROPS: any = {};
        expect(component(MOCK_PROPS)).toMatchSnapshot();
    });

    it('Should call upload method on click', () => {
        const mockCallBack = jest.fn();

        const MOCK_PROPS: IFileChooserDialogProps = {
            onClose: jest.fn(),
            error: '',
            upload: mockCallBack,
            isUploading: false,
            onlyPDF: false,
        };

        const mountedComponent = component(MOCK_PROPS);

        mountedComponent.setState({
            files: [createFile()],
        });

        expect(mockCallBack).toHaveBeenCalledTimes(0);
        mountedComponent.find('.positive').simulate('click');
        expect(mockCallBack).toHaveBeenCalledTimes(1);
    });
});

describe('FileListView', () => {
    it('Should render with valid props', () => {
        const MOCK_PROPS = {
            files: [createFile()],
            removeItem: jest.fn(),
        };
        const mountedComponent = fileList(MOCK_PROPS);

        expect(mountedComponent).toMatchSnapshot();
    });

    it('Should call removeItem callback', () => {
        const mockCallBack = jest.fn();

        const MOCK_PROPS = {
            files: [createFile()],
            removeItem: mockCallBack,
        };

        const mountedComponent = fileList(MOCK_PROPS);

        expect(mockCallBack).toHaveBeenCalledTimes(0);
        mountedComponent.find('.remove').simulate('click');
        expect(mockCallBack).toHaveBeenCalledTimes(1);
    });
});
