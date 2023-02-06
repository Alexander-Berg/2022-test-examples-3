import { shallow } from 'enzyme';
import * as React from 'react';

import { LANGUAGES } from '../../../types';
import { LocalizationReducerState } from '../../reducers/localizationReducer';
import { Translate } from './index';

const TEST_TEXT_ID = 'test_text_id';
const WRONG_TEST_TEXT_ID = 'wrong_test_text_id';
const HTML = `<div><span data-type="translate_error_text"><sup class="global_error">*</sup>${WRONG_TEST_TEXT_ID}</span></div>`;
const TRANSLATE_STORE: LocalizationReducerState = {
    localizations: {
        [TEST_TEXT_ID]: {
            [LANGUAGES.ENG]: {
                value: 'text',
            },
            [LANGUAGES.RUS]: {
                value: 'текс',
            },
        },
    },
    currentLang: LANGUAGES.ENG,
};
const EXPECTED_TEXT = TRANSLATE_STORE && TRANSLATE_STORE.localizations
    && TRANSLATE_STORE.localizations[TEST_TEXT_ID][LANGUAGES.ENG].value;

describe('Translate', () => {
    it('should be translation error', () => {
        const t = new Translate(TRANSLATE_STORE);
        const WrongTranslateTextComponent = () => <div>{t.getItem(WRONG_TEST_TEXT_ID)}</div>;
        const wrapper = shallow(<WrongTranslateTextComponent/>);
        expect(wrapper.html()).toEqual(HTML);
    });

    it('should be translation error by simple text', () => {
        const t = new Translate(TRANSLATE_STORE);
        const text = t.getSimpleText(WRONG_TEST_TEXT_ID);
        expect(text).toEqual(WRONG_TEST_TEXT_ID);
    });

    it('should be normal translation', () => {
        const t = new Translate(TRANSLATE_STORE);
        const translateText = t.getItem(TEST_TEXT_ID);
        expect(translateText).toEqual(EXPECTED_TEXT);
    });
});
