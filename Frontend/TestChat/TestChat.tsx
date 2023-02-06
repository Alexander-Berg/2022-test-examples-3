/* eslint-disable react/jsx-no-bind */
/* eslint-disable @typescript-eslint/no-use-before-define */
import React, { useReducer, useState, useRef, useCallback, useEffect } from 'react';
import { useGetSet } from 'react-use';
import block from 'propmods';
import { Button as LegoButton, CheckBox, Dropdown, Menu, Popup } from 'lego-on-react';
import * as keyset from 'i18n/TestPage';
import i18n from '@yandex-int/i18n';
import Login from './login.svg';
import ArrowTopIcon from './arrow-top.svg';
import CrossIcon from './cross.svg';
import { sessionStateReducer, initialSessionState } from '../../reducers/testChat/sessionState';
import {
    AudioPlaybackState,
    AudioPlayerState,
    AudioPlayerEvent,
    isErrorMessage,
    isAudioPlayerMessage,
    TextMessage,
    Button,
    TestChatHistory,
    CardActionArgs,
    AudioPlayerEventType,
    TextMessageSession,
    PlayerControlPhrases,
    Message,
} from '../../types/skillTest';
import { assistantMessage, CARD_TYPES, History, userMessage } from '../../lib/searchband';
import { Interface, getLeastCapableSurface } from '../../lib/surfaces';
import { skillTestApi } from '../../api';
import { TestPageHistoryHighlighter } from '../TestPageHistoryHighlighter/TestPageHistoryHighlighter';
import { H2 } from '../Typo/Typo';
import { openAccountLinkingWindow } from '../../utils/accountLinking';
import { TestAudioItem } from './TestAudioItem';
import { useMessageCardsGetter } from '../../hooks/TestChat/useMessageCardsGetter';
import { useTestAudioPlayer } from '../../hooks/TestChat/useTestAudioPlayer';
import { TestGeo } from './TestGeo';

import './TestChat.scss';

const b = block('TestChat');
const t = i18n(keyset);

interface SendMessageParams {
    text: string;
    payload?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    directives?: any[];
    audioPlayerEvent?: AudioPlayerEvent;
}

export type GeoPoint = {
    lat: number;
    lon: number;
};

interface Props {
    skillId: string;
    isDraft?: boolean;
}

const MissingInterfaceLabel = {
    [Interface.Screen]: t('chat-menu-option-no-screen'),
};

const makeTextMessage = (text: string, backgroundColor?: string, color?: string) => {
    const msg = assistantMessage({
        cards: [
            {
                type: CARD_TYPES.text,
                text,
            },
        ],
    });

    if (backgroundColor) {
        msg.backgroundColor = backgroundColor;
    }

    if (color) {
        msg.color = color;
    }

    return msg;
};

const makeErrorMessage = (text: string) => {
    return makeTextMessage(text, '#FF8888');
};

const makeServiceMessage = (text: string) => {
    return makeTextMessage(text, '#1ec4f9');
};

const getPrettifiedHistory = (history?: TestChatHistory): [string, string] => {
    if (!history) {
        return ['', ''];
    }

    const request = JSON.stringify(history.request, null, 2);

    let response = history.response_raw ?? '';
    try {
        response = history.response_raw ? JSON.stringify(JSON.parse(history.response_raw), null, 2) : '';
    } catch (err) {
        // ignore error
    }

    return [request, response];
};

interface TestChatHistoryBlockProps {
    history?: TestChatHistory;
}

const TestChatHistoryBlock: React.FC<TestChatHistoryBlockProps> = ({ history }) => {
    const [request, response] = getPrettifiedHistory(history);

    return (
        <div>
            <TestPageHistoryHighlighter data={response} placeholder="empty response" label="Response:" />
            <TestPageHistoryHighlighter data={request} label="Request:" />
        </div>
    );
};

const initialSession: TextMessageSession = {
    id: '',
    seq: 0,
    isEnded: false,
};

const getKeyboardButtons = (message: TextMessage) => (message.buttons || []).filter(button => button.hide);

