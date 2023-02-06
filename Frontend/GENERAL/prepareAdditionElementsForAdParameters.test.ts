import { BSMetaVideoAdConstructorData } from '../../typings';
import { prepareAdditionElementsForAdParameters, DEFAULT_ADDITION_ELEMENTS } from './prepareAdditionElementsForAdParameters';

describe('[VASTAdCreator] prepareAdditionElementsForAdParameters', () => {
    it('should return default value if addition elements are empty', () => {
        expect(prepareAdditionElementsForAdParameters(undefined)).toEqual(DEFAULT_ADDITION_ELEMENTS);
        expect(prepareAdditionElementsForAdParameters([])).toEqual(DEFAULT_ADDITION_ELEMENTS);
    });

    it('should return correct HAS-value', () => {
        const additionElements: BSMetaVideoAdConstructorData['AdditionElements'] = [{
            Type: 'BUTTON',
            Options: {},
        }];
        const expectedResult = {
            ...DEFAULT_ADDITION_ELEMENTS,
            HAS_BUTTON: true,
        };

        expect(prepareAdditionElementsForAdParameters(additionElements)).toEqual(expectedResult);
    });

    it('should return correct options', () => {
        const additionElements: BSMetaVideoAdConstructorData['AdditionElements'] = [{
            Type: 'TITLE',
            Options: {
                BackgroundColor: '#fff',
                BorderColor: '#000',
                Color: '#ccc',
                CustomLabel: 'CustomLabel',
                Position: 'left',
                Text: 'Text',
                TextColor: '#eee',
            },
        }];
        const expectedResult = {
            ...DEFAULT_ADDITION_ELEMENTS,
            HAS_TITLE: true,
            TITLE_BACKGROUND_COLOR: additionElements[0].Options.BackgroundColor,
            TITLE_BORDER_COLOR: additionElements[0].Options.BorderColor,
            TITLE_COLOR: additionElements[0].Options.Color,
            TITLE_LABEL: additionElements[0].Options.CustomLabel,
            TITLE_POSITION: additionElements[0].Options.Position,
            TITLE_TEXT: additionElements[0].Options.Text,
            TITLE_TEXT_COLOR: additionElements[0].Options.TextColor,
        };

        expect(prepareAdditionElementsForAdParameters(additionElements)).toEqual(expectedResult);
    });

    it('shoudl return special labels for button', () => {
        const additionElements: BSMetaVideoAdConstructorData['AdditionElements'] = [{
            Type: 'BUTTON',
            Options: { CustomLabel: 'Перейти на сайт' },
        }];
        const expectedResult = {
            ...DEFAULT_ADDITION_ELEMENTS,
            HAS_BUTTON: true,
            BUTTON_LABELS: {
                ru: additionElements[0].Options.CustomLabel,
                en: additionElements[0].Options.CustomLabel,
                tr: additionElements[0].Options.CustomLabel,
            },
        };

        expect(prepareAdditionElementsForAdParameters(additionElements)).toEqual(expectedResult);
    });
});
