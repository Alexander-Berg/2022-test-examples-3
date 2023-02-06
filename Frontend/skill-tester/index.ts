/* eslint-disable @typescript-eslint/no-explicit-any */
import { IncomingHttpHeaders } from 'http';
import { timeout } from 'promise-timeout';
import type { Response as GotResponse } from 'got';
import { SkillInstance } from '../../db/tables/skill';
import { serializeSkillForExternal, SkillInfoV1 } from '../../serializers/skills';
import log from '../../services/log';
import { clientIdBySurface, EmulatableSurface, ImplicitSurface } from '../../services/surface';
import { createGot } from '../../services/got';
import config from '../../services/config';
import { SettingsSchema, BackendSettings } from '../../db/tables/settings';
import * as socialService from '../../services/social';
import { UserFeatureFlags } from '../../db/tables/user';
import { extractAudioPlayerCommand } from './blocks/audioPlayer';
import { extractCardAndButtons, processAudioPlayerBlockForResponse } from './utils';

const skillTesterGot = createGot({
    sourceName: 'skill_tester',
    sourceNameForTvm: 'dialogovo',
    useTvm: config.skillTester.useTvm,
});

const timestamp = () => Math.floor(Date.now() / 1000);

const meta = (utterance: any, uid: string, surface: EmulatableSurface) => ({
    epoch: timestamp(),
    tz: 'UTC',
    uuid: `dev-console-${uid}`,
    client_id: clientIdBySurface[surface],
    utterance,
    uid: Number(uid),
    experiments: {
        enable_ner_for_skills: '',
    },
});

const skillIdSlot = (skillId: string) => ({
    name: 'skill_id',
    type: 'skill',
    optional: false,
    value: skillId,
});

interface DescriptionSlotParams {
    skill: SkillInstance;
    webhookUrl?: string;
    functionId?: string;
    isDraft: boolean;
    userFeatureFlags: UserFeatureFlags;
    grammarsBase64: string | null;
    persistentUserIdSalt: string;
    useStateStorage: boolean;
    appMetricaApiKey: string | null;
    rsyPlatformId: string | null;
}

interface AdditionalSkillDescriptionParams {
    userFeatureFlags: UserFeatureFlags;
    grammarsBase64: string | null;
    persistentUserIdSalt: string;
    useStateStorage: boolean;
    appMetricaApiKey: string | null;
    rsyPlatformId: string | null;
}

type SlotName =
    | 'skill_description'
    | 'skill_meta'
    | 'request'
    | 'session'
    | 'skill_debug'
    | 'response';
type SlotType = 'skill' | 'string' | 'button' | 'session' | 'json' | 'response';

interface Slot<TName extends SlotName, TType extends SlotType, TValue> {
    name: TName;
    type: TType;
    optional: boolean;
    value: TValue;
}

interface SessionSlotValue {
    id: string;
    seq?: number;
    isEnded: boolean;
}

interface ResponseSlotValue {
    text: string | null;
    voice: string | null;
}

interface SkillDebugValue {
    response_raw: string | null;
    request: any;
}

type SkillDescriptionSlot = Slot<
    'skill_description',
    'skill',
    SkillInfoV1 & AdditionalSkillDescriptionParams
>;
type SkillMetaSlot = Slot<'skill_meta', 'skill', SkillMeta>;
type RequestSlot = Slot<'request', 'string', string>;
type RequestButtonSlot<T> = Slot<'request', 'button', { payload: T }>;
type SessionSlot = Slot<'session', 'session', SessionSlotValue>;
type SkillDebugSlot = Slot<'skill_debug', 'json', SkillDebugValue>;
type ResponseSlot = Slot<'response', 'response', ResponseSlotValue>;

type SkillTesterSlot =
    | SkillDescriptionSlot
    | SkillMetaSlot
    | RequestSlot
    | RequestButtonSlot<any>
    | SessionSlot
    | SkillDebugSlot
    | ResponseSlot;

