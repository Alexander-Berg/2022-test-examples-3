import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
    Checkbox,
    DeeplinkRedirect,
    OnTop,
    Popup,
    SaveToCookie,
    ICheckboxParams,
    IDeeplinkRedirectParams,
    IOnTopParams,
    IPopupParams,
    ISaveToCookieParams,
} from '../presets';
import { ILcCustomJsProps, ScriptPreset, ScriptType } from '../LcCustomJs.types';
import { LcCustomJs } from '../LcCustomJs';

jest.mock('../presets', () => ({
    __esModule: true,
    Checkbox: jest.fn(),
    DeeplinkRedirect: jest.fn(),
    OnTop: jest.fn(),
    Popup: jest.fn(),
    SaveToCookie: jest.fn(),
}));

describe('<LcCustomJs/> component', () => {
    let wrapper: ReactWrapper;

    afterEach(() => {
        wrapper.unmount();
    });

    test('should include script "Checkbox"', () => {
        const checkboxJest = jest.fn();
        (Checkbox as jest.Mock).mockImplementation(checkboxJest);

        const params: ICheckboxParams = {
            sectionSelector: '#section',
            checkedClass: 'some-checkbox_checked',
            checkbox1Selector: '#checkbox_1',
            checkbox2Selector: '#checkbox_2',
            checkbox3Selector: '#checkbox_3',
            queryParams1: ['foo=1', 'foo=2'],
            queryParams2: ['bar=1', 'bar=2'],
            queryParams3: ['key=1', 'key=2']
        };

        const props: ILcCustomJsProps = {
            sectionId: '',
            nonce: '',
            events: [],
            script: {
                type: ScriptType.Preset,
                preset: ScriptPreset.Checkbox,
                params,
            },
        };

        wrapper = mount(<LcCustomJs {...props} />);

        expect(checkboxJest).toHaveBeenCalledWith(params);
    });

    test('should include script "DeeplinkRedirect"', () => {
        const deeplinkRedirectJest = jest.fn();
        (DeeplinkRedirect as jest.Mock).mockImplementation(deeplinkRedirectJest);

        const params: IDeeplinkRedirectParams = {
            url: 'yandexbrowser-open-url://App-prefs%3ASAFARI%26path%3DSEARCH_ENGINE_SETTING',
            fallbackUrl: 'https://itunes.apple.com/ru/app/id483693909?at=11l9Wx&ct=browser-paid',
            fallbackTimeout: 5000,
        };

        const props: ILcCustomJsProps = {
            sectionId: '',
            nonce: '',
            events: [],
            script: {
                type: ScriptType.Preset,
                preset: ScriptPreset.DeeplinkRedirect,
                params,
            },
        };

        wrapper = mount(<LcCustomJs {...props} />);

        expect(deeplinkRedirectJest).toHaveBeenCalledWith(params);
    });

    test('should include script "OnTop"', () => {
        const onTopJest = jest.fn();
        (OnTop as jest.Mock).mockImplementation(onTopJest);

        const params: IOnTopParams = {
            dataSelector: 'selector1',
            parentLinkSelector: 'selector2',
            windowWidth: 1,
            windowHeight: 2,
            windowTop: 3,
            windowLeft: 4,
        };

        const props: ILcCustomJsProps = {
            sectionId: '',
            nonce: '',
            events: [],
            script: {
                type: ScriptType.Preset,
                preset: ScriptPreset.OnTop,
                params,
            },
        };

        wrapper = mount(<LcCustomJs {...props} />);

        expect(onTopJest).toHaveBeenCalledWith(params);
    });

    test('should include script "Popup"', () => {
        const popupJest = jest.fn();
        (Popup as jest.Mock).mockImplementation(popupJest);

        const params: IPopupParams = {
            buttonSelector: 'button1',
            dataSelector: 'selector1',
            parentLinkSelector: 'selector2',
            windowWidth: 78,
            windowHeight: 420,
            windowTop: 100,
            windowLeft: 4,
        };
        const props: ILcCustomJsProps = {
            sectionId: '',
            nonce: '',
            events: [],
            script: {
                type: ScriptType.Preset,
                params,
                preset: ScriptPreset.Popup,
            },
        };

        wrapper = mount(<LcCustomJs {...props} />);

        expect(popupJest).toHaveBeenCalledWith(params);
    });

    test('should include script "SaveToCookie"', () => {
        const saveToCookieJest = jest.fn();
        (SaveToCookie as jest.Mock).mockImplementation(saveToCookieJest);

        const params: ISaveToCookieParams = {
            parameter: 'test',
            lifetime: 1000 * 60,
        };
        const props: ILcCustomJsProps = {
            sectionId: '',
            nonce: '',
            events: [],
            script: {
                type: ScriptType.Preset,
                params,
                preset: ScriptPreset.SaveToCookie,
            },
        };

        wrapper = mount(<LcCustomJs {...props} />);

        expect(SaveToCookie).toHaveBeenCalledWith(params);
    });
});
