import {Command} from 'commander';

import {loadTestCases} from './loadTestCases';

const program = new Command();

export interface ITestpalmToolArgs {
    auth: string;
    filter: string;
    dir: string;
}

program
    .option(
        '-a, --auth [auth]',
        'OAuth токен. Его можно получить по ссылке https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6d967b191847496a8a7077e2e636142f',
    )
    .option('-d, --dir [dir]', 'Папка в которой нужно создать ямлы')
    .option(
        '-f, --filter [filter]',
        'Фильтр запроса тесткейсов. Пример: "Сервис=Отели&Страница=Бронирование"',
    )
    .action((args: ITestpalmToolArgs) => {
        if (!args.auth) {
            throw new Error(
                'Не указан параметр --auth c OAuth токеном. Его можно получить по ссылке https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6d967b191847496a8a7077e2e636142f',
            );
        }

        if (!args.dir) {
            throw new Error(
                'Не указана папка в которой нужно создавать ямлы. Параметр --dir. ',
            );
        }

        loadTestCases(args);
    });

program.parse(process.argv);
