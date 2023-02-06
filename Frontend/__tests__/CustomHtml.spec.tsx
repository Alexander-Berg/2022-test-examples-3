import * as React from 'react';
import { shallow } from 'enzyme';
import { CustomHtmlContent as CustomHtml } from '../CustomHtml';
import { CUSTOM_BLOCKS_MAP } from '../../../screens/ProductScreen/containers/Info';

const simpleContent = {
    content_type: 'paragraph',
    content: 'Почтой, 3-10 дней, 450 ₽. ',
};

const contenWithImage = [
    {
        content_type: 'paragraph',
        content: 'Состав набора:',
    },
    {
        content_type: 'paragraph',
        content: {
            content_type: 'image',
            type: 'inline',
            alt: 'sequin_art_logo',
            src: '//avatars.mds.yandex.net',
            height: 68,
            width: 150,
            content: null,
        },
    },
];

const contentWithNestedArrays = {
    text: {
        content_type: 'list',
        content: [
            [
                {
                    content_type: 'image',
                    type: 'inline',
                    alt: 'blue',
                    src: '//avatars.mds.yandex.net',
                    height: 24,
                    width: 25,
                    content: null,
                },
                ' разноцветные пайетки',
            ],
            [
                {
                    content_type: 'image',
                    type: 'inline',
                    alt: 'green',
                    src: '//avatars.mds.yandex.net/',
                    height: 24,
                    width: 25,
                    content: null,
                },
                ' булавки-гвоздики',
            ],
        ],
    },
};

describe('CustomHtml', () => {
    describe('С дефолтным списком блоков', () => {
        it('должен рендерится без ошибок с простым контентом', () => {
            shallow(<CustomHtml content={simpleContent} />);
        });

        it('должен рендерится без ошибок с контентом с изображением', () => {
            shallow(<CustomHtml content={contenWithImage} />);
        });

        it('должен рендерится без ошибок с контентом с вложенными массивами', () => {
            // @ts-ignore
            shallow(<CustomHtml content={contentWithNestedArrays} />);
        });
    });

    describe('С пользовательским списком блоков', () => {
        it('должен рендерится без ошибок с простым контентом', () => {
            shallow(
                <CustomHtml
                    mapBlocks={CUSTOM_BLOCKS_MAP}
                    content={simpleContent}
                />
            );
        });

        it('должен рендерится без ошибок с контентом с изображением', () => {
            shallow(
                <CustomHtml
                    mapBlocks={CUSTOM_BLOCKS_MAP}
                    content={contenWithImage}
                />
            );
        });

        it('должен рендерится без ошибок с контентом с вложенными массивами', () => {
            shallow(
                <CustomHtml
                    mapBlocks={CUSTOM_BLOCKS_MAP}
                    // @ts-ignore
                    content={contentWithNestedArrays}
                />
            );
        });
    });
});
