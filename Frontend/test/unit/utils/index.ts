/* eslint-disable */
import test from 'ava';
import * as utils from '../../../utils';

test('tokenize() return tokens', t => {
    t.deepEqual(utils.tokenize('a b c'), ['a', 'b', 'c']);
});

test('tokenize() empty list on empty string input', t => {
    t.deepEqual(utils.tokenize(''), []);
});

test('tokenize() supports cyrillic letters', t => {
    t.deepEqual(utils.tokenize('мама мыла раму'), ['мама', 'мыла', 'раму']);
});

test('tokenize() removes double spaces', t => {
    t.deepEqual(utils.tokenize('мама  мыла  раму'), ['мама', 'мыла', 'раму']);
});

test('tokenize() removes punctuation', t => {
    t.deepEqual(utils.tokenize('мама, мыла – раму'), ['мама', 'мыла', 'раму']);
});

test('stripPunctuation() removes punctuation and double spaces', t => {
    t.is(utils.stripPunctuation('мама      – мыла раму'), 'мама мыла раму');
});
