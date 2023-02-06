# language: ru
Функция: поиск заправок

    Предыстория:
        Пусть выбрана локаль ru_RU

    Сценарий: поиск заправок

        Структура сценария: поиск заправок 
            Пусть запрос пользователя равен "(provider:lukoil-azs | provider:benzuber)"
            И запрос задаётся откуда-то из региона "<region>"
            Когда пользователь запускает поиск
            Тогда первая организация относится к рубрике "АЗС" 

            Примеры: заправки в регионах
                | region          |
                | Москва          |
                | Санкт-Петербург |
                | Башкортостан    |


    Сценарий: сниппеты заправок

        Структура сценария:
            Пусть пользователь поискал "(provider:gas_stations) (category_id:184105274 | category_id:184105272)" в регионе "Москва"
            И запрошены сниппеты
                experimental/1.x/ref_gas_stations
                experimental/1.x
            Когда пользователь запускает поиск
            Тогда хотя бы один документ из top10 имеет непустой сниппет "ExperimentalMetaData"
            И у всех документов есть gta с ключом "ref_gas_stations"