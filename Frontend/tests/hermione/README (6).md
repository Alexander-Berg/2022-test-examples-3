## Запуск тестов на dev

Для начала обойдёмся без архона, важно запустить тесты как таковые.

Чтобы протестить дев-сборку:
1.  Запускаем сервер
    ```bash
    npm run start
    ```

2.  В отдельной вкладке консоли запускаем, собственно, гермиону со своим логином
    ```bash
    DEV=belyaev-sv npm run hermione:gui
    ```
