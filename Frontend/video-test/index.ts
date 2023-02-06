import { storyId } from 'blocks/stories/story-id';

import { StoryConfig } from 'types/stories';
import { ShowContentConditionType, VideoFormat } from 'types/stories/story-background-video';
import { StoryItemVideoConfig } from 'types/stories/story-item-custom';
import { StoryItemType } from 'types/stories/story-item-type';

const id = storyId('videoTest');

const stickbug = (sid: string): StoryItemVideoConfig => ({
    id: id(sid),
    type: StoryItemType.VIDEO,
    options: {
        backgroundVideo: {
            videoSrc: {
                // по-моему файл не очень правильно закодирован, или плохо пережил арк, мой новый ноут его не ест :-/
                // [StoryVideoFormat.AV1]: require('./assets/1_av1.mp4').default,
                [VideoFormat.VP9]: require('./assets/1_vp9.webm').default,
                [VideoFormat.H265]: require('./assets/1_h265.mp4').default,
                [VideoFormat.H264]: require('./assets/1_h264.mp4').default,
            },
            showContentCondition: {
                type: ShowContentConditionType.TIMECODE,
                timeCode: 5,
            },
        },
        title: ('Get Stick Bugged Lol'),
        paragraphs: [{ text: ('Я не очень в тестовые данные') }],
        beta: true,
    },
});

export const videoTest = (): StoryConfig => {
    const items: StoryItemVideoConfig[] = [];

    items.push(stickbug('stickbug'));
    items.push(stickbug('another-stickbug'));

    return ({
        id: id(),
        miniature: {
            backgroundImage: {
                resolutions: {
                    x1: require('./assets/thumb.png').default,
                    x2: require('./assets/thumb.png').default,
                    x3: require('./assets/thumb.png').default,
                },
                thumbnails: {
                    x1: require('./assets/thumb.png').default,
                    x2: require('./assets/thumb.png').default,
                    x3: require('./assets/thumb.png').default,
                },
            },
            title: ('What is this?'),
            shortTitle: ('OwO'),
        },
        items,
        beta: true,
    });
};
