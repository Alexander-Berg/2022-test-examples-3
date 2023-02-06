import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {screen, within, waitFor, fireEvent} from '@testing-library/dom';
import {buildUrl} from '@self/root/src/utils/router';
import {reviewStates} from '@self/root/src/entities/review/constants';
import * as voteRequestAction from '@self/root/src/actions/reviews/reviewVote';

import {
    productCfg,
    shopCfg,
    USER_UID,
    PUBLIC_USER_ID,
    DEFAULT_AVERAGE_GRADE,
    DEFAULT_VOTES_COUNT,
    PRO,
    CONTRA,
    COMMENT,
    publicDisplayName,
} from '@self/root/src/spec/testament/review/mocks';

import {
    prepareKadavrState,
    EXPECTED_ZERO_STATE_TEXT,
    EXPECTED_ANONYMOUS_USER_INFO,
    TEN_MORE_REVIEWS_COUNT,
    TEN_LESS_REVIEWS_COUNT,
    DEFAULT_VISIBLE_REVIEWS_COUNT,
    EXPECTED_GRADE_DESCRIPTION,
    EXPECTED_STATUS_TEXT,
} from './helpers';

const WIDGET_PATH = '../';
const DEFAULT_PARAMS = {userUid: USER_UID, isPublic: false};

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext(cookies = {}, exps = {}, user = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {
        kadavr_session_id: await kadavrLayer.getSessionId(),
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps,
            },
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });

    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('UserReviews', () => {
    describe('Cниппеты отзывов отсутствуют.', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {reviewsCount: 0});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('По умолчанию содержит верный текст', async () => {
            expect(screen.queryByText(EXPECTED_ZERO_STATE_TEXT.caption)).toBeInTheDocument();
            expect(screen.queryByText(EXPECTED_ZERO_STATE_TEXT.description)).toBeInTheDocument();
        });
    });

    describe('Анонимный отзыв.', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {anonymous: 1});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('Отображается заглушка аватарки', async () => {
            const emptyAvatar = screen.getByTestId('empty-avatar');
            const imageSrc = within(emptyAvatar).getByRole('img').getAttribute('src');

            expect(emptyAvatar).toBeInTheDocument();
            expect(imageSrc).toBe(
                buildUrl('external:user-avatar', {
                    avatar: '0/0-0',
                    type: 'islands-middle',
                })
            );
        });

        it('Отображается заглушка для имени пользователя', async () => {
            expect(screen.queryByText(EXPECTED_ANONYMOUS_USER_INFO)).toBeInTheDocument();
        });
    });

    // Тикет на исправление: MARKETFRONT-99775
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Единственный отзыв, который можно удалить.', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('При удалении единственного отзыва отображается zero-стейт страницы', async () => {
            expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(1);

            const menu = screen.getByRole('menu');
            fireEvent.click(within(menu).getByRole('button', {name: /удалить/i}));

            expect(await screen.findByText(/действительно хотите удалить отзыв\?/i)).toBeInTheDocument();

            const dialog = screen.getByTestId('review-controls');
            fireEvent.click(within(dialog).getByRole('button', {name: /да, удалить/i}));

            expect(await screen.findByText(EXPECTED_ZERO_STATE_TEXT.caption)).toBeInTheDocument();
            expect(await screen.findByText(EXPECTED_ZERO_STATE_TEXT.description)).toBeInTheDocument();
        });

        it('Кнопка "Отменить". Отмена удаления происходит корректно', async () => {
            expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(1);

            const menu = screen.getByRole('menu');
            fireEvent.click(within(menu).getByRole('button', {name: /удалить/i}));

            expect(await screen.findByText(/действительно хотите удалить отзыв\?/i)).toBeInTheDocument();

            const dialog = screen.getByTestId('review-controls');
            fireEvent.click(within(dialog).getByRole('button', {name: /отменить/i}));

            expect(await screen.findAllByTestId('personal-cabinet-card')).toHaveLength(1);
        });

        it('Нажатие на паранжу. Отмена удаления происходит корректно', async () => {
            expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(1);

            const menu = screen.getByRole('menu');
            fireEvent.click(within(menu).getByRole('button', {name: /удалить/i}));

            expect(await screen.findByText(/действительно хотите удалить отзыв\?/i)).toBeInTheDocument();

            // TODO: Реализовать клик вне модального окна

            expect(await screen.findAllByTestId('personal-cabinet-card')).toHaveLength(1);
        });
    });

    describe('Если отзывов больше 10.', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {reviewsCount: TEN_MORE_REVIEWS_COUNT});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('Кнопка "Показать еще" отображается', async () => {
            expect(screen.queryByRole('button', {name: /показать ещё/i})).toBeInTheDocument();
        });

        it('Отображается верное количество отзывов', async () => {
            expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(DEFAULT_VISIBLE_REVIEWS_COUNT);
        });

        it('Клик по кнопке "Показать еще" скрывает саму кнопку и подгружает отзывы', async () => {
            const button = screen.queryByRole('button', {name: /показать ещё/i});
            expect(button).toBeInTheDocument();

            fireEvent.click(button);

            await waitFor(() => {
                expect(button).not.toBeInTheDocument();
                expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(TEN_MORE_REVIEWS_COUNT);
            });
        });
    });

    describe('Если отзывов меньше 10.', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {reviewsCount: TEN_LESS_REVIEWS_COUNT});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('Кнопка "Показать еще" не отображается', async () => {
            expect(screen.queryByRole('button', {name: /показать ещё/i})).not.toBeInTheDocument();
        });

        it('Отображается верное количество отзывов', async () => {
            expect(screen.getAllByTestId('personal-cabinet-card')).toHaveLength(TEN_LESS_REVIEWS_COUNT);
        });
    });

    describe.each([
        [
            'Магазинный сниппет отзыва.',
            {productId: null},
            {
                gradeDescription: EXPECTED_GRADE_DESCRIPTION.shop,
                link: buildUrl('market:shop', {
                    shopId: String(shopCfg.shopId),
                    slug: shopCfg.slug,
                }),
                addReviewLink: buildUrl('market:shop-reviews-add', {
                    shopId: shopCfg.shopId,
                    slug: shopCfg.slug,
                }),
            },
            {type: 'shop'},
        ],
        [
            'Товарный сниппет отзыва.',
            {},
            {
                gradeDescription: EXPECTED_GRADE_DESCRIPTION.product,
                link: buildUrl('market:product', {
                    productId: String(productCfg.productId),
                    slug: productCfg.slug,
                }),
                addReviewLink: buildUrl('market:product-reviews-add', {
                    productId: productCfg.productId,
                    slug: productCfg.slug,
                }),
            },
            {type: 'product'},
        ],
    ])('%s', (_, stateParams, expectedText, props) => {
        const {type} = props;
        const {gradeDescription, link, addReviewLink} = expectedText;

        describe('Блок c информацией об авторе.', () => {
            beforeAll(async () => {
                await prepareKadavrState(kadavrLayer, stateParams);
                await makeContext({user: {UID: USER_UID}});
                await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
            });

            it('По умолчанию отображается', async () => {
                expect(screen.getByTestId('content-user-info')).toBeInTheDocument();
            });

            it('Содержит аватарку пользователя', async () => {
                const userInfo = screen.queryByTestId('content-user-info');
                expect(within(userInfo).getByRole('img')).toBeInTheDocument();
            });

            it('Содержит дату оставления отзыва', async () => {
                const userInfo = screen.queryByTestId('content-user-info');
                expect(within(userInfo).getByTestId('creation-date')).toBeInTheDocument();
            });

            it('Содержит правильное имя пользователя', async () => {
                const userInfo = screen.queryByTestId('content-user-info');
                expect(within(userInfo).queryByText(publicDisplayName)).toBeInTheDocument();
            });
        });

        describe('Блок оценки в отзыве.', () => {
            beforeAll(async () => {
                await prepareKadavrState(kadavrLayer, stateParams);
                await makeContext({user: {UID: USER_UID}});
                await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
            });

            it('По умолчанию отображается', async () => {
                const userInfo = screen.getByTestId('content-user-info');
                expect(within(userInfo).getByRole('img')).toBeInTheDocument();
            });

            it('Количество звезд соответствует оценке', async () => {
                const ratingStars = screen.getByTestId('rating-stars');
                const rating = Number(ratingStars.getAttribute('data-rate'));
                expect(rating).toEqual(DEFAULT_AVERAGE_GRADE);
            });

            it('Текстовое описание соответствует оценке', async () => {
                expect(screen.queryByText(gradeDescription)).toBeInTheDocument();
            });
        });

        describe('Блок текста отзыва. Преимущества, недостатки, комментарий.', () => {
            beforeAll(async () => {
                await prepareKadavrState(kadavrLayer, stateParams);
                await makeContext({user: {UID: USER_UID}});
                await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
            });

            it('Содержит верный текст преимущества', async () => {
                expect(screen.queryByText(/Достоинства:/i)).toBeInTheDocument();
                const definition = screen.getByTestId('review-pro');
                expect(within(definition).queryByText(PRO)).toBeInTheDocument();
            });

            it('Содержит верный текст недостатков', async () => {
                expect(screen.queryByText(/Недостатки:/i)).toBeInTheDocument();
                const definition = screen.getByTestId('review-contra');
                expect(within(definition).queryByText(CONTRA)).toBeInTheDocument();
            });

            it('Содержит верный текст комментария', async () => {
                expect(screen.queryByText(/Комментарий:/i)).toBeInTheDocument();
                const definition = screen.getByTestId('review-comment');
                expect(within(definition).queryByText(COMMENT)).toBeInTheDocument();
            });
        });

        describe('Шапка сниппета отзыва.', () => {
            beforeAll(async () => {
                await prepareKadavrState(kadavrLayer, stateParams);
                await makeContext({user: {UID: PUBLIC_USER_ID}});
                await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
            });

            it('Содержит верную ссылку на товар или магазин', async () => {
                const headline = screen.getByTestId(`${type}-headline`);
                const href = headline.getAttribute('href');
                expect(href).toBe(link);
            });

            describe('Кнопка управления отзывом (троеточие).', () => {
                it('По умолчанию отображается', async () => {
                    expect(screen.getByTestId('dropdown-menu-button')).toBeInTheDocument();
                });

                it('Клик по кнопке открывает попап управления отзывом', async () => {
                    expect(screen.getByTestId('dropdown-menu')).not.toHaveClass('isOpen');

                    const button = screen.getByTestId('dropdown-menu-button');
                    fireEvent.click(button);

                    expect(await screen.findByTestId('dropdown-menu')).toHaveClass('isOpen');
                });
            });
        });

        describe('Статусы.', () => {
            describe.each([
                ['Автоматически отклоненный отзыв.', {moderationState: reviewStates.AUTOMATICALLY_REJECTED, spam: true},
                    EXPECTED_STATUS_TEXT.AUTHOMATICALLY_REJECTED],
                ['Отзыв, отклоненный модератором.', {spam: true},
                    EXPECTED_STATUS_TEXT.REJECTED_BY_MODERATOR],
                ['Модерирующийся или отложенный отзыв.', {moderationState: reviewStates.UNMODERATED},
                    EXPECTED_STATUS_TEXT.UNMODERATED],
            ])('%s', (_, statusStateParams, expectedText) => {
                const {header, reason, description} = expectedText;

                beforeAll(async () => {
                    await prepareKadavrState(kadavrLayer, {...stateParams, ...statusStateParams});
                    await makeContext({user: {UID: USER_UID}});
                    await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
                });

                it('Плашка статуса отображается и содержит верный текст', async () => {
                    const headline = screen.getByTestId('status-headline');
                    expect(within(headline).queryByText(header)).toBeInTheDocument();
                });

                it('Футер отображается', async () => {
                    expect(screen.getByTestId('alter-review-footer')).toBeInTheDocument();
                });

                it('Футер содержит кнопку, которая ведет на страницу редактирования отзыва', async () => {
                    const footer = screen.getByTestId('alter-review-footer');
                    const href = within(footer).getByRole('link', {name: /редактировать отзыв/i}).getAttribute('href');
                    expect(href).toBe(addReviewLink);
                });

                it('Футер содержит текст причины отклонения или текст рекомендаций к оставлению отзыва', async () => {
                    const footer = screen.getByTestId('alter-review-footer');
                    expect(within(footer).getByRole('heading', {name: reason})).toBeInTheDocument();
                    expect(within(footer).queryByText(description)).toBeInTheDocument();
                });
            });
        });
    });

    describe('Товарный сниппет отзыва. Футер c лайками и комментариями', () => {
        beforeAll(async () => {
            await prepareKadavrState(kadavrLayer, {});
            await makeContext({user: {UID: USER_UID}});
            await apiaryLayer.mountWidget(WIDGET_PATH, DEFAULT_PARAMS);
        });

        it('По умолчанию отображается', async () => {
            expect(screen.getByTestId('review-footer')).toBeInTheDocument();
        });

        it('Кнопка "Комментировать" по умолчанию отображается', async () => {
            expect(screen.getByRole('button', {name: /комментировать/i})).toBeInTheDocument();
        });

        it('Лайки и дизлайки по умолчанию отображаются', async () => {
            expect(screen.getByTestId('review-votes')).toBeInTheDocument();
        });

        // Тикет на исправление: MARKETFRONT-99775
        // eslint-disable-next-line jest/no-disabled-tests
        it.skip('Кнопка лайка. При двойном клике количество лайков сначала увеличивается, а потом возвращается', async () => {
            const voteRequestSuccessSpy = jest.spyOn(voteRequestAction, 'voteRequestSuccess');

            const like = screen.getByTestId('review-like');
            const button = within(like).getByRole('button');

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT));

            fireEvent.click(button);

            await waitFor(() => {
                expect(voteRequestSuccessSpy).toHaveBeenCalled();
            });

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT + 1));

            fireEvent.click(button);

            await waitFor(() => {
                expect(voteRequestSuccessSpy).toHaveBeenCalled();
            });

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT));
        });

        // Тикет на исправление: MARKETFRONT-99775
        // eslint-disable-next-line jest/no-disabled-tests
        it.skip('Кнопка дизлайка. При двойном клике количество дизлайков сначала увеличивается, а потом возвращается', async () => {
            const voteRequestSuccessSpy = jest.spyOn(voteRequestAction, 'voteRequestSuccess');

            const dislike = screen.getByTestId('review-dislike');
            const button = within(dislike).getByRole('button');

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT));

            fireEvent.click(button);

            await waitFor(() => {
                expect(voteRequestSuccessSpy).toHaveBeenCalled();
            });

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT + 1));

            fireEvent.click(button);

            await waitFor(() => {
                expect(voteRequestSuccessSpy).toHaveBeenCalled();
            });

            expect(button).toHaveTextContent(String(DEFAULT_VOTES_COUNT));
        });
    });
});
