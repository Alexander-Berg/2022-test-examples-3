import {screen, within} from '@testing-library/dom';

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
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Акция недоступна', () => {
        // @testpalm https://testpalm.yandex-team.ru/marketfront/testcases/5001
        beforeEach(async () => {
            await makeContext(layers, {isAuth: true});
            await prepareState(kadavrLayer, {
                isReferralProgramActive: false,
            });
        });
        it('заголовок корректно отображается', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(screen.getByRole('heading', {name: /промокод уже не работает/i})).toBeInTheDocument();
        });
        it('информационное сообщение корректно отображается', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(screen.getByText('Акция закончилась, увы')).toBeInTheDocument();
        });
        it('кнопка отображается и ведёт на главную страницу', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(screen.getByRole('link', {name: /эх/i})).toHaveAttribute('href', '/');
        });
    });

    describe('Попап доступности партнерской программы', () => {
        // @testpalm https://testpalm.yandex-team.ru/marketfront/testcases/4859

        it('отображается при первой загрузке страницы + после достижения макс. кол-ва баллов в акции', async () => {
            await makeContext(layers, {isAuth: true}, {availability_of_partner_program_popup_was_showed: undefined});
            await prepareState(kadavrLayer, {
                isReferralProgramActive: true,
                isGotFullReward: true,
            });
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

            /**
             * Временно используем queryByTestId, т.к у попапа нет a11y роли. Нужно проставить ему эту роль
             * и выбирать его с `findByRole('dialog', ...)`.
             */
            const popup = await screen.findByTestId('availability-of-partner-program');
            expect(popup).toBeInTheDocument();

            expect(
                screen.getByRole('heading', {name: /вы получили 3.*000.*баллов.*-.*максимум для этой акции/i})
            ).toBeInTheDocument();
            expect(
                screen.getByText(/но можете стать партнером маркета.*и зарабатывать до 50 000 ₽ в месяц/i)
            ).toBeInTheDocument();

            const link = within(popup).getByRole('link', {name: /узнать про партнёрство/i});
            expect(link).toBeInTheDocument();
            expect(link).toHaveAttribute('href', 'https://aff.market.yandex.ru/influencers');
        });

        it('не отображается при повторной загрузке страницы + после достижения макс. кол-ва баллов в акции', async () => {
            await makeContext(layers, {isAuth: true}, {availability_of_partner_program_popup_was_showed: 'true'});
            await prepareState(kadavrLayer, {
                isReferralProgramActive: true,
                isGotFullReward: true,
            });
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            /**
             * Временно используем queryByTestId, т.к у попапа нет a11y роли. Нужно проставить ему эту роль
             * и выбирать его с `queryByRole('dialog', ...)`.
             */
            expect(screen.queryByTestId('availability-of-partner-program')).not.toBeInTheDocument();
        });
    });
});
