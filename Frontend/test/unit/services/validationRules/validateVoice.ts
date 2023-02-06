/* eslint-disable */
import test from 'ava';
import { validateVoice } from '../../../../services/validationRules';
import { Voice, defaultVoice } from '../../../../fixtures/voices';
import { Channel } from '../../../../db/tables/settings';
import FormValidationError from '../../../../db/errors';

test('validateVoice for organizationChat with non-default voice throws error', t => {
    for (const isAdmin of [true, false]) {
        t.throws(
            () =>
                validateVoice(Voice.Jane, {
                    isAdmin,
                    channel: Channel.OrganizationChat,
                }),
            {
                instanceOf: FormValidationError,
            },
        );
    }
});

test('validateVoice for organizationChat with default voice', t => {
    for (const isAdmin of [true, false]) {
        t.notThrows(() =>
            validateVoice(defaultVoice, {
                isAdmin,
                channel: Channel.OrganizationChat,
            }),
        );
    }
});

test('validateVoice for skill throws error for unknown voice', t => {
    for (const isAdmin of [true, false]) {
        t.throws(
            () =>
                validateVoice('unknown', {
                    isAdmin,
                    channel: Channel.AliceSkill,
                }),
            {
                instanceOf: FormValidationError,
            },
        );
    }
});

test('validateVoice for skill with public voice does not throw error', t => {
    for (const isAdmin of [true, false]) {
        t.notThrows(() =>
            validateVoice(Voice.Oksana, {
                isAdmin,
                channel: Channel.AliceSkill,
            }),
        );
    }
});
