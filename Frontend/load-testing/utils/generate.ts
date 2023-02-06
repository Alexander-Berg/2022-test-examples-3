/* eslint-disable */
import { shuffle } from 'lodash';
import ms = require('ms');
import routesConfig from '../configs/routes';
import { keysOf, makeGeneratorGetter } from '../utils';
import {
    TestSuite,
    LoadSchemaType,
    AutostopSchemaType,
    LoadSchema,
    AutostopSchema,
    LoadProfile,
    TankConfigOptions,
} from '../types';
import { getSkillIds, getUserIds, getSkillSlugs, getSkillsMeta, getOauthAppsMeta } from '../db/utils';
import { DraftStatus } from '../../db/tables/draft';

const calculateShootingDuration = (suite: TestSuite) => {
    if (suite.loadProfile.duration) {
        return ms(suite.loadProfile.duration);
    }

    const schema = suite.loadProfile.schema!;

    return schema.reduce((acc, cur) => acc + Math.round(ms(cur.dur) / 1000), 0);
};

const processLoadSchema = (profile: LoadSchema, RPS: number) => {
    switch (profile.type) {
        case LoadSchemaType.Const:
            return `const(${Math.round(profile.loadMultiplier * RPS)},${profile.dur})`;

        case LoadSchemaType.Line:
            let from = Math.round(profile.fromMultiplier * RPS);
            let to = Math.round(profile.toMultiplier * RPS);

            return `line(${from},${to},${profile.dur})`;

        case LoadSchemaType.Step:
            from = Math.round(profile.fromMultiplier * RPS);
            to = Math.round(profile.toMultiplier * RPS);

            return `step(${from},${to},${profile.step},${profile.dur})`;
    }
};

const processAutostopSchema = (schema: AutostopSchema) => {
    switch (schema.type) {
        case AutostopSchemaType.Net:
            return `net(${schema.code}, ${schema.threshold}, ${schema.dur})`;

        case AutostopSchemaType.HTTP:
            return `http(${schema.code}, ${schema.threshold}, ${schema.dur})`;

        case AutostopSchemaType.Time:
            return `time(${schema.threshold}, ${schema.dur})`;
    }
};

const findWarmingUpThreshold = (schema: LoadSchema, RPS: number) => {
    switch (schema.type) {
        case LoadSchemaType.Const:
            return Math.round(RPS * schema.loadMultiplier);

        case LoadSchemaType.Line:
            return Math.round(RPS * schema.fromMultiplier);

        case LoadSchemaType.Step:
            return Math.round(RPS * schema.fromMultiplier);
    }
};

const findSlowingDownThreshold = (schema: LoadSchema, RPS: number) => {
    switch (schema.type) {
        case LoadSchemaType.Const:
            return Math.round(RPS * schema.loadMultiplier);

        case LoadSchemaType.Line:
            return Math.round(RPS * schema.toMultiplier);

        case LoadSchemaType.Step:
            return Math.round(RPS * schema.toMultiplier);
    }
};

const processLoadProfile = (profile: LoadProfile, RPS: number) => {
    if (profile.duration) {
        const { duration, warmingUp, slowingDown } = profile;

        return `line(${0}, ${RPS}, ${warmingUp}) const(${RPS}, ${duration}) line(${RPS}, ${0}, ${slowingDown})`;
    }

    if (!profile.schema || profile.schema.length === 0) {
        throw new Error('Schema must be non empty array of load profiles if *duration* is not specified');
    }

    const { schema, warmingUp: warmingUpDur, slowingDown: slowingDownDur } = profile;

    const warmingUpTo = findWarmingUpThreshold(schema[0], RPS);
    const slowingDownFrom = findSlowingDownThreshold(schema[schema.length - 1], RPS);

    return `line(0, ${warmingUpTo}, ${warmingUpDur}) ${schema
        .map(s => processLoadSchema(s, RPS))
        .join(' ')} line(${slowingDownFrom}, 0, ${slowingDownDur})`;
};

export const makeTankConfig = (options: TankConfigOptions) => {
    const { target, tank, schedule, autostopOptions, description, name, operator, ticket } = options;

    return `phantom:
  writelog: all
  enabled: true
  address: "${target}"
  ammo_type: phantom
  use_caching: false
  header_http: "1.0"
  load_profile:
    load_type: rps
    schedule: "${schedule}"
  package: yandextank.plugins.Phantom
  uris: []
uploader:
  enabled: true
  job_dsc: "${description}"
  job_name: "${name}"
  meta:
    use_tank: "${tank}"
    multitag: true
  operator: ${operator}
  package: yandextank.plugins.DataUploader
  task: ${ticket}
  ver: 1.4.0 #
telegraf:
  enabled: false
console:
  enabled: true
autostop:
  autostop:
${autostopOptions.map(o => `    - ${o}`).join('\n')}
    `;
};

