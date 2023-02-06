/* eslint-disable */
import test from 'ava';
import {
    generatePublicShareKey,
    generateOneTimeShareKey,
    isOneTimeShareKey,
    isPublicShareKey,
    validateShareKey,
} from '../../../utils/shares';

import config from '../../../services/config';

test('generatePublicShareId', t => {
    t.is(generatePublicShareKey().length, config.share.publicShareIdLength);
});

test('generateOneTimeShareKey', t => {
    t.is(generateOneTimeShareKey().length, config.share.oneTimeShareIdLength);
});

test('isPublicShareId', t => {
    t.true(isPublicShareKey('aaaaaaaaaaaaaaaa'));
    t.false(isPublicShareKey('aaaaaaaaaaaaaaaaaaaa'));
    t.false(isPublicShareKey('aaaaaaaaaaaaaaaaaaaaaa'));
    t.false(isPublicShareKey('aaaaaaaaaaaaaa'));
});

test('isOneTimeShareId', t => {
    t.true(isOneTimeShareKey('aaaaaaaaaaaaaaaaaaaa'));
    t.false(isOneTimeShareKey('aaaaaaaaaaaaaaaa'));
    t.false(isOneTimeShareKey('aaaaaaaaaaaaaaaaaaaaa'));
});

test('validateShareId', t => {
    t.true(validateShareKey('aaaaaaaaaaaaaaaa'));
    t.true(validateShareKey('aaaaaaaaaaaaaaaaaaaa'));
    t.false(validateShareKey('aaaaaaaaaaaaaaaaaaaaa'));
    t.false(validateShareKey('aaaaaaaaaaaaaaaaaaa'));
    t.false(validateShareKey('aaaaaaaaaaaaaaa'));
    t.false(validateShareKey({}));
    t.false(validateShareKey(null));
    t.false(validateShareKey(true));
});
