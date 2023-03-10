const print = require('@yandex-market/yammy-lib/lib/print');

module.exports = [
    print.tip(
        `Для запуска тестов можно использовать команды:

- ${print.command('yammy test <pkgs...>')} - который запустит тесты во всех указанных пакетах (по-умолчанию вообще во всех где настроены тесты). Доводит тесты до конца даже если есть упавшие, пишет саммари о том в каких пакетах тесты упали
- ${print.command('yammy test-only <pkg> <testNames...>')} - который выполнит все тесты из указанного списка ${print.arg('testNames')} вида ${print.arg('test:<testName>')} для пакета ${print.arg('pkg')}. Падение одного из тестов никак не влияет на выполнение остальных. Перед запуском тестов выполняется сборка пакета, если не указана переменная окружения ${print.env('CI')} или ${print.env('NO_AUTO_BUILD')}
- ${print.command('yammy run <pkg> <script>')} - универсальная команда для запуска любых скриптов. Позволяет передавать допольнительные аргументы после ${print.arg('--')}
- ${print.command('yammy each <script> <pkgs...>')} - запускает указанный скрипт во всех указанных пакетах (по-умолчанию во всех где он есть). Исполнение будет остановлено после первого же упавшего скрипта.`
    ),
];
