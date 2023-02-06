import { shallow } from 'enzyme';
import * as React from 'react';

import { Dict } from '../../../types';
import { ytLogs } from '../../utils/sendLogs';
import { Button, ButtonTypes } from './index';

const TITLE = 'title';
const setUp = (props?: Dict<any>) => shallow(<Button {...props}>{TITLE}</Button>);

describe('Button', () => {
    describe('Should render Button component', () => {
        it('Should render simple Button component', () => {
            const component = setUp();
            expect(component).toMatchSnapshot();
        });

        it('Should render simple basic Button component', () => {
            const component = setUp({ basic: true });
            expect(component).toMatchSnapshot();
        });

        it('Should render simple loading Button component', () => {
            const component = setUp({ isLoading: true });
            expect(component).toMatchSnapshot();
        });

        it('Should render simple disabled Button component', () => {
            const component = setUp({ disabled: true });
            expect(component).toMatchSnapshot();
        });

        it('Should render simple custom className Button component', () => {
            const component = setUp({ className: 'custom_class_name' });
            expect(component).toMatchSnapshot();
        });

        it('Should render positive Button component', () => {
            const component = setUp({ type: ButtonTypes.positive });
            expect(component).toMatchSnapshot();
        });

        it('Should render negative Button component', () => {
            const component = setUp({ type: ButtonTypes.negative });
            expect(component).toMatchSnapshot();
        });

        it('Should render warning Button component', () => {
            const component = setUp({ type: ButtonTypes.warning });
            expect(component).toMatchSnapshot();
        });
    });

    describe('Should init log', () => {

        it('Should init log with ytLogs props', () => {
            const ytLog = ytLogs.getInstance();
            const component = setUp({ ytLog });
            const instance: any = component.instance();
            expect(instance.log).not.toBe(null);
        });

        it('Should not init log without ytLogs props', () => {
            const component = setUp();
            const instance: any = component.instance();
            expect(instance.log).toBe(null);
        });
    });

    describe('Should click', () => {

        it('Should call this.props.onClick()', () => {
            const mockCallBack = jest.fn();
            const component = setUp({ onClick: mockCallBack });

            expect(mockCallBack).toHaveBeenCalledTimes(0);
            component.simulate('click');
            expect(mockCallBack).toHaveBeenCalledTimes(1);
        });

        it('Should call this.log.send if ytLogs props', () => {
            const ytLog = ytLogs.getInstance();
            ytLog.isTest = true;
            const component = setUp({ ytLog });
            const instance: any = component.instance();

            const spyMethod = jest.spyOn(instance.log, 'send');
            component.simulate('click');
            expect(spyMethod).toHaveBeenCalledTimes(1);
        });

        it('Should call openFile() if file', () => {
            const component: any = setUp({ file: true });
            const SPY_METHOD = 'openFile';

            const spyMethod = jest.spyOn(component.instance(), SPY_METHOD);
            component.simulate('click');
            expect(spyMethod).toHaveBeenCalledTimes(1);
        });

        it('Should not call this.props.onClick() if disabled', () => {
            const mockCallBack = jest.fn();
            const component = setUp({ disabled: true, onClick: mockCallBack });

            expect(mockCallBack).toHaveBeenCalledTimes(0);
            component.simulate('click');
            expect(mockCallBack).toHaveBeenCalledTimes(0);
        });

        it('Should not call this.props.onClick() if isLoading', () => {
            const mockCallBack = jest.fn();
            const component = setUp({ isLoading: true, onClick: mockCallBack });

            expect(mockCallBack).toHaveBeenCalledTimes(0);
            component.simulate('click');
            expect(mockCallBack).toHaveBeenCalledTimes(0);
        });

        it('Should not call this.props.onClick() if disabled and isLoading', () => {
            const mockCallBack = jest.fn();
            const component = setUp({ disabled: true, isLoading: true, onClick: mockCallBack });

            expect(mockCallBack).toHaveBeenCalledTimes(0);
            component.simulate('click');
            expect(mockCallBack).toHaveBeenCalledTimes(0);
        });
    });
});
