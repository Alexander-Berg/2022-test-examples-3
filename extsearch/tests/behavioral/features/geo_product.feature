language: ru
Функция: реклама

    Сценарий: реклама организации
        Пусть запрос пользователя равен "yandex"
        И запрос задаётся откуда-то из региона "Москва"
        И ограничение на рекламу равно 5
        И advert_page_id равен testsuite
        Когда пользователь запускает поиск
        Тогда организация "Yandex" в ответе содержит рекламу

    Сценарий: организация без рекламы
        Пусть запрос пользователя равен "yandex"
        И запрос задаётся откуда-то из региона "Москва"
        И ограничение на рекламу равно 5
        И advert_page_id равен empty
        Когда пользователь запускает поиск
        Тогда организация "Yandex" в ответе не содержит рекламу