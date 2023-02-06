/* eslint-disable */
import {v4 as uuid} from 'uuid';
import {
    Draft,
    DraftIntent,
    DraftMarketDevice,
    Image,
    NewsContent,
    NewsFeed,
    OAuthApp,
    PublishedIntent,
    PublishedMarketDevice,
    sequelize,
    ShowFeedModel,
    Skill,
    SkillUserShare,
    User,
} from '../../db';
import {ImageAttributes, ImageType} from '../../db/tables/image';
import {Channel} from '../../db/tables/settings';
import {SkillAttributes, SkillInstance} from '../../db/tables/skill';
import {UserAttributes, UserInstance} from '../../db/tables/user';
import {create} from '../../services/skill-lifecycle';
import {OAuthAppAttributes} from '../../db/tables/oauthApp';
import {defaultNotificationSettings} from '../../types';
import {DraftMarketDeviceAttributes, DraftMarketDeviceInstance,} from '../../db/tables/draftMarketDevice';
import {PublishedMarketDeviceAttributes, PublishedMarketDeviceInstance,} from '../../db/tables/publishedMarketDevice';
import * as lodash from 'lodash';
import {DraftIntentAttributes, DraftIntentInstance} from '../../db/tables/draftIntent';
import {PublishedIntentAttributes, PublishedIntentInstance,} from '../../db/tables/publishedIntent';
import {NewsFeedAttributes, NewsFeedType} from '../../db/tables/newsFeed';
import {NewsContentAttributes} from '../../db/tables/newsContent';
import {getPrivacyFromRawSources} from '../../services/skillPrivacy';
import * as randomString from 'crypto-random-string';
import {query} from '../../lib/pgPool';
import * as retry from 'retry-as-promised';
import {ShowFeedAttributes, ShowFeedType} from "../../db/tables/showFeed";

const getTables = () => {
    return retry(
        async () => {
            const tables: any[] = await query({
                type: 'promise',
                query: {
                    sql: `
                        SELECT *
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
            AND table_type = 'BASE TABLE'
            AND table_name != 'SequelizeMeta'
        ;
    `,
                },
            });

            return tables;
        },
        {
            max: 3,
            backoffBase: 1000,
            name: 'getTables',
        },
    );
};

const truncateTables = async (tables: any[]) => {
    const quotedTableNames = tables.map((table) => `"${table.table_name}"`).join(',');

    return retry(
        async () => {
            return await query({
                type: 'promise',
                query: {
                    sql: `TRUNCATE ${quotedTableNames} CASCADE;`,
                },
            });
        },
        {
            max: 3,
            backoffBase: 1000,
            name: 'truncateTable',
        },
    );
};

export const wipeDatabase = async () => {
    const tables = await getTables();

    await truncateTables(tables);
};

export const createUser = async (props: UserAttributes = {}) => {
    const {
        id = '0001',
        name = 'user',
        isBanned = false,
        featureFlags = {},
        roles = [],
        yandexTeamLogin = 'user',
        hasNewsSubscription = false,
        hasSubscription = true,
    } = props;

    const user = await User.create({
        id,
        name,
        isBanned,
        featureFlags,
        roles,
        yandexTeamLogin,
        hasNewsSubscription,
        hasSubscription,
    });

    return user;
};

let skillCounter = 0;

const getSkillPropsWithDefaults = (props: SkillAttributes) => {
    const {
        channel = Channel.AliceSkill,
        look = 'external',
        name = `skill ${++skillCounter}`,
        logo = '',
        activationPhrases = [],
        userId = '0001',
        featureFlags = [],
        backendSettings = {},
        publishingSettings = {},
        catalogRank = 1,
        useNLU = false,
        onAir,
        isRecommended = true,
        automaticIsRecommended = null,
        id: skillId = uuid(),
        slug = '',
        score = null,
        notificationSettings = defaultNotificationSettings,
        oauthAppId = null,
        inflectedActivationPhrases = [],
        appMetricaApiKey = null,
        publicShareKey = null,
        requiredInterfaces = [],
        tags = [],
    } = props;

    let { hideInStore, skillAccess } = props;

    const privacy = getPrivacyFromRawSources(skillAccess, hideInStore, channel);
    hideInStore = privacy.hideInStore;
    skillAccess = privacy.skillAccess;

    const isTrustedSmartHomeSkill = props.isTrustedSmartHomeSkill || false;

    const draft = {
        skillId,
        channel,
        name,
        logo,
        activationPhrases,
        featureFlags,
        backendSettings,
        publishingSettings,
        hideInStore,
        skillAccess,
        oauthAppId,
        inflectedActivationPhrases,
        isTrustedSmartHomeSkill,
        appMetricaApiKey,
        requiredInterfaces,
    };
    return {
        id: skillId,
        name,
        channel,
        look,
        userId,
        catalogRank,
        draft,
        useNLU,
        onAir,
        hideInStore,
        skillAccess,
        featureFlags,
        backendSettings,
        publishingSettings,
        isRecommended,
        automaticIsRecommended,
        isTrustedSmartHomeSkill,
        slug,
        score,
        notificationSettings,
        activationPhrases,
        oauthAppId,
        inflectedActivationPhrases,
        appMetricaApiKey,
        publicShareKey,
        requiredInterfaces,
        tags,
    };
};

