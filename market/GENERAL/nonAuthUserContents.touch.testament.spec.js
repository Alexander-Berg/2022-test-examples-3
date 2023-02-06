import {screen} from '@testing-library/dom';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {setup, makeContext} from './helpers';

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

        setup(layers);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Неавторизованный пользователь.', () => {
        // @testpalm https://testpalm.yandex-team.ru/marketfront/testcases/4832
        beforeEach(async () => {
            await makeContext(layers, {isAuth: false});
        });

        it('заголовок должен отображаться', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(screen.getByRole('heading', {name: /войдите.*чтобы.*пригласить.*друзей/i})).toBeInTheDocument();
        });
        it('текстовый контент должен отображаться', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(
                screen.getByText(
                    /за приглашение подарим вам баллы Плюса,.*а другу — скидку на первый заказ в приложении/i
                )
            ).toBeInTheDocument();
        });
        it('кнопка "Войти" должна отображаться и вести на страницу авторизации', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            expect(screen.getByRole('link', {name: /войти/i})).toBeInTheDocument();
            expect(screen.getByRole('link', {name: /войти/i})).toHaveAttribute('href', expect.stringContaining('auth'));
        });
    });
});
