/* eslint-disable */
import test from 'ava';
import { capitalizeFirstLetter } from '../../../utils';

test('capitalizeFirstLetter() with empty string', t => {
    t.is(capitalizeFirstLetter(''), '');
});

test('capitalizeFirstLetter() changes only first letter', t => {
    t.is(capitalizeFirstLetter('teSt'), 'TeSt');
});

test('capitalizeFirstLetter() works when first letter is already capital', t => {
    t.is(capitalizeFirstLetter('Test'), 'Test');
});