const skillDescriptionSlot = ({
    skill,
    webhookUrl,
    isDraft,
    functionId,
    userFeatureFlags,
    grammarsBase64,
    appMetricaApiKey,
    rsyPlatformId,
    useStateStorage,
}: DescriptionSlotParams): SkillDescriptionSlot => {
    const value: SkillInfoV1 & AdditionalSkillDescriptionParams = {
        ...serializeSkillForExternal(skill),
        backendSettings: {
            uri: webhookUrl,
            functionId,
        },
        logo: {
            avatarId: 'blame/petrk',
            color: '#BADA55',
        },
        onAir: true,
        userFeatureFlags,
        grammarsBase64,
        persistentUserIdSalt: skill.persistentUserIdSalt,
        appMetricaApiKey,
        useStateStorage,
        rsyPlatformId,
    };

    if (isDraft) {
        value.accountLinking = skill.draft.oauthApp ?
            { applicationName: skill.draft.oauthApp.socialAppName } :
            null;
    }

    return {
        name: 'skill_description',
        type: 'skill',
        optional: false,
        value,
    };
};

interface SkillMeta {
    skillId: string;
    isDraft: boolean;
}

const skillMetaSlot = ({ skillId, isDraft }: SkillMeta): SkillMetaSlot => {
    return {
        name: 'skill_meta',
        type: 'skill',
        optional: false,
        value: {
            skillId,
            isDraft,
        },
    };
};

const getRequestSlot = (text: string): RequestSlot => ({
    name: 'request',
    type: 'string',
    optional: true,
    value: text,
});

const getRequestButtonSlot = <T>(payload: T): RequestButtonSlot<T> => ({
    name: 'request',
    type: 'button',
    optional: true,
    value: { payload },
});

const sessionSlot = (id: string, seq?: number): SessionSlot => ({
    name: 'session',
    type: 'session',
    optional: false,
    value: {
        id,
        seq,
        isEnded: false,
    },
});

const makeName = (sessionId?: string) => {
    return sessionId ?
        'personal_assistant.scenarios.external_skill__continue' :
        'personal_assistant.scenarios.external_skill';
};

const extractSlot = <T extends SkillTesterSlot, TName extends SlotName = T['name']>(
    response: GotResponse<any>,
    slotName: TName,
) => {
    const slots = response.body.form.slots as T[];

    const slot = slots.find(x => x.name === slotName);

    return slot?.value as T['value'] | undefined;
};

interface ErrorBlock {
    type: 'error';
    data: {
        problems: ErrorBlockProblem[];
    };
    error: ErrorBlockErrorInfo;
}

interface ErrorBlockProblem {
    path?: string | null;
    type: string;
    message: string;
    details?: string | null;
}

interface ErrorBlockErrorInfo {
    msg: string;
    type: string;
}

const isErrorBlock = (object: any): object is ErrorBlock => object.type === 'error';

enum PlaybackState {
    PLAYING = 'PLAYING',
    FINISHED = 'FINISHED',
    STOPPED = 'STOPPED',
}

interface AudioPlayerState {
    token: string;
    state: PlaybackState;
    offset_ms: number;
}

export enum AudioPlayerEventType {
    PlaybackStarted = 'AudioPlayer.PlaybackStarted',
    PlaybackFinished = 'AudioPlayer.PlaybackFinished',
    PlaybackNearlyFinished = 'AudioPlayer.PlaybackNearlyFinished',
    PlaybackStopped = 'AudioPlayer.PlaybackStopped',
    PlaybackFailed = 'AudioPlayer.PlaybackFailed',
}

export interface AudioPlayerEvent {
    type: AudioPlayerEventType;
    error?: {
        message: string;
        type: string;
    };
}

