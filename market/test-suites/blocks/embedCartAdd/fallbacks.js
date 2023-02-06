import {makeSuite, makeCase} from '@yandex-market/ginny';

import {postMessage, checkMessageCount, checkThatMessageWasDispatched, waitForLoad} from './utils';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Встраиваемое окно корзины. Обработка ошибки добавления в корзину.', {
    environment: 'kadavr',
    params: {
        offerId: 'ИД оффера, который добавляем в корзину',
    },
    story: {
        'Изменение тайтла': {
            'при ошибке добавления в корзину': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await this.preloader.waitForHidden(5000);
                    const afterLoadTitleText = await this.view.getTitleText();
                    await this.browser.expect(afterLoadTitleText).to.be.equal('Не добавилось', 'Отображается сообщение об ошибке');
                },
            }),
        },

        'Проверка отправки сообщений': {
            'при ошибке добавлении в корзину': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await this.view.close();

                    await checkThatMessageWasDispatched.call(this, {
                        event: 'closeMe',
                    }, 'Событие closeMe должно отправиться');

                    await checkMessageCount.call(this, 'ready', 1, 'Событие ready должно отправиться только один раз');
                },
            }),
        },
    },
});
