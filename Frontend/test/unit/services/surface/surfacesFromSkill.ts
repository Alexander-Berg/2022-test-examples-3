/* eslint-disable */
import test from 'ava';
import {Interface, Surface, surfacesFromSkill} from '../../../../services/surface';
import {Channel, Surfaced} from '../../../../db/tables/settings';

type Factory<T> = (props: Partial<T>) => T;

const makeSkill: Factory<Surfaced> = props => ({
    channel: Channel.AliceSkill,
    exactSurfaces: [],
    surfaceWhitelist: [],
    surfaceBlacklist: [],
    requiredInterfaces: [],
    ...props,
});

test('no surfaces for chat', t => {
    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                channel: Channel.OrganizationChat,
            }),
        ),
        [],
    );

    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                channel: Channel.OrganizationChat,
                exactSurfaces: [Surface.Auto],
            }),
        ),
        [],
    );

    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                channel: Channel.OrganizationChat,
                surfaceWhitelist: [Surface.Auto],
            }),
        ),
        [],
    );
});

test('surfaces from interfaces', t => {
    t.deepEqual(surfacesFromSkill(makeSkill({})), [Surface.Auto, Surface.Navigator, Surface.Station, Surface.Maps]);

    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
            }),
        ),
        [],
    );
});

test('surfaces from exactSurfaces field', t => {
    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                exactSurfaces: [Surface.Auto],
            }),
        ),
        [Surface.Auto],
    );

    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                exactSurfaces: [Surface.Auto],
                surfaceWhitelist: [Surface.Navigator],
            }),
        ),
        [Surface.Auto],
    );
});

test('surfaces with whitelist', t => {
    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
                surfaceWhitelist: [Surface.Watch],
            }),
        ),
        [Surface.Watch],
    );
});

test('surfaces with blacklist', t => {
    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                surfaceBlacklist: [Surface.Station],
            }),
        ),
        [Surface.Auto, Surface.Navigator, Surface.Maps],
    );

    t.deepEqual(
        surfacesFromSkill(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
                surfaceWhitelist: [Surface.Station],
                surfaceBlacklist: [Surface.Station],
            }),
        ),
        [],
    );
});
