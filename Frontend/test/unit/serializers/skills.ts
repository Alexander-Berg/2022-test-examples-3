/* eslint-disable */
import test from 'ava';
import { Image, OAuthApp } from '../../../db';
import { SkillAttributes } from '../../../db/tables/skill';
import {
    serializeSkillForCatalog,
    serializeSkillForDevConsole,
    serializeSkillForExternal,
} from '../../../serializers/skills';
import { ImplicitSurface, Interface, Surface } from '../../../services/surface';
import { buildSkill } from '../../functional/_helpers';

const makeSkill = (props: SkillAttributes = {}) => {
    const skill = buildSkill({
        backendSettings: {
            uri: '',
        },
        ...props,
    });
    skill.logo2 = Image.build({
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    skill.createdAt = new Date(0);

    return skill;
};

test('serializeForCatalog returns implicit surfaces', t => {
    t.deepEqual(serializeSkillForCatalog(makeSkill()).surfaces, [
        ImplicitSurface.Desktop,
        ImplicitSurface.Mobile,
        Surface.Auto,
        Surface.Navigator,
        Surface.Station,
        Surface.Maps
    ]);

    t.deepEqual(
        serializeSkillForCatalog(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
            }),
        ).surfaces,
        [ImplicitSurface.Desktop, ImplicitSurface.Mobile],
    );
});

test('serializeForDevConsole does not return implicit surfaces', t => {
    t.deepEqual(serializeSkillForDevConsole(makeSkill(), false).surfaces, [
        Surface.Auto,
        Surface.Navigator,
        Surface.Station,
        Surface.Maps
    ]);

    t.deepEqual(
        serializeSkillForDevConsole(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
            }),
            false,
        ).surfaces,
        [],
    );
});

test('serializeForExternal does not return implicit surfaces', t => {
    t.deepEqual(serializeSkillForExternal(makeSkill()).surfaces, [Surface.Auto, Surface.Navigator, Surface.Station, Surface.Maps]);

    t.deepEqual(
        serializeSkillForExternal(
            makeSkill({
                requiredInterfaces: [Interface.Screen],
            }),
        ).surfaces,
        [],
    );
});

test('serializeForExternal returns accountLinking', t => {
    t.deepEqual(serializeSkillForExternal(makeSkill()).accountLinking, null);

    const applicationName = 'applicationName';

    const oauthApp = OAuthApp.build({
        socialAppName: applicationName,
    });

    const skill = makeSkill();
    skill.oauthApp = oauthApp;

    t.deepEqual(serializeSkillForExternal(skill).accountLinking, {
        applicationName,
    });
});