export const makeCommonGeneratorOptions = async() => {
    const skillIds = await getSkillIds(1000);
    const userIds = await getUserIds(100);
    const adminIds = await getUserIds(100, true);
    const slugs = await getSkillSlugs(1000);
    const skillMeta = await getSkillsMeta({ limit: 1000 });
    const oauthAppMeta = await getOauthAppsMeta(100);
    const inDevelopmentSkillsMeta = await getSkillsMeta({ limit: 100, draftStatuses: [DraftStatus.InDevelopment] });
    const reviewRequestedSkillsMeta = await getSkillsMeta({ limit: 100, draftStatuses: [DraftStatus.ReviewRequested] });
    const reviewApprovedSkillsMeta = await getSkillsMeta({ limit: 100, draftStatuses: [DraftStatus.ReviewApproved] });
    const deployRequestedSkillsMeta = await getSkillsMeta({ limit: 100, draftStatuses: [DraftStatus.DeployRequested] });
    const skillsWithWebhookMeta = await getSkillsMeta({ limit: 100, withWebhook: true });
    const skillsWithOauthAppsMeta = await getSkillsMeta({ limit: 100, withOauthApp: true });

    const getSkillId = makeGeneratorGetter(skillIds);
    const getUserId = makeGeneratorGetter(userIds);
    const getAdminId = makeGeneratorGetter(adminIds);
    const getSlug = makeGeneratorGetter(slugs);
    const getSkillMeta = makeGeneratorGetter(skillMeta);
    const getOauthAppMeta = makeGeneratorGetter(oauthAppMeta);
    const getSkillMetaInDevelopment = makeGeneratorGetter(inDevelopmentSkillsMeta);
    const getSkillMetaReviewRequested = makeGeneratorGetter(reviewRequestedSkillsMeta);
    const getSkillMetaReviewApproved = makeGeneratorGetter(reviewApprovedSkillsMeta);
    const getSkillMetaDeployRequested = makeGeneratorGetter(deployRequestedSkillsMeta);
    const getSkillMetaWithWebhook = makeGeneratorGetter(skillsWithWebhookMeta);
    const getSkillWithOauthAppMeta = makeGeneratorGetter(skillsWithOauthAppsMeta);

    return {
        getSkillId,
        getUserId,
        getSlug,
        getSkillMeta,
        getOauthAppMeta,
        getSkillMetaInDevelopment,
        getSkillMetaReviewRequested,
        getSkillMetaReviewApproved,
        getSkillMetaDeployRequested,
        getSkillMetaWithWebhook,
        getAdminId,
        getSkillWithOauthAppMeta,
    };
};

export const processTestSuite = async(suite: TestSuite) => {
    const { routes, loadProfile, autostop } = suite;
    const commonGeneratorOptions = await makeCommonGeneratorOptions();

    const durationSec = calculateShootingDuration(suite);
    let ammoArray: string[] = [];
    let resultRPS = 0;

    for (const label of keysOf(routes)) {
        const route = routesConfig[label];

        const testCases = routes[label]!.testCases;

        for (const testCase of testCases) {
            const { RPS, generator, name, method } = testCase;

            // Генерируем патронов на стрельбу в течении двух минут или менее, чтобы избежать переполнения памяти
            const count = Math.min(durationSec, 120) * RPS;
            resultRPS += RPS;

            const ammoLabel = name ? `${name}_${method}_${label}` : `${method}_${label}`;

            for (let i = 0; i < count; i++) {
                const ammo = generator({
                    ammoLabel,
                    method,
                    route,
                    ...commonGeneratorOptions,
                });

                ammoArray.push(ammo);
            }
        }
    }

    ammoArray = shuffle(ammoArray);

    const schedule = processLoadProfile(loadProfile, resultRPS);
    const autostopOptions = autostop!.map(a => processAutostopSchema(a));

    return {
        tankConfig: makeTankConfig({
            autostopOptions,
            schedule,
            ...suite,
        }),
        ammo: ammoArray.join(''),
    };
};
