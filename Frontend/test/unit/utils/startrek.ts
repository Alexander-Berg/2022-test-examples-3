/* eslint-disable */
import test from 'ava';
import { escapeUserInput } from '../../../utils/startrek';

test('escapeUserInput', t => {
    t.is(escapeUserInput('привет мир'), '""привет мир""');
    t.is(escapeUserInput('привет "" мир'), '""привет ~"" мир""');
    t.is(escapeUserInput('**привет** мир'), '""**привет** мир""');
    t.is(
        escapeUserInput(`привет



мир`),
        `""привет
мир""`,
    );
});
