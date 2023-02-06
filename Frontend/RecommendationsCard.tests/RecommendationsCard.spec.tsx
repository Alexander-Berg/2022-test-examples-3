import * as React from 'react';
import { shallow } from 'enzyme';

import { Link } from '@yandex-turbo/components/Link/Link';
import { EStorageTypes } from '@yandex-turbo/core/utils/recommendations/appropriateImageSource';
import { TurboCard } from '@yandex-turbo/components/TurboCard/TurboCard';

import { RecommendationsCard } from '../RecommendationsCard';
import { RecommendationsCardAction } from '../Action/RecommendationsCard__Action';
import { RecommendationsCardImage } from '../Image/RecommendationsCard__Image';
import { RecommendationsCardMeta } from '../Meta/RecommendationsCard__Meta';
import { RecommendationsCardSkeleton } from '../Skeleton/RecommendationsCard__Skeleton';
import { recommendationsCardCn } from '../RecommendationsCard.cn';
import { IRecommendationsCardProps, ERecommendationsCardTypes, EImagePosition } from '../RecommendationsCard.types';

describe('Компонент RecommendationsCard', () => {
    test('Маленькая карточка рендерится без ошибок c минимально необходимыми props', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);

        expect(wrapper.exists()).toBe(true);
    });

    test('Маленькая карточка с кнопкой рендерится без ошибок c минимально необходимыми props', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);

        expect(wrapper.exists()).toBe(true);
    });

    test('Большая карточка с кнопкой рендерится без ошибок c минимально необходимыми props', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);

        expect(wrapper.exists()).toBe(true);
    });

    test('При передаче titleNode стандартный заголовок заменяется переданным компонентом', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            titleNode: (<div className="custom-title" key="123">Я заголовок</div>),
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const defaultTitle = wrapper.find(recommendationsCardCn('title'));
        const customTitle = wrapper.find('.custom-title');

        expect(defaultTitle.exists()).toBe(false);
        expect(customTitle.exists()).toBe(true);
    });

    test('При передаче contentNode стандартный контент заменяется переданным компонентом', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentNode: (<div className="custom-content">Я контенте</div>),
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const defaultContent = wrapper.find(recommendationsCardCn('content'));
        const customContent = wrapper.find('.custom-content');

        expect(defaultContent.exists()).toBe(false);
        expect(customContent.exists()).toBe(true);
    });

    test('Рендерит картинку из аватарницы', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentImage: {
                storage: EStorageTypes.avatars,
                src: 'https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/orig',
                height: 280,
                width: 350,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const image = wrapper.find(RecommendationsCardImage);

        expect(image.exists(), 'Не отрендерили картинку').toBe(true);
    });

    test('Рендерит картинку из тумбнейлер', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentImage: {
                storage: EStorageTypes.im2tub,
                src: 'http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506',
                height: 280,
                width: 350,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const image = wrapper.find(RecommendationsCardImage);

        expect(image.exists(), 'Не отрендерили картинку').toBe(true);
    });

    test('Рендерит картинку из другого источника', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentImage: {
                storage: EStorageTypes.other,
                src: 'https://lisa.ru/images/cache/2020/3/18/resize_1200_630_true_crop_1920_1079_0_51_q90_887582_25d9c03ae8af50ca4cfbc9a05.jpeg',
                height: 630,
                width: 1200,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const image = wrapper.find(RecommendationsCardImage);

        expect(image.exists(), 'Не отрендерили картинку').toBe(true);
    });

    test('Маленькая карточка обернута в ссылку', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            actionText: 'test text',
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const button = wrapper.find(RecommendationsCardAction);
        expect(wrapper.is(Link)).toBe(true);
        expect(wrapper.getElement().props.url).toBe('https://yandex.ru/turbo');
        expect(button.prop('url')).toBe(undefined);
    });

    test('Большая карточка рендерит кнопку, передавая url', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            actionText: 'test text',
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const action = wrapper.find(RecommendationsCardAction);
        expect(wrapper.is(Link)).toBe(false);
        expect(action.prop('url')).toBe('https://yandex.ru/turbo');
    });

    test('Рендерит кнопку с текстом из actionText в маленькой карточке', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            actionText: 'test text',
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const button = wrapper.find(RecommendationsCardAction);

        expect(button.children().text()).toEqual('test text');
    });

    test('Рендерит кнопку с текстом из actionText в большой карточке', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            actionText: 'test text',
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const button = wrapper.find(RecommendationsCardAction);

        expect(button.children().text()).toEqual('test text');
    });

    test('Вызывает onActionClick при клике на кнопку в маленькой карточке', () => {
        const onClick = jest.fn();
        const props: IRecommendationsCardProps = {
            id: 'id',
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            onActionClick: onClick,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const button = wrapper.find(RecommendationsCardAction);
        button.simulate('click');

        expect(onClick).toBeCalledWith('id');
    });

    test('Вызывает onActionClick при клике на кнопку в большой карточке', () => {
        const onClick = jest.fn();
        const props: IRecommendationsCardProps = {
            id: 'id',
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            onActionClick: onClick,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const button = wrapper.find(RecommendationsCardAction);
        button.simulate('click');

        expect(onClick).toBeCalledWith('id');
    });

    test('Маленькая карточка рендерит строку даты в мета-данных', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippet,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const meta = wrapper.find(RecommendationsCardMeta);

        expect(meta.exists(), 'Не отрендерился элемент meta').toBe(true);

        const dateProp = meta.prop('date');

        expect(dateProp).toEqual({ timestamp: 1591370317330 });
    });

    test('Большая карточка рендерит строку даты в мета-данных', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const meta = wrapper.find(RecommendationsCardMeta);

        expect(meta.exists(), 'Не отрендерился элемент meta').toBe(true);

        const dateProp = meta.prop('date');

        expect(dateProp).toEqual({ timestamp: 1591370317330 });
    });

    test('Рендерит неинтерактивный скелетон, если передан параметр isLoading', () => {
        const props: IRecommendationsCardProps = {
            type: ERecommendationsCardTypes.snippet,
            isLoading: true,
            articleUrl: 'https://yandex.ru/turbo',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const skeleton = wrapper.find(RecommendationsCardSkeleton);
        const turboCard = wrapper.find(TurboCard);

        expect(skeleton.exists()).toBe(true);
        expect(turboCard.prop('interactive')).toBe(false);
        expect(wrapper.is(Link)).toBe(false);
    });

    test('Передаёт описание картинки', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentImage: {
                storage: EStorageTypes.avatars,
                src: 'https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/orig',
                height: 280,
                width: 350,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
            imageCaption: 'Фото: такое сякое',
        };

        const wrapper = shallow(<RecommendationsCard {...props} />);
        const image = wrapper.find(RecommendationsCardImage);

        expect(image.prop('caption')).toBe(props.imageCaption);
    });

    describe('Позиция картинки в маленькой карточке', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            contentImage: {
                storage: EStorageTypes.avatars,
                src: 'https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/orig',
                height: 280,
                width: 350,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            type: ERecommendationsCardTypes.snippetWithSourceBtn,
            articleUrl: 'https://yandex.ru/turbo',
            imageCaption: 'Фото: такое сякое',
        };

        test('Картинка рендерится вверху карточки по умолчанию', () => {
            const wrapper = shallow(<RecommendationsCard {...props} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.first().is(RecommendationsCardImage)).toBe(true);
        });

        test('Картинка рендерится вверху карточки при соответствующем imagePosition', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.top} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.first().is(RecommendationsCardImage)).toBe(true);
        });

        test('Картинка рендерится между заголовком и аннотацией карточки при соответствующем imagePosition', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.center} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.at(1).is(RecommendationsCardImage)).toBe(true);
        });

        test('Картинка рендерится после заголовка и аннотации при соответствующем imagePosition', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.bottom} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.at(2).is(RecommendationsCardImage)).toBe(true);
        });

        test('Проставляется модификатор со значением позиции картинки', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.bottom} />);

            const [, expectedClassName] = recommendationsCardCn({ 'img-pos': 'bottom' }).split(' ');

            expect(wrapper.hasClass(expectedClassName)).toBe(true);
        });
    });

    describe('Позиция картинки в большой карточке', () => {
        const props: IRecommendationsCardProps = {
            title: 'Я заголовок новости',
            annotation: 'Я аннотация...',
            meta: {
                date: {
                    timestamp: 1591370317330,
                },
            },
            contentImage: {
                storage: EStorageTypes.avatars,
                src: 'https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/orig',
                height: 280,
                width: 350,
                projectId: 'get-turbo',
                imageSizes: { '244x122': '/max_g480_c6_r16x9_pd10' },
            },
            type: ERecommendationsCardTypes.largeWithSourceBtn,
            actionText: 'test text',
            articleUrl: 'https://yandex.ru/turbo',
        };

        test('Карточка всегда идёт после меты, несмотря на переданую позицию 1', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.bottom} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.at(2).is(RecommendationsCardImage)).toBe(true);
        });

        test('Карточка всегда идёт после меты, несмотря на переданую позицию 2', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.top} />);

            const cardContent = wrapper.find(TurboCard).children();
            expect(cardContent.at(2).is(RecommendationsCardImage)).toBe(true);
        });

        test('Не проставляется модификатор со значением позиции картинки', () => {
            const wrapper = shallow(<RecommendationsCard {...props} imagePosition={EImagePosition.bottom} />);

            const [, expectedClassName] = recommendationsCardCn({ 'img-pos': 'bottom' }).split(' ');

            expect(wrapper.hasClass(expectedClassName)).toBe(false);
        });
    });
});