export const createSkill = async (props: SkillAttributes = {}) => {
    const fullSkillProps = getSkillPropsWithDefaults(props);

    return await create(fullSkillProps);
};

export const buildSkill = (props: SkillAttributes = {}) => {
    const fullSkillProps = getSkillPropsWithDefaults(props);

    return Skill.build(fullSkillProps, {
        include: [Draft],
    });
};

/**
 * Эффективное создание большого количества скиллов.
 * Предполагается, что на момент вызова таблица skills не содержит записей.
 */
export const bulkCreateSkill = async (n: number, props: SkillAttributes = {}) => {
    await sequelize.transaction(async (t) => {
        const skills = [];
        const drafts = [];
        for (let i = 0; i < n; i++) {
            const skill = await getSkillPropsWithDefaults(props);
            drafts.push(skill.draft);
            skills.push(skill);
        }
        await Skill.bulkCreate(skills);
        await Draft.bulkCreate(drafts);
    });
    return Skill.scope('withAllRelations').findAll();
};

export const createImage = async (props: ImageAttributes = {}) => {
    const {skillId, type, url, origUrl} = props;

    const image = await Image.create({
        size: 0,
        skillId,
        type,
        url,
        origUrl,
    });

    return image;
};

export const createImageForSkill = (skill: SkillInstance, url?: string) => {
    return Image.create({
        size: 0,
        skillId: skill.id,
        type: ImageType.SkillSettings,
        url: url || 'http://localhost',
    });
};

export const createShareForSkill = (skill: SkillInstance, user: UserInstance) => {
    return SkillUserShare.create({
        skill_id: skill.id,
        user_id: user.id,
    });
};

export const createOAuthApp = (props: OAuthAppAttributes = {}) => {
    const { userId = '0001', name = 'OAuth app name', socialAppName = 'social app name' } = props;
    return OAuthApp.create({
        name,
        userId,
        socialAppName,
    });
};

export const createMarketDevice = <T extends boolean>(
    props: DraftMarketDeviceAttributes | PublishedMarketDeviceAttributes = {},
    params?: { isDraft?: T },
) => {
    return (params?.isDraft
        ? DraftMarketDevice.create(props)
        : PublishedMarketDevice.create(props)) as Promise<T extends true ? DraftMarketDeviceInstance : PublishedMarketDeviceInstance>;
};

interface DeviceParams {
    isDraft?: boolean;
    marketId: string;
}

export const createMarketDevices = (devices: DeviceParams[], skillId: string) => {
    return Promise.all(
        devices.map(({ isDraft, marketId }) =>
            createMarketDevice(
                {
                    marketId,
                    skillId,
                },
                { isDraft },
            ),
        ),
    );
};

export const isSameContent = (arr1: any[], arr2: any[]) => {
    return arr1.length === arr2.length && lodash.difference(arr2, arr1).length === 0;
};

export const createIntent = <T extends boolean>(
    props: DraftIntentAttributes | PublishedIntentAttributes = {},
    params?: { isDraft?: T },
) => {
    return (params?.isDraft ? DraftIntent.create(props) : PublishedIntent.create(props)) as Promise<T extends true ? DraftIntentInstance : PublishedIntentInstance>;
};

interface IntentParams {
    humanReadableName?: string;
    formName?: string;
    sourceText?: string;
    positiveTests?: string[];
    negativeTests?: string[];
    base64?: string;

    isDraft?: boolean;
}

export const createIntents = (intents: IntentParams[], skillId: string) => {
    return Promise.all(
        intents.map(({ isDraft, ...params }) =>
            createIntent(
                {
                    ...params,
                    skillId,
                },
                { isDraft },
            ),
        ),
    );
};

export const createFeed = (attributes: NewsFeedAttributes) => {
    const {
        description = null,
        enabled = true,
        iconUrl = null,
        name = 'name',
        type = NewsFeedType.FTP,
        skillId,
        preamble = null,
        topic = '',
        url = randomString(10),
        inflectedTopicPhrases = null,
        id = uuid(),
        depth = 1,
        settingsTypes,
    } = attributes;

    return NewsFeed.create({
        description,
        enabled,
        iconUrl,
        name,
        type,
        skillId,
        preamble,
        topic,
        url,
        inflectedTopicPhrases,
        id,
        depth,
        settingsTypes,
    });
};

export const createNewsContent = (attributes: NewsContentAttributes) => {
    const {
        detailsUrl = null,
        detailsText = null,
        feedId,
        id = uuid(),
        imageUrl = null,
        mainText = '',
        pubDate = new Date(),
        soundId,
        streamUrl = null,
        title = 'title',
        uid,
    } = attributes;

    return NewsContent.create({
        detailsUrl,
        detailsText,
        feedId,
        id,
        imageUrl,
        mainText,
        pubDate,
        soundId,
        streamUrl,
        title,
        uid,
    });
};

export const createShowFeed = (attributes: ShowFeedAttributes) => {
    const {
        id = uuid(),
        skillId,
        name = 'name',
        nameTts = null,
        description = "description",
        type = ShowFeedType.MORNING,
        onAir = true,
    } = attributes;

    return ShowFeedModel.create({
        id,
        skillId,
        name,
        nameTts,
        description,
        type,
        onAir,
    });
};
