/* eslint-disable */
import test from 'ava';
import { createUser, createSkill, createIntents, wipeDatabase } from '../../_helpers';
import { collectGrammarsFromIntents } from '../../../../services/granet';

const stubSourceText = 'root: привет';

const getValidIntentStubSetting = (formName: string) => ({
    sourceText: stubSourceText,
    formName,
    humanReadableName: 'test',
    base64: 'base64',
});

test.beforeEach(async t => {
    await wipeDatabase();
});

test('collectGrammars should collect grammars from valid intents without imports', async t => {
    await createUser();
    const skill = await createSkill();

    const intents = await createIntents(
        [
            {
                ...getValidIntentStubSetting('test1'),
                isDraft: true,
            },
            {
                ...getValidIntentStubSetting('test2'),
                isDraft: true,
            },
            {
                ...getValidIntentStubSetting('test3'),
                isDraft: true,
            },
        ],
        skill.id,
    );

    const grammars = collectGrammarsFromIntents(intents);

    const expected = {
        [intents[0].id + '.grnt']: `form ${skill.id}.${intents[0].formName}:\n    root: привет`,
        [intents[1].id + '.grnt']: `form ${skill.id}.${intents[1].formName}:\n    root: привет`,
        [intents[2].id + '.grnt']: `form ${skill.id}.${intents[2].formName}:\n    root: привет`,
    };

    t.deepEqual(grammars, expected);
});
