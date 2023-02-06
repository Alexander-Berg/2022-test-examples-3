import React, { useState } from 'react';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IProductImage } from '@src/typings/productImage';
import { RadioThumbs } from '../RadioThumbs@desktop';

const images: IProductImage[] = [{
    src: '//avatars.mds.yandex.net/get-mpic/5150634/img_id469305697924921366.png/900x1200',
    srcHd: '//avatars.mds.yandex.net/get-mpic/5150634/img_id469305697924921366.png/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/5280162/img_id246572731244694980.png/600x600',
    srcHd: '//avatars.mds.yandex.net/get-mpic/5280162/img_id246572731244694980.png/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/4725655/img_id2393729985120199151.png/600x600',
    srcHd: '//avatars.mds.yandex.net/get-mpic/4725655/img_id2393729985120199151.png/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/4860193/img_id7591099002096571342.png/600x600',
    srcHd: '//avatars.mds.yandex.net/get-mpic/4860193/img_id7591099002096571342.png/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/4519356/img_id7797142052940791146.jpeg/600x600',
    srcHd: '//avatars.mds.yandex.net/get-mpic/4519356/img_id7797142052940791146.jpeg/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/5251330/img_id6152274662578312880.jpeg/600x600',
    srcHd: '//avatars.mds.yandex.net/get-mpic/5251330/img_id6152274662578312880.jpeg/600x600',
}, {
    src: '//avatars.mds.yandex.net/get-mpic/5220508/img_id6588039446420037738.jpeg/8hq',
    srcHd: '//avatars.mds.yandex.net/get-mpic/5220508/img_id6588039446420037738.jpeg/600x600',
}];

createPlatformStories(
    'Tests/RadioThumbs',
    {
        desktop: RadioThumbs,
    },
    stories => {
        stories
            .add('plain', RadioThumbs => {
                const [current, setCurrent] = useState(0);

                return (
                    <div>
                        <div>Active: {current}</div>
                        <RadioThumbs
                            images={images}
                            current={current}
                            navigate={setCurrent}
                        />
                    </div>
                );
            });
    },
);
