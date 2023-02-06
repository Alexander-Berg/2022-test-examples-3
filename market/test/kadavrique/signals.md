# Сигналы
Ряд действий внутри приложения можно совершить, отправив сигнал.

Как послать сигнал:
```bash
kill -SIGUSR1 pid
```
*В зависимости от ОС команда отправки сигнала может отличаться.*

## SIGHUP

Обрабатывается приложением, как команда на пересоздание файлов логов. Сейчас этот сигнал отправляет logrotate после окончания ротации.

## SIGUSR1

Служит для создания хипдампа (в директории логов, которая определяется энвой `LOGS_DIR`). Хипдамп создается при помощи библиотеки [heapdump](https://www.npmjs.com/package/heapdump).

## SIGUSR2

Служит для создания CPU профиля (в течение 60 секунд с момента получения сигнала, в директории логов - берется из энвы LOGS_DIR).