export interface TestSkillRequest {
    sessionId?: string;
    sessionSeq?: number;
    settings: BackendSettings;
    skillId: string;
    skillName: string;
    skillSalt: string;
    text: string;
    uid: string;
    userFeatureFlags: UserFeatureFlags;
    payload?: any;
    skill: SkillInstance;
    useZora: boolean;
    exposeInternalFlags: boolean;
    surface?: EmulatableSurface;
    directives?: Directive[];
    isDraft?: boolean;
    userTicket?: IncomingHttpHeaders[string];
    isAnonymousUser?: boolean;
    userIp: string;
    audioPlayerState?: AudioPlayerState;
    audioPlayerEvent?: AudioPlayerEvent;
    includeErrors?: boolean;
    location?: {
        lat: number;
        lon: number;
    };
}

function getHint(problem: ErrorBlockProblem): string | undefined {
    if (!problem.path) {
        return undefined;
    }

    if (typeof problem.path === 'string') {
        return problem.path.replace(/\/response\//gi, '').replace(/^\//gi, '');
    }

    return undefined;
}

function getProblemDescriptionFromData(problem: ErrorBlockProblem, error: ErrorBlockErrorInfo) {
    if (error.type === 'dialogovo_error') {
        return problem.message;
    }

    return undefined;
}

function getProblemDescriptionByType(code: string) {
    switch (code) {
        case 'external_skill_unavaliable':
            return 'URL диалога недоступен';
        case 'external_skill_deactivated':
            return 'Диалог неактивен';
    }

    return undefined;
}

function getProblems(errorBlock: ErrorBlock): Problem[] {
    const defaultErrorMessage = 'Ошибка сервера';

    if (errorBlock.data && errorBlock.data.problems && errorBlock.data.problems.length) {
        return errorBlock.data.problems.map(x => {
            return {
                description:
                    getProblemDescriptionFromData(x, errorBlock.error) || defaultErrorMessage,
                type: x.type,
                hint: getHint(x),
                details: x.details ?? undefined,
            };
        });
    }
    return [
        {
            description:
                    getProblemDescriptionByType(errorBlock.error.type) || defaultErrorMessage,
            type: errorBlock.error.type,
        },
    ];
}

export interface Directive {
    type: 'server_action' | 'client_action';
    name: string;
    payload: any;
}

interface BassAction {
    name: string;
    data: any;
}

const getAction = (directives: Directive[] | undefined): BassAction | undefined => {
    if (!directives) {
        return undefined;
    }

    const bassAction = directives.find(
        x => x.type === 'server_action' && x.name === 'bass_action',
    );
    if (!bassAction) {
        return undefined;
    }

    return bassAction.payload;
};

const audioPlayerStateSlot = (audioPlayerState?: AudioPlayerState) => {
    if (!audioPlayerState) {
        return;
    }

    return {
        name: 'audio_player',
        type: 'audio_player',
        value: {
            token: audioPlayerState.token,
            offset_ms: audioPlayerState.offset_ms,
            state: audioPlayerState.state,
        },
    };
};

const audioPlayerEventSlot = (audioPlayerEvent?: AudioPlayerEvent) => {
    if (!audioPlayerEvent) {
        return;
    }

    return {
        name: 'request',
        optional: 'true',
        type: 'audio_player_event',
        value: audioPlayerEvent,
    };
};

const emptyHistory: SkillDebugValue = { request: {}, response_raw: '' };

export const testSkill = async(params: TestSkillRequest) => {
    const {
        sessionId,
        sessionSeq,
        settings,
        skillId,
        text,
        uid,
        userFeatureFlags,
        payload,
        skill,
        surface = ImplicitSurface.Mobile,
        directives,
        isDraft = false,
        userTicket,
        isAnonymousUser,
        userIp,
        audioPlayerState,
        audioPlayerEvent,
        location,
        includeErrors = false,
    } = params;

    const info: SettingsSchema<any> = isDraft ? skill.draft : skill;

    const descriptionSlot = skillDescriptionSlot({
        skill,
        isDraft,
        webhookUrl: settings.uri,
        functionId: settings.functionId,
        userFeatureFlags,
        grammarsBase64: info.grammarsBase64,
        persistentUserIdSalt: skill.persistentUserIdSalt,
        appMetricaApiKey: info.appMetricaApiKey,
        useStateStorage: info.useStateStorage,
        rsyPlatformId: info.rsyPlatformId,
    });
    const socialAppName =
        descriptionSlot.value.accountLinking &&
        descriptionSlot.value.accountLinking.applicationName;
    const tokenPromise = timeout(
        socialAppName ?
            socialService.checkAvailableToken({
                userId: uid,
                applicationName: socialAppName,
                userIp,
            }) :
            Promise.resolve(false),
        1000,
    ).catch(() => false);

    const requestSlot = payload ? getRequestButtonSlot(payload) : getRequestSlot(text);

    const request = {
        meta: meta(text, uid, surface),
        form: {
            slots: [
                skillIdSlot(skillId),
                requestSlot,
                sessionId && sessionSlot(sessionId, sessionSeq),
                skillMetaSlot({ skillId, isDraft }),
                audioPlayerStateSlot(audioPlayerState),
                audioPlayerEventSlot(audioPlayerEvent),
            ].filter(Boolean),
            name: makeName(sessionId),
        },
        action: getAction(directives),
        ...(location ? { location: { ...location, accuracy: 15 } } : {}),
    };

    log.info('skill-tester:testSkill:request', { request, skillId });

    try {
        const response = await skillTesterGot<any>(config.skillTester.url, {
            method: 'post',
            responseType: 'json',
            json: request,
            headers: {
                'x-ya-user-ticket': isAnonymousUser ? undefined : userTicket,
            },
        });

        log.info('skill-tester:testSkill:response', { response: response.body, skillId });
        const errorBlocks: ErrorBlock[] = (response.body.blocks || []).filter(isErrorBlock);
        if (errorBlocks.length > 0) {
            // skill responded with an error
            const errorBlock = errorBlocks[0];

            let history: SkillDebugValue;
            try {
                history = extractSlot<SkillDebugSlot>(response, 'skill_debug') ?? emptyHistory;
            } catch (e) {
                // Ignore error
                history = emptyHistory;
            }

            let errorMessage = 'Ошибка сервера';
            const problems: Problem[] = errorBlock ? getProblems(errorBlock) : [];

            if (problems.length) {
                errorMessage = problems
                    .map((x, i) => {
                        return `${i + 1}. ${
                            x.description +
                            (x.hint ? `: ${x.hint}` : '') +
                            (x.details ? ` – ${x.details}` : '')
                        }`;
                    })
                    .join('\n');
            }

            return {
                error: errorMessage,
                history,
                problems: includeErrors ? problems : undefined,
                errorBlocks: includeErrors ? errorBlocks : undefined,
            };
        }
        const blocks = response.body.blocks || [];
        // valid skill response
        const session = extractSlot<SessionSlot>(response, 'session');
        const responseValue = extractSlot<ResponseSlot>(response, 'response');
        let responseText = responseValue?.text;
        const history = extractSlot<SkillDebugSlot>(response, 'skill_debug');
        const { card, buttons, isAccountLinkingRequest } = extractCardAndButtons(blocks, info);
        if (isAccountLinkingRequest) {
            responseText = `Чтобы пользоваться навыком «${info.name}», нужна авторизация.`;
        }

        const isLoggedIn = await tokenPromise;

        const playerBlock = extractAudioPlayerCommand(blocks);

        const audioPlayerAction = playerBlock ?
            processAudioPlayerBlockForResponse(playerBlock) :
            undefined;

        return {
            text: responseText,
            buttons,
            card,
            session,
            history,
            isAccountLinkingRequest,
            isLoggedIn,
            audioPlayerAction,
            layout: response.body.layout,
        };
    } catch (error) {
        log.info('skill-tester:testSkill:bass-error', { error, skillId });

        const history = { request: {}, response_raw: '' };

        return {
            error: 'Ошибка сервера',
            history,
            problems: includeErrors ? [] : undefined,
            errorBlocks: includeErrors ? [] : undefined,
        };
    }
};

interface Problem {
    type: string;
    description: string;
    hint?: string;
    details?: string;
}
