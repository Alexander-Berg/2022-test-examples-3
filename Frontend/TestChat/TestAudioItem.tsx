import React, { useRef, useLayoutEffect } from 'react';
import block from 'propmods';
import clamp from 'lodash/clamp';
import { AudioProgress } from '../AudioProgress/AudioProgress';
import { TestAudioTrack } from '../../types/skillTest';
import { formatSecondsForDuration } from '../../utils/time';
import { PlayIcon } from '../../icons/play';
import { PauseIcon } from '../../icons/pause';
import { Loading } from '../Loading/Loading';
import { SwitchTrackIcon } from '../../icons/switch-track';

import './TestAudioItem.scss';

const b = block('TestAudioItem');

interface Props {
    track: TestAudioTrack;
    isPlaying?: boolean;
    duration?: number;
    currentTime?: number;
    isLoading?: boolean;
    preload?: boolean;
    onPlay?: () => void;
    onPause?: () => void;
    onSeek?: (currentTime: number) => void;
    onSeekEnd?: () => void;
    onNextTrack?: () => void;
    onPrevTrack?: () => void;
}

export const TestAudioItem: React.FC<Props> = ({
    track,
    duration = 0,
    currentTime = 0,
    isPlaying = false,
    isLoading = false,
    preload = false,
    onPause,
    onPlay,
    onSeek,
    onSeekEnd,
    onNextTrack,
    onPrevTrack,
}) => {
    const { title, logoUrl } = track;
    const audioRef = useRef<HTMLAudioElement>(null);

    const iconStyle: React.CSSProperties = {
        backgroundImage: `url(${logoUrl})`,
        backgroundSize: 'cover',
    };

    const durationString = formatSecondsForDuration(duration);
    const currentTimeString = formatSecondsForDuration(currentTime);

    const showProgress = duration > 0 && !isLoading;

    const ControlIcon = isPlaying ? PauseIcon : PlayIcon;
    const Icon = isLoading ? <Loading size="xxs" /> : <ControlIcon size="m" color="#fff" transparentControl />;

    const onIconClick = isPlaying ? onPause : onPlay;

    const innerDuration = duration === 0 ? 1 : duration;

    const preloadAudio = preload && Boolean(track);

    useLayoutEffect(() => {
        if (preloadAudio && audioRef.current) {
            audioRef.current.currentTime = track.offsetSec;
        }
    }, [track, preloadAudio]);

    return (
        <div {...b()}>
            <div {...b('icon')} style={iconStyle} onClick={onIconClick}>
                {Icon}
            </div>
            <div {...b('controls')}>
                <div {...b('control-prev')} onClick={onPrevTrack}>
                    <SwitchTrackIcon type="prev" size="xs" />
                </div>
                <div {...b('control-next')} onClick={onNextTrack}>
                    <SwitchTrackIcon type="next" size="xs" />
                </div>
            </div>
            <div {...b('body')}>
                <div {...b('title-wrapper')}>
                    <div {...b('title', { showProgress })}>{title}</div>
                    {showProgress && (
                        <div {...b('time')}>
                            {currentTimeString} / {durationString}
                        </div>
                    )}
                </div>
                {/* Элемент audio нужен для фоновой загрузки звуков при добавлении трека в очередь */}
                {preloadAudio && <audio hidden src={track.url} key={track.url} preload="auto" ref={audioRef} />}
                {!isLoading && (
                    <div {...b('progress')}>
                        <AudioProgress
                            duration={innerDuration}
                            // range взрывается, если передать ему значения выходящие за диапазон допустимых
                            value={clamp(currentTime, 0, innerDuration)}
                            trackColorActive="#6839CF"
                            trackColorInactive="#C8CDD9"
                            onChange={onSeek}
                            onFinalChange={onSeekEnd}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};
