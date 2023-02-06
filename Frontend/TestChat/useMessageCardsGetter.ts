/* eslint-disable */
import { useRef, useCallback } from 'react';
import { Button, TextMessage, Card, TextCard, DivCardBlock, CardType, MessageCard } from '../../types/skillTest';
import {
    BACKGROUND_TYPE,
    BLOCK,
    CARD_TYPES,
    ELEMENT,
    POSITION,
    SIZE,
    PREDEFINED_TYPE,
    NUMERIC_TYPE,
    FRAME_STYLE,
    CONTAINER_DIRECTION,
    TEXT_STYLE,
    PREDEFINED_SIZE,
} from '../../lib/searchband';

export const useMessageCardsGetter = () => {
    const cardButtons = useRef<Record<string, any>>({});
    const cardButtonSeqId = useRef(0);

    const fixButton = useCallback((button: Button) => {
        if (button && !button.url) {
            const url = 'dialog://url-stub/' + cardButtonSeqId.current++;
            button.url = url;

            cardButtons.current[url] = button;
        }

        return button;
    }, []);

    const renderDivCard = (card: MessageCard) => {
        let blocks: DivCardBlock[] = [];
        const data = card.data;
        switch (card.card_template) {
            case CardType.BigImage:
                blocks = [
                    {
                        image: {
                            image_url: data.image_url,
                            ratio: 2.24,
                        },
                        type: BLOCK.image,
                        action: data.button,
                    },
                    {
                        size: SIZE.xxs,
                        type: BLOCK.separator,
                    },
                    {
                        title: data.title,
                        text: `<font color="#818181">${data.description || ''}</font>`,
                        type: BLOCK.universal,
                        action: data.button,
                    },
                    {
                        size: SIZE.xs,
                        type: BLOCK.separator,
                        action: data.button,
                    },
                ];
                break;
            case CardType.ImageGallery:
                blocks = [
                    {
                        items: [
                            ...data.items.map((x: any) => ({
                                children: [
                                    {
                                        children: [
                                            {
                                                image: {
                                                    image_url: x.image_url,
                                                    ratio: 0.7,
                                                    type: ELEMENT.image,
                                                },
                                                type: BLOCK.image,
                                            },
                                        ],
                                        direction: CONTAINER_DIRECTION.vertical,
                                        width: {
                                            type: NUMERIC_TYPE,
                                            value: 320.0,
                                            // unit: 'dp',
                                        },
                                        height: {
                                            type: PREDEFINED_TYPE,
                                            value: PREDEFINED_SIZE.wrapContent,
                                        },
                                        frame: {
                                            style: FRAME_STYLE.onlyRoundCorners,
                                        },
                                        type: BLOCK.container,
                                    },
                                    {
                                        size: SIZE.xxs,
                                        type: BLOCK.separator,
                                    },
                                    {
                                        title: x.title,
                                        title_style: TEXT_STYLE.titleS,
                                        action: x.button,
                                        type: BLOCK.universal,
                                    },
                                ],
                                direction: CONTAINER_DIRECTION.vertical,
                                width: {
                                    type: PREDEFINED_TYPE,
                                    value: PREDEFINED_SIZE.wrapContent,
                                },
                                height: {
                                    type: PREDEFINED_TYPE,
                                    value: PREDEFINED_SIZE.wrapContent,
                                },
                                frame: {
                                    style: FRAME_STYLE.onlyRoundCorners,
                                },
                                type: BLOCK.container,
                            })),
                        ],
                        type: BLOCK.gallery,
                    },
                ];
                break;
            case CardType.ItemsList:
                blocks = [
                    data.header && {
                        title: data.header.text,
                        type: BLOCK.universal,
                        action: data.button,
                    },
                    {
                        size: SIZE.xs,
                        type: BLOCK.separator,
                    },
                    ...data.items.map((x: any) => ({
                        title: x.title,
                        text: `<font color="#818181">${x.description || ''}</font>`,
                        type: BLOCK.universal,
                        action: fixButton(x.button),
                        side_element: {
                            element: {
                                image_url: x.image_url,
                                ratio: 1,
                                type: ELEMENT.image,
                            },
                            size: SIZE.m,
                            position: POSITION.left,
                        },
                    })),
                    {
                        size: SIZE.xs,
                        type: BLOCK.separator,
                    },
                    data.footer && {
                        text: `<font color="#0A4DC3">${data.footer.text || ''}</font>`,
                        type: BLOCK.footer,
                        action: fixButton(data.footer.button),
                    },
                ].filter(Boolean);
                break;
        }
        return blocks;
    };

    const getCards = useCallback(
        (message: TextMessage): Card[] => {
            const card = message.card;
            const inlineButtons = (message.buttons || []).filter(button => !button.hide);
            const textBlocks = [
                {
                    type: CARD_TYPES.textWithButton,
                    text: message.text.replace(/\\n/g, '\n'),
                    buttons: inlineButtons.map(x => ({
                        title: x.title,
                        directives: [x],
                    })),
                } as TextCard,
            ];
            if (!card) {
                return textBlocks;
            }

            let blocks: DivCardBlock[] = [];
            blocks = renderDivCard(card);
            fixButton(card.data.button);

            const background = [
                {
                    color: '#FFFFFF',
                    type: BACKGROUND_TYPE.solid,
                },
            ];

            const state = {
                state_id: 0,
                blocks,
            };

            return [
                {
                    type: CARD_TYPES.div,
                    state_id: 0,
                    body: {
                        states: [state],
                        background,
                    },
                },
            ];
        },
        [fixButton],
    );

    return { getCards, cardButtons };
};
