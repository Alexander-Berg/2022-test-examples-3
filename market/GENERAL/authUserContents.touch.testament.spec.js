import {screen, fireEvent} from '@testing-library/dom';
import {act} from 'react-dom/test-utils';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {prepareState, setup, makeContext} from './helpers';

/** @type {Mirror} */
let mirror;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {MandrelLayer} */
let mandrelLayer;

let layers;

const TERMS_LINK_HREF = 'https://yandex.ru/support/market/promo/recommendation-program.html';

describe('Widget: ReferralLandingInfo: Реферальная программа', () => {
    const WIDGET_PATH = '@self/root/src/widgets/content/ReferralLandingInfo';
    const WIDGET_OPTIONS = {};

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirrorTouch({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });

        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        layers = {jestLayer, kadavrLayer, mandrelLayer};

        await setup(layers);
        jest.useFakeTimers();
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Авторизированный пользователь', () => {
        describe('Недостигший максимального кол-ва баллов', () => {
            // @testpalm https://testpalm.yandex-team.ru/marketfront/testcases/4811
            beforeEach(async () => {
                await makeContext(layers, {isAuth: true});
                await prepareState(kadavrLayer, {
                    isReferralProgramActive: true,
                    isGotFullReward: false,
                });
            });

            it('заголовок корректно отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByRole('heading', {name: /приведите.*друга на маркет/i})).toBeInTheDocument();
            });

            it('информационное сообщение корректно отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByTestId('text')).toHaveTextContent(
                    'PROMOCODE — промокод, который даст ему скидку на первый заказ в приложении от 5 000 ₽. ' +
                        'А вы после этого получите 300 баллов Плюса.'
                );
            });

            it('кнопка отображается и меняет текст при нажатии', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                const button = screen.getByRole('button', {name: /скопировать промокод/i});
                document.execCommand = jest.fn(() => true);

                // При нажатии текст должен измениться уведомив пользователя о том что информация скопирована
                fireEvent.click(button);

                expect(document.execCommand).toHaveBeenCalledTimes(1);
                expect(document.execCommand).toHaveBeenCalledWith('copy');
                expect(await screen.findByRole('button', {name: /промокод скопирован/i})).toBeInTheDocument();

                // По истечению задержки текст кнопки должен вернуться обратно
                act(() => jest.advanceTimersByTime(3000));
                expect(await screen.findByRole('button', {name: /скопировать промокод/i})).toBeInTheDocument();

                document.execCommand.mockClear();
            });

            it('ссылка отображается и ведёт на справочную страницу рефералки', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

                expect(screen.getByRole('link', {name: /читать условия акции/i})).toHaveAttribute(
                    'href',
                    TERMS_LINK_HREF
                );
            });

            it('блок статистики отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByText(/600/i)).toBeInTheDocument();
                expect(screen.getByText(/из 3 000 баллов.*получено/i)).toBeInTheDocument();
            });

            it('информация о баллах в ожидании отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByText(/900/i)).toBeInTheDocument();
                expect(screen.getByText(/баллов.*в ожидании/i)).toBeInTheDocument();
            });

            it('информация о количестве друзей сделавших заказ отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByText(/2/i)).toBeInTheDocument();
                expect(screen.getByText(/друзей.*сделали заказ/i)).toBeInTheDocument();
            });
        });
        describe('Достигший максимального кол-ва баллов', () => {
            // @testpalm https://testpalm.yandex-team.ru/marketfront/testcases/4813
            beforeEach(async () => {
                await makeContext(layers, {isAuth: true});
                await prepareState(kadavrLayer, {
                    isReferralProgramActive: true,
                    isGotFullReward: true,
                });
            });
            it('заголовок корректно отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                screen.getByRole('heading', {
                    name: /приведите.*друга на маркет/i,
                });
            });

            it('информационное сообщение корректно отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(screen.getByTestId('text')).toHaveTextContent(
                    'PROMOCODE — промокод, который даст ему скидку на первый заказ в приложении от 5 000 ₽.'
                );
            });

            it('кнопка отображается и меняет текст при нажатии', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                const button = screen.getByRole('button', {name: /скопировать промокод/i});
                document.execCommand = jest.fn(() => true);

                fireEvent.click(button);
                expect(document.execCommand).toHaveBeenCalledTimes(1);
                expect(document.execCommand).toHaveBeenCalledWith('copy');

                // При нажатии текст должен измениться уведомив пользователя о том что информация скопирована
                expect(await screen.findByRole('button', {name: /промокод скопирован/i})).toBeInTheDocument();

                // По истечению задержки текст кнопки должен вернуться обратно
                act(() => jest.advanceTimersByTime(3000));
                expect(await screen.findByRole('button', {name: /скопировать промокод/i})).toBeInTheDocument();

                document.execCommand.mockClear();
            });

            it('ссылка отображается и ведёт на справочную страницу рефералки', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

                expect(screen.getByRole('link', {name: /читать условия акции/i})).toHaveAttribute(
                    'href',
                    TERMS_LINK_HREF
                );
            });

            it('блок статистики содержит корректный контент', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                expect(
                    screen.getByRole('heading', {name: /зарабатывайте до 50.*000.*₽ в.*месяц/i})
                ).toBeInTheDocument();

                expect(
                    screen.getByText(
                        /вы получили 3 000 баллов за приглашение друзей — у вас отлично получается\. а теперь можете стать партнёром маркета и зарабатывать до 50 000 ₽ в месяц\./i
                    )
                ).toBeInTheDocument();
            });

            it('блок статистики содержит ссылку ведущую на страницу партнерства', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                const link = screen.getByRole('link', {name: /узнать про партнёрство/i});
                expect(link).toHaveAttribute('href', 'https://aff.market.yandex.ru/influencers');
            });
        });
    });
});
