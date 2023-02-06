/* eslint-disable @typescript-eslint/explicit-function-return-type */
import _ from 'lodash';
import { PluginConfig, PluginConfigPartial } from './types';

const defaultOptions: PluginConfig = {
    enabled: false,
    rewrite: true,
    allLinks: false,
    filePaths: [],
    hermioneToTestpalm: true,
    ignoreObjects: [],
    textReplacers: {},
    replacers: {
        browser: {
            addValue: ({ arguments: args }) => [
                { do: `установить курсор в текстовое поле элемента [${args[0]}]` },
                { do: `добавить текст в поле "${args[1]}"` },
            ],
            assertView: ({ arguments: args }) => ({ screenshot: `внешний вид [${args[0]}]` }),
            back: () => ({ do: 'нажать на кнопку "Назад" браузера' }),
            click: ({ arguments: args }) => ({ do: `нажать на элемент [${args[0]}]` }),
            deleteCookie: () => ({ do: 'в настройках браузера очистить куки и кэш' }),
            keys: ({ arguments: args }) => [
                { do: 'убедиться, что к устройству подключена физическая клавиатура' },
                { do: `на физической клавиатуре нажать на кнопку "${args[0]}"` },
            ],
            moveToObject: ({ arguments: args }) => ({
                do: `навести курсор на элемент [${args[0]}]`,
            }),
            orientation: ({ arguments: args }) => ({
                do: `перевернуть девайс в ${args[0]} ориентацию`,
            }),
            scroll: ({ arguments: args }) => {
                const endString = _.isString(args[0]) ? ` до ${args[0]}` : '';

                return { do: `проскроллить страницу${endString}` };
            },
            setValue: ({ arguments: args }) => [
                { do: `установить курсор в текстовое поле элемента [${args[0]}]` },
                { do: `заменить текст в поле на "${args[1]}"` },
            ],
            swipeDown: ({ arguments: args }) => ({
                do: `свайпнуть элемент [${args[0]}] сверху вниз`,
            }),
            swipeLeft: ({ arguments: args }) => ({
                do: `свайпнуть элемент [${args[0]}] справа налево`,
            }),
            swipeRight: ({ arguments: args }) => ({
                do: `свайпнуть элемент [${args[0]}] слева направо`,
            }),
            swipeUp: ({ arguments: args }) => ({
                do: `свайпнуть элемент [${args[0]}] снизу вверх`,
            }),
            url: ({ arguments: args }) => ({ do: `открыть страницу по адресу ${args[0]}` }),
            touch: ({ arguments: args }) => ({ do: `нажать на элемент [${args[0]}]` }),
            waitForExist: ({ arguments: args }) => {
                const assertString = args[2] ? 'отсутствует' : 'отображается';

                return { assert: `элемент [${args[0]}] ${assertString} на странице` };
            },
            waitForVisible: ({ arguments: args }) => {
                const assertString = args[2] ? 'скрылся со страницы' : 'появился на странице';

                return { assert: `элемент [${args[0]}] ${assertString}` };
            },
            waitUntil: _.noop,
            execute: _.noop,
            hideDeviceKeyboard: _.noop,
            pause: _.noop,
        },
    },
};

export function parseConfig(options: PluginConfigPartial): PluginConfig {
    return _.cloneDeep(_.defaultsDeep(options, defaultOptions));
}
