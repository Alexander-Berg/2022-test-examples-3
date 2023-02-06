import {
    VPAID_THEME_EMPTY,
    VPAID_THEME_INTERACTIVE_VIEWER,
    VPAID_THEME_MOTION,
    VPAID_THEME_SURVEY,
    VPAID_URL_INTERACTIVE_VIEWER,
    VPAID_URL_OLD,
    VPAID_URL_SURVEY,
} from '../../const';
import { BSMetaVideoAdConstructorData } from '../../typings';
import { getVpaidUrlAndTheme, VpaidUrlAndTheme } from './getVpaidUrlAndTheme';

describe('[VASTAdCreator] getVpaidUrlAndTheme', () => {
    it('should return undefined if it is not VPAID', () => {
        const constructorData: BSMetaVideoAdConstructorData = {};

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(undefined);
    });

    it('should return motion params', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            Theme: VPAID_THEME_MOTION,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_MOTION,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return interactive viewer params by InteractiveVpaid field', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            InteractiveVpaid: true,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_INTERACTIVE_VIEWER,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return interactive viewer params by VpaidPcodeUrl field', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            VpaidPcodeUrl: VPAID_URL_INTERACTIVE_VIEWER,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_INTERACTIVE_VIEWER,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return interactive viewer params despite Theme', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            VpaidPcodeUrl: VPAID_URL_INTERACTIVE_VIEWER,
            Theme: VPAID_THEME_EMPTY,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_INTERACTIVE_VIEWER,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return theme empty params', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            Theme: VPAID_THEME_EMPTY,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_EMPTY,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return survey params', () => {
        const constructorData: BSMetaVideoAdConstructorData = {
            Theme: VPAID_THEME_SURVEY,
            VpaidPcodeUrl: VPAID_URL_SURVEY,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_SURVEY,
            url: VPAID_URL_INTERACTIVE_VIEWER,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return survey params with custom url', () => {
        const customUrl = 'some.js';
        const constructorData: BSMetaVideoAdConstructorData = {
            Theme: VPAID_THEME_SURVEY,
            VpaidPcodeUrl: customUrl,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme: VPAID_THEME_SURVEY,
            url: customUrl,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('should return given params', () => {
        const url = 'some.js';
        const theme = 'theme';
        const constructorData: BSMetaVideoAdConstructorData = {
            VpaidPcodeUrl: url,
            Theme: theme,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme,
            url,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });

    it('shoud return given theme and default url', () => {
        const theme = 'theme';
        const constructorData: BSMetaVideoAdConstructorData = {
            Theme: theme,
        };
        const expectedResult: VpaidUrlAndTheme = {
            theme,
            url: VPAID_URL_OLD,
        };

        expect(getVpaidUrlAndTheme(constructorData)).toEqual(expectedResult);
    });
});