export const TestChat: React.FC<Props> = ({ isDraft, skillId }) => {
    const [sessionState, dispatchSessionAction] = useReducer(sessionStateReducer, initialSessionState);
    const [isAnonymousUser, setIsAnonymousUser] = useState(false);
    const [supportedInterfaces, setSupportedInterfaces] = useState<Record<Interface, boolean>>({ screen: true });
    const { cardButtons } = useMessageCardsGetter();
    const sessionRef = useRef<TextMessageSession>(initialSession);
    const [isSessionStarted, setIsSessionStarted] = useState(false);
    const [isAudioError, setIsAudioError] = useState(false);
    const [delayedMessage, setDelayedMessage] = useState<Message>();

    const { isLoggedIn, buttons, messages, history } = sessionState;

    const [getSharingGeo, setSharingGeo] = useGetSet<GeoPoint | null>(null);

    const messagesRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const currentAudioPlayerStateRef = useRef<AudioPlayerState | undefined>();

    const unmounted = useRef(false);
    useEffect(
        () => () => {
            unmounted.current = true;
        },
        [],
    );
    const currentMessageText = useRef<string>('');

    const [showGeo, setShowGeo] = useState(false);

    const sendMessageUnhandled = useCallback(
        ({ text, payload, directives, audioPlayerEvent }: SendMessageParams) => {
            if (text) {
                const message = userMessage({ text });

                dispatchSessionAction({
                    type: 'Message',
                    payload: { message },
                });
            }

            const { id, seq } = sessionRef.current;
            const surface = getLeastCapableSurface(supportedInterfaces);
            const sharingGeo = getSharingGeo();
            const params = {
                skillId,
                text,
                isDraft: Boolean(isDraft),
                sessionId: id,
                sessionSeq: seq,
                payload,
                surface,
                directives,
                isAnonymousUser,
                // На каждый запрос в навык отправляем текущее состояние плеера
                audioPlayerState: currentAudioPlayerStateRef.current,
                audioPlayerEvent,
                ...sharingGeo ? { location: sharingGeo } : {},
            };

            return skillTestApi.sendMessage(params);
        },
        [supportedInterfaces, getSharingGeo, skillId, isDraft, isAnonymousUser],
    );

    const {
        audioEl,
        currentTrack,
        queue,
        onAudioResponse,
        init: initAudioPlayer,
        currentTime,
        duration,
        playbackState,
        isAudioMetaLoading,
        audioControls,
    } = useTestAudioPlayer({
        hooks: {
            onPlay: () =>
                sendAudioPlayerEvent({
                    type: AudioPlayerEventType.PlaybackStarted,
                }),
            onPause: () =>
                sendAudioPlayerEvent({
                    type: AudioPlayerEventType.PlaybackStopped,
                }),
            onFinish: async() => {
                void sendAudioPlayerEvent({
                    type: AudioPlayerEventType.PlaybackFinished,
                });

                if (delayedMessage) {
                    if (isErrorMessage(delayedMessage)) {
                        dispatchSessionAction({
                            type: 'ReplyErrorMessage',
                            payload: {
                                message: makeErrorMessage(delayedMessage.error),
                            },
                        });
                    } else {
                        dispatchSessionAction({
                            type: 'ReplyMessage',
                            payload: {
                                message: makeMessage(delayedMessage),
                                buttons: getKeyboardButtons(delayedMessage),
                                history: delayedMessage.history,
                                isLoggedIn: delayedMessage.isLoggedIn,
                            },
                        });
                    }

                    setDelayedMessage(undefined);
                }
            },
            onNearlyFinished: async() => {
                const message = await sendAudioPlayerEvent({
                    type: AudioPlayerEventType.PlaybackNearlyFinished,
                });

                if (unmounted.current) {
                    return;
                }

                setDelayedMessage(message);
            },
            onError: async(errorType, message) => {
                setIsAudioError(true);

                await sendAudioPlayerEvent({
                    type: AudioPlayerEventType.PlaybackFailed,
                    error: {
                        type: errorType,
                        message,
                    },
                });
            },
        },
    });

    useEffect(() => {
        setIsAudioError(false);
    }, [currentTrack]);

    useEffect(() => {
        if (currentTrack === null || playbackState === null) {
            return;
        }

        currentAudioPlayerStateRef.current = {
            token: currentTrack.token,
            state: playbackState,
            offset_ms: Math.floor(currentTime * 1000),
        };
    }, [currentTrack, playbackState, currentTime]);

    const isCurrentSessionMessage = useCallback((message: TextMessage) => {
        const isSubsequentMessage = sessionRef.current.id === message.session.id;
        const isInitialMessage = sessionRef.current.id === '' && message.session.seq === 0;

        return isSubsequentMessage || isInitialMessage;
    }, []);

    const sendAudioPlayerEvent = useCallback(
        async(event: AudioPlayerEvent) => {
            if (!__appConfig.skillTesting.audioPlayer.activeEvents[event.type]) {
                return;
            }

            dispatchSessionAction({
                type: 'Message',
                payload: {
                    message: makeServiceMessage(`Событие ${event.type}`),
                },
            });

            const message = await sendMessageUnhandled({
                text: '',
                audioPlayerEvent: event,
            });

            if (unmounted.current) {
                return;
            }

            if (!isErrorMessage(message) && !isCurrentSessionMessage(message)) {
                return;
            }

            if (message.history !== undefined && event.type !== AudioPlayerEventType.PlaybackStarted) {
                dispatchSessionAction({
                    type: 'SetHistory',
                    payload: {
                        history: message.history,
                    },
                });
            }

            if (isAudioPlayerMessage(message)) {
                onAudioResponse(message.audioPlayerAction, event.type);
            }

            return message;
        },
        [sendMessageUnhandled, onAudioResponse, isCurrentSessionMessage],
    );

    useEffect(() => {
        void sendMessage({ text: '' });

        inputRef.current?.focus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        setTimeout(() => {
            const messagesEl = messagesRef.current;
            if (messagesEl) {
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }
        });
    }, [messages, queue]);

    const makeMessage = useCallback((message: TextMessage) => assistantMessage(message.layout), []);

    const sendMessage = useCallback(
        async(params: SendMessageParams) => {
            try {
                const message = await sendMessageUnhandled(params);

                if (unmounted.current) {
                    return;
                }

                if (isErrorMessage(message)) {
                    dispatchSessionAction({
                        type: 'ReplyErrorMessage',
                        payload: {
                            message: makeErrorMessage(message.error),
                            history: message.history,
                        },
                    });

                    return;
                }

                if (!isCurrentSessionMessage(message)) {
                    return;
                }

                if (isAudioPlayerMessage(message)) {
                    onAudioResponse(message.audioPlayerAction);
                }

                const isSessionEnded = message.session.isEnded;

                sessionRef.current = isSessionEnded ? initialSession : message.session;

                setIsSessionStarted(!isSessionEnded);

                dispatchSessionAction({
                    type: 'ReplyMessage',
                    payload: {
                        message: makeMessage(message),
                        buttons: getKeyboardButtons(message),
                        history: message.history,
                        isLoggedIn: message.isLoggedIn,
                        trailingMessage: isSessionEnded ? makeServiceMessage('Сессия завершена') : undefined,
                    },
                });
            } catch (err) {
                console.error(err);

                let errorText = 'Что-то пошло не так.';

                if (err.code >= 500 && err.code < 600) {
                    errorText = 'Ошибка. Попробуйте ещё раз.';
                }

                if (err.code >= 400 && err.code < 500) {
                    errorText = 'Ошибка. Некорректный URL';
                }

                dispatchSessionAction({
                    type: 'ReplyErrorMessage',
                    payload: {
                        message: makeErrorMessage(errorText),
                        history: { request: {}, response_raw: '' },
                    },
                });
            }
        },
        [sendMessageUnhandled, isCurrentSessionMessage, makeMessage, onAudioResponse],
    );

    const onSubmitGeo = useCallback(async(geo: GeoPoint) => {
        setShowGeo(false);
        setSharingGeo(geo);
        await sendMessage({ text: currentMessageText.current });
    }, [sendMessage, setSharingGeo]);

    const onCloseGeo = React.useCallback(() => {
        setShowGeo(false);
    }, []);

    const onRemoveGeo = useCallback(() => {
        setSharingGeo(null);
    }, [setSharingGeo]);

    const toggleSupportedInterface = useCallback(
        (i: Interface) => {
            const isSupported = !supportedInterfaces[i];

            setSupportedInterfaces({
                ...supportedInterfaces,
                [i]: isSupported,
            });
        },
        [supportedInterfaces],
    );

    const logout = useCallback(async() => {
        const errorMessage = makeErrorMessage('Не удалось выйти из навыка');

        try {
            const { result } = await skillTestApi.logoutFromSkill({
                isDraft: Boolean(isDraft),
                skillId,
            });

            if (unmounted.current) {
                return;
            }

            const message = result ? makeTextMessage('Вы успешно вышли из навыка') : errorMessage;

            dispatchSessionAction({
                type: 'LogoutMessage',
                payload: {
                    isLoggedIn: !result,
                    message,
                },
            });
        } catch (e) {
            dispatchSessionAction({
                type: 'LogoutMessage',
                payload: {
                    isLoggedIn,
                    message: errorMessage,
                },
            });
        }
    }, [isDraft, isLoggedIn, skillId]);

    const openAccountLinking = useCallback(
        (url: string) => {
            const onSuccess = async() => {
                const directive = {
                    type: 'server_action',
                    name: 'bass_action',
                    payload: {
                        name: 'external_skill__account_linking_complete',
                        data: {},
                    },
                };

                await sendMessage({ text: '', directives: [directive] });
            };

            const onFailure = () => {
                dispatchSessionAction({
                    type: 'Message',
                    payload: {
                        message: makeErrorMessage('Произошла ошибка авторизации'),
                    },
                });
            };

            openAccountLinkingWindow({ url, onSuccess, onFailure });
        },
        [sendMessage],
    );

    const onButtonClick = useCallback(
        async(button: Button, hide = false) => {
            if (button.type === 'start_account_linking' && button.url) {
                openAccountLinking(button.url);
                return;
            }

            if (button.type === 'request_geosharing' && button.period_min) {
                setShowGeo(true);
                return;
            }

            if (button.url) {
                window.open(button.url);
            }

            if (hide) {
                dispatchSessionAction({ type: 'HideButtons' });
            }

            await sendMessage({ text: button.title ?? currentMessageText.current, payload: button.payload });
        },
        [openAccountLinking, sendMessage],
    );

    const handleMessageAction = useCallback(
        (arg: CardActionArgs) => setTimeout(async() => {
            const { action } = arg;
            switch (action.type) {
                case 'button': {
                    const directive = action.directives[0];
                    if (directive) {
                        return onButtonClick(directive, directive.hide);
                    }
                    return sendMessage({ text: currentMessageText.current });
                }
                case 'uri': {
                    const button = action.url ? cardButtons.current[action.url] : null;

                    if (button) {
                        await sendMessage({ text: button.text, payload: button.payload });
                    } else {
                        window.open(action.url);
                    }
                    break;
                }
                case 'dialog-action': {
                    const [button] = action.directives;
                    await sendMessage({ text: button.text, payload: button.payload });
                    break;
                }
            }
        }),
        [cardButtons, onButtonClick, sendMessage],
    );

    const handleFormSubmit = useCallback(
        async(event: React.FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            const message = inputRef.current?.value;

            if (message && inputRef.current) {
                inputRef.current.value = '';
                inputRef.current.focus();

                await sendMessage({ text: message });
            }
        },
        [sendMessage],
    );

    const handleFormReset = useCallback(async() => {
        dispatchSessionAction({ type: 'ResetSessionState' });
        setIsSessionStarted(false);
        sessionRef.current = initialSession;

        if (playbackState === AudioPlaybackState.PLAYING) {
            await sendAudioPlayerEvent({ type: AudioPlayerEventType.PlaybackStopped });
        }

        initAudioPlayer();
        currentAudioPlayerStateRef.current = undefined;
        // Инициализируем новую сессию
        await sendMessage({ text: '' });
    }, [initAudioPlayer, playbackState, sendAudioPlayerEvent, sendMessage]);

    const displayAnonymousUserOption =
        Boolean(__appConfig.app.displayAnonymousUserOption) ||
        Boolean(sessionStorage.getItem('__display-anonymous-option')) ||
        Boolean(__appConfig.user.featureFlags.stateStorage);

    const sharingGeo = getSharingGeo();

    return (
        <div {...b()}>
            <div {...b('main')}>
                <div {...b('chatColumn')}>
                    <H2>{t('header-chat')}</H2>
                    <div {...b('head-toolbar')}>
                        <div {...b('options')}>
                            <Dropdown
                                switcher={
                                    <LegoButton size="s" theme="normal">
                                        {t('chat-menu-settings')}
                                    </LegoButton>
                                }
                                hasTick
                                theme="normal"
                                popup={
                                    <Popup target="anchor">
                                        <Menu theme="normal" size="m" type="navigation">
                                            <Menu.Item>
                                                <CheckBox
                                                    theme="normal"
                                                    size="m"
                                                    tone="default"
                                                    view="default"
                                                    name={Interface.Screen}
                                                    checked={!supportedInterfaces[Interface.Screen]}
                                                    onChange={() => toggleSupportedInterface(Interface.Screen)}
                                                >
                                                    {MissingInterfaceLabel[Interface.Screen]}
                                                </CheckBox>
                                            </Menu.Item>
                                            {displayAnonymousUserOption && (
                                                <Menu.Item>
                                                    <CheckBox
                                                        theme="normal"
                                                        size="m"
                                                        tone="default"
                                                        view="default"
                                                        name="isAnonymousUser"
                                                        checked={isAnonymousUser}
                                                        onChange={() => setIsAnonymousUser(!isAnonymousUser)}
                                                    >
                                                        {t('chat-menu-settings')}
                                                    </CheckBox>
                                                </Menu.Item>
                                            )}
                                        </Menu>
                                    </Popup>
                                }
                            />
                        </div>
                        {sharingGeo && (
                            <Dropdown
                                switcher={
                                    <LegoButton size="s" theme="link">
                                        {t('chat-menu-geo')}: {sharingGeo.lat}, {sharingGeo.lon}
                                    </LegoButton>
                                }
                                hasTick
                                theme="normal"
                                popup={
                                    <Popup target="anchor">
                                        <Menu theme="normal" size="m" type="navigation">
                                            <Menu.Item onClick={onRemoveGeo}>{t('chat-geo-disable')}</Menu.Item>
                                        </Menu>
                                    </Popup>
                                }
                            />
                        )}
                        {isLoggedIn && (
                            <Dropdown
                                switcher={
                                    <LegoButton size="s" theme="link">
                                        <Login style={{ verticalAlign: 'sub' }} /> {t('chat-you-logged')}
                                    </LegoButton>
                                }
                                hasTick
                                theme="normal"
                                popup={
                                    <Popup target="anchor">
                                        <Menu theme="normal" size="m" type="navigation">
                                            <Menu.Item onClick={logout}>{t('chat-exit-skill')}</Menu.Item>
                                        </Menu>
                                    </Popup>
                                }
                            />
                        )}
                    </div>
                    <div {...b('chat-components')}>
                        <div {...b('chat')}>
                            <div
                                {...b('messages')}
                                ref={messagesRef}
                                onClick={e => {
                                    // Bugfix https://st.yandex-team.ru/PASKILLS-2512
                                    const target = e.target as HTMLElement;
                                    if (target && target.classList.contains('assistant__message__card__text__action')) {
                                        currentMessageText.current = target.textContent ?? '';
                                        e.preventDefault();
                                    }
                                }}
                            >
                                <History messages={messages} onAction={handleMessageAction} />
                            </div>
                            {buttons.length > 0 && (
                                <div {...b('suggest')}>
                                    {buttons.map((button, index) => (
                                        <button
                                            {...b('action', { external: Boolean(button.url) })}
                                            key={index}
                                            onClick={() => onButtonClick(button, true)}
                                        >
                                            {button.title}
                                        </button>
                                    ))}
                                </div>
                            )}
                            <div {...b('toolbar')}>
                                <form {...b('form')} onSubmit={handleFormSubmit}>
                                    <input {...b('input')} ref={inputRef} placeholder={t('input-placeholder')} />
                                    <button {...b('submit')} type="submit" title={t('button-label-send')}>
                                        <ArrowTopIcon />
                                    </button>
                                </form>

                                <div {...b('actions')}>
                                    {isSessionStarted && (
                                        <button {...b('reset')} onClick={handleFormReset} title={t('button-label-delete')}>
                                            <CrossIcon />
                                        </button>
                                    )}
                                </div>
                            </div>
                        </div>
                        {currentTrack !== null && (
                            <div {...b('player')}>
                                <TestAudioItem
                                    track={currentTrack}
                                    currentTime={isAudioError ? 0 : currentTime}
                                    duration={isAudioError ? 0 : duration}
                                    isPlaying={playbackState === AudioPlaybackState.PLAYING}
                                    isLoading={isAudioMetaLoading}
                                    onPlay={() => {
                                        void sendMessage({ text: PlayerControlPhrases.Continue });
                                    }}
                                    onPause={() => {
                                        void audioControls.pause();
                                    }}
                                    onSeek={newCurrentTime => {
                                        audioControls.pause();
                                        audioControls.seek(newCurrentTime);
                                    }}
                                    onNextTrack={() => {
                                        void sendMessage({ text: PlayerControlPhrases.NextTrack });
                                    }}
                                    onPrevTrack={() => {
                                        void sendMessage({ text: PlayerControlPhrases.PrevTrack });
                                    }}
                                    onSeekEnd={() => audioControls.play()}
                                />
                                {queue.map((track, i) => {
                                    return (
                                        <TestAudioItem
                                            track={track}
                                            key={i}
                                            preload
                                            onPlay={() => {
                                                void sendMessage({ text: PlayerControlPhrases.NextTrack });
                                            }}
                                        />
                                    );
                                })}
                            </div>
                        )}
                        {audioEl}
                    </div>
                </div>
                {history && (
                    <div {...b('codeColumn')}>
                        <H2>{t('header-last-request')}</H2>
                        <div {...b('code')}>
                            <TestChatHistoryBlock history={history} />
                        </div>
                    </div>
                )}
            </div>
            {showGeo && <TestGeo onCancel={onCloseGeo} onSubmit={onSubmitGeo} />}
        </div>
    );
};
