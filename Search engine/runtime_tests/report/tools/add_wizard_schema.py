#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os, errno
import re
import json
import argparse
import hashlib
import traceback
import urllib
import urlparse
import requests, socket

from copy import deepcopy
from subprocess import call
from cPickle import dumps as pkl_dumps, load as pkl_load
from collections import OrderedDict

import sys
reload(sys) # XXX weird hack for the following string
sys.setdefaultencoding('utf-8')
#os.putenv('PYTHONIOENCODING', 'UTF-8')

PWD = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(PWD, '..', '..'))

from report.tools.genson import Schema

TESTPALM_SPLITTER = '21ffb831-6583-4234-9e3a-0362eea4ee00'

ExcludedCases = [
    'Завирусованны', # SERP-40425
    'Фидбек на результаты поиска - ', # SERP-42224
    'Вертикаль - Поиск по людям - ', # SERP-40420
    'Вертикаль - Поиск по блогам - ', # SERP-40418
    ('Расширенны', ' поиск - '), # SERP-42262
    'Сниппеты - Черная дата', # SERP-42262
    'Разместить объявление - ', # SERP-40397
    ('Дистрибуционны', ' футер -'), # SERP-42312
    'Колдунщик дистрибуции - ', # SERP-42312
    'Директ', 'Баннер - ', # SERP-42961
    'Спец.размещение - Нижни', # direct_premium + direct_halfpremium SERP-42961
    'Спец.размещение - Показ', # SERP-42961
    ('Полоски - ', 'визуальные закладки'), 'Полоски - Нижняя', 'Полоски - Полоска', 'Полоски - Промо', # SERP-42978
    ('Цвета - ', 'невалидное'), # SERP-43023
    ('Адресны', 'рефреша страницы'), # SERP-43352
    ('Оплата ЖКХ', 'Проверка логотипа ЯД'), # SERP-43359

    # SERP-43395
    ('Маркет - Контекстно-зависимые', 'Выбор модели вендора в категории'),
    'Маркет - Вендор для визуальных категори',
    ('Маркет - Контекстно-зависимые. Категории', 'без карточки с фильтрами'),
    'Маркет - Родительская категория',

    # покрыто отдельно
    'Лого - ',
    'Табы сервисов - ', # functional/web/test_navigation_context.py
    'Подвал - Подвал - Выбор языка интерфе', # test_lang_switcher
    'Переход на мобильную версию', # uatraits in common, there is a lot logic in Verstka
    'Сниппеты - Базовая функциональность', # functional/web/test_templates.py
    ('Сниппеты - Длинны', ' зелены', ' урл'), # functional/web/test_templates.py
    ('Сниппеты - На', 'дено по ссылке'), # functional/web/test_templates.py
    'Фидбек', # functional/web/test_logs.py / abuse_link
    ('~COMTR', 'Офисны', ' формат документа'), # functional/web/test_templates.py test_search_doc_with_mime

    # чисто версточный кейс
    ('Погода по общему запросу', ' - Просто', ' редиза Эксперимен'),
    'Подвал - Подвал.json',
    'Список с буллитами', # дубликат Список обычный
    'Поиск в других системах',
    'Поисковая стрелка - ',
    'Саджест - ',
    'Сообщение об отсутствии подключения к интернету - ',
    'отсутствие интернета',
    'Авторизация - ',
    ('Директ - ', 'Размер шрифта'),
    'Экранная клавиатура - ',
    ('Сниппеты - Нижни', ' отступ Эксперимент'),
    ('Адресны', 'Сети-рубрики справа', 'с флагом размера'),
    ('Адресны', 'c кнопко', ' Веб-са'),
]

GenericSnippets = [
    'Форумы - ',
    'Список - Список обычны',
    'Сниппеты - ',
]

GenericSnippetsDict = dict([(k, True) for k in GenericSnippets])
ExcludedCasesDict = dict([(k, True) for k in ExcludedCases])

DocsKind = {
    'Форумы - ': 'docs',
    ('~COMTR', 'Игры - '): 'docs',

    'в объектном ответе': 'docs_right',
    'Сети-рубрики справа': 'docs_right',
    ('Объектны', ' ответ -'): 'docs_right',
    (' в право', ' колонке'): 'docs_right',
    'Оскара - ': 'docs_right',

    ('Музыкальны', ' пле', 'ер - Текстовы'): 'docs',
    ('Фактовые - ', 'Фактовы', ' ответ -'): 'docs',
    'Видео - Видео - Текстовы': 'docs',
    'Фактовые - Фактовые - Бензин': 'docs',
    'Фактовые - Фактовые - Курс акци': 'docs',
    'Фактовые - Фактовые - Психологическая помощь': 'docs',
    ('Адресны', ' - Без карты - одна организаци'): 'docs',

    'Фактовые - ': 'wizplaces',
    'Экстренные': 'wizplaces',
    'Калькулятор - Калькулятор': 'wizplaces',
    'Конвертер - ': 'wizplaces',
    'Геометрические фигуры - ': 'wizplaces',
    'Связанные запросы - ': 'wizplaces',
    'Псевдо - Псевдо - Конкретны': 'wizplaces',
    'Псевдо - Псевдо - Много': 'wizplaces',
    'Стихолюб - ': 'wizplaces',
    'Маркет - Предложения в право': 'wizplaces',
    'Игры - ': 'wizplaces',

    'Опечатк': 'wizplaces', #'searchdata',
    ('Опечатк', 'Исправлена опечатка'): 'searchdata',
    ('Опечатк', 'раскладки'): 'searchdata',
    ('Опечатк', 'кавычк'): 'searchdata',
    ('Опечатк', 'Ничего'): 'searchdata',
    ('Опечатк', 'Пусто'): 'searchdata',
    ('Опечатк', 'интаксич'): 'searchdata',

    ('Пе', 'джер - '): 'navi',

    'Полоски - Гео': 'banner.stripe_universal',
}

AllowMultiple = [
    '/snippet/lists/',
    '/snippet/turkey_exp/dreams/',
    '/snippet/ratings_reviews/',
    '/snippet/turkish_price/',
    '/snippet/snip_rating',
    '/snippet/generic/',
    '/snippet/generic/~generic',
    '/snippet/generic/~list_snip',
    '/snippet/generic/~forums',
    '/snippet/generic/~forum_topic',
    '/snippet/generic/~forum_forums',
    '/snippet/mime_view/',
    '/snippet/market/',
    '/snippet/mediawiki/',
    '/snippet/special_dates/',
    '/snippet/social_snippet/',
    '/snippet/social_annotation/',
    '/snippet/extended/',
    '/snippet/sitelinks/',
    '/snippet/yabs_proxy/',
    '/snippet/tripadvisor-rating',
    '/snippet/booking-rating',
    '/snippet/entity_search/', '/parallel/result/snippet/entity_search/',
    '/snippet/infected/',
    '/snippet/recipe/',
    '/snippet/foto_recipe',
    '/snippet/foto_product',
    '/snippet/bno/',
    '/snippet/adress_button/',
]

# search for specific wizard by case name
PrimaryWizard = {
    # fake wizards
    ('Пе', 'джер - '): 'navi',
    'Полоски - ': 'banner',

    # Опечатки
    ('Опечатк', 'интаксич'): 'reask',
    ('Опечатк', 'Пусто'): 'reask',
    ('Опечатк', 'Ничего'): 'reask',
    ('Опечатк', 'Исправлена опечатка'): 'reask',
    ('Опечатк', 'раскладки'): 'reask',
    ('Опечатк', 'кавычк'): 'reask',
    ('Опечатк', 'Длинн'): '/wiz/request_filter/',
    ('Опечатка и Ко', 'Исправлена опечатка'): 'reask',
    ('Опечатк', 'Антипиратски'): '/wiz/anti_pirate/',
    ('Опечатк', 'забвение', 'забвение', 'забвение'): '/wiz/anti_pirate/',
    ('Опечатк', 'Быть может вы искали'): '/wiz/misspell/',
    ('Опечатк', 'исключ'): '/wiz/minuswords/',
    ('Опечатк', 'Исключ'): '/wiz/minuswords/',
    ('Опечатк', 'возвратом на сервис'): '/wiz/service_redirect/',
    ('Опечатк', 'Порно'): '/wiz/web_misspell/misspell/porno/',
    # default
    'Опечатк': '/wiz/web_misspell/misspell/',

    # wizards
    'Booking.com - ': '/snippet/booking-rating',
    'Gismeteo - ': '/snippet/gismeteo',
    'Stackoverflow - ': '/snippet/stackoverflow',
    'TripAdvisor - ': '/snippet/tripadvisor-rating',
    'Авиабилеты - ': 'snippet/buy_tickets/',
    'Время - ': 'snippet/time/',
    'Авто - Марка': 'snippet/auto_2/vendor/',
    'Авто - Марка - Подержанные авто': 'snippet/auto_2/vendor/',
    ('Авто - Марка - Неточны', 'поиск auto.ru'): 'snippet/auto_2/general/',
    'Авто - Модель': 'snippet/auto_2/model/',
    ('Авто - Модель - Неточны', 'поиск auto.ru'): 'snippet/auto_2/general/',
    'Авто - Общи': 'snippet/auto_2/common/',
    ('Авто - Общи', ' - Неточны', 'поиск auto.ru'): 'snippet/auto_2/general/',
    'Автомобильные коды - ': 'snippet/auto_regions/',
    ('Адресны', ' - Без карты - несколько организаци'): 'snippet/companies/list/',
    ('Адресны', ' - Без карты - одна организаци'): '/snippet/companies/list/',
    ('Адресны', ' - Одна организация'): 'snippet/companies/company/',
    ('Адресны', ' - Сети-рубрики'): 'snippet/companies/map/',
    ('Адресны', ' - Текстовы', ' вид'): 'snippet/companies/map/',
    ('Адресны', ' - Загрузка та', 'после геопозиционирования'): 'snippet/companies/map/',
    ('Адресны', ' - Спецсниппет Одна точка'): '/snippet/adress_button/', # https://testpalm.yandex-team.ru/testcase/serp-2130
    ('Адресны', ' - Спецсниппет Много точек'): '/snippet/adress_button/', # https://testpalm.yandex-team.ru/testcase/serp-2950
    ('Антипиратски'): '/snippet/anti_pirate/',
    'БНО - ': '/snippet/bno/',
    'Бронировщик - ': '/snippet/booking-rating',
    'Геометрические фигуры - ': '/wiz/math/',
    'Денежные переводы - ': '/snippet/transfer/', # XXX exacly with ending / !! it is different from /snippet/transfer !!
    'Договор купли-продажи авто - ': '/snippet/autoru',
    ('Завирусованны', ' сниппет - '): '/snippet/infected/',
    'Игры - ': '/wiz/yaca_games/',
    'Индексы - ': 'snippet/post_indexes/',
    'Индексы - Индекс по конкретному адресу': 'snippet/post_indexes/house/',
    'Индексы - Индекс по улице': 'snippet/post_indexes/street/',
    'Калькулятор - Калькулятор': '/wiz/calculator/',
    'Калькулятор - ': 'snippet/calculator/',
    'Картинки - ': 'snippet/images/',
    'Афиша ': 'snippet/afisha/rubrics/',
    'Афиша - Событие': 'snippet/afisha/event/',
    'КЛД мобильного приложения контекстны': 'snippet/context_app_distribution_wizard/',
    'Конвертер': '/snippet/graph/currencies/',
    'Колдунщик ППЛ - ': '/snippet/people/showcase/',
    'Видео - Видео - ': 'snippet/video/',
    'Карточка быстрого ответа на p0 - ': '/snippet/long_fact',
    'Карточны': 'snippet/maps/', # TODO й
    ('Карточны', ' - Координаты'): 'snippet/maps/coord/',
    ('Карточны', ' - Топонимы с пробками и маршрутным блоком') : 'snippet/maps/', # TODO snippets.full.traffic => 1
    ('Карточны', ' - Топонимы с пробками по городу') : 'snippet/traffic/', # TODO й
    'Маркет - Вендор': 'snippet/mcv/',
    'Маркет - Доставка': '/snippet/market_snippets/',
    'Маркет - Карточка модели': 'snippet/mmc/',
    'Маркет - Категория и вендор': 'snippet/mccv/',
    'Маркет - Категория': 'snippet/market/ext_category/',
    'Маркет - Книги': 'snippet/mmc/',
    'Маркет - Контекстно-зависимые. Категории': '/snippet/topic/market/',
    'Маркет - Контекстно-зависимые. Отзывы': 'snippet/mmc/',
    'Маркет - Неявная модель': 'snippet/market/implicit_model/',
    'Маркет - Одежда': '/snippet/market/clothes/',
    'Маркет - Предложения': 'snippet/market/offers/',
    'Маркет - Родительская категория': 'snippet/market/parent_category/',
    'Маршруты - ': 'snippet/route/',
    ('Музыкальны', 'стихолюб'): 'snippet/lyrics/',
    ('Музыкальны', ' пле', 'ер - Альбом'): 'snippet/musicplayer/album/',
    ('Музыкальны', ' пле', 'ер - Исполнитель'): 'snippet/musicplayer/artist/',
    ('Музыкальны', ' пле', 'ер - Песня'): 'snippet/musicplayer/track/',
    ('Музыкальны', ' пле', 'ер - Подборка'): 'snippet/musicplayer/playlist/',
    ('Музыкальны', ' пле', 'ер - Текстовы'): 'snippet/musicplayer/artist/',
    'Метро - ': 'snippet/metro/',
    'Недвижимость - Витринны': 'snippet/realty/complex',
    'Недвижимость - Конкретная': 'snippet/realty/site-thumb',
    'Недвижимость - Новостро': 'snippet/realty/newbuilding',
    'Недвижимость - Общи': 'snippet/realty/text',
    'Недвижимость - Объявления': 'snippet/realty/offers',
    'Недвижимость - Прием объявлени': 'snippet/realty/add',
    'Новости - ': 'snippet/news/',
    'Оплата ': 'snippet/payments/',
    'Оплата кредитов - ': '/snippet/credits',
    'Оплата налогов - ': 'snippet/taxes/',
    'Панорамы - Музеи': 'snippet/museum_panoramas/',
    'Оскара - ': '/parallel/result/snippet/pseudo_fast/oscar_oo/', # TODO make schema for /snippet/oscar/ ??
    'Отели - Витрина - ': '/snippet/tours/rooms/hotels',
    'Отели - Витрина - общи': '/snippet/tours/directions',
    'Отели - Просто': '/snippet/tours/hotel/snippet',
    'Отели - Таблица': '/snippet/tours/hotel/prices',
    ('Отключение горяче', ' воды - '): '/snippet/cold_water/',
    'Панорамы - Улица города': 'snippet/panoramas/',
    'Перевод - Проброс': '/snippet/translate/text/',
    'Перевод - Пусто': '/snippet/translate/empty/',
    'Погода - ': 'snippet/weather/',
    'Псевдо - Псевдо - Тестовы': 'snippet/pseudo_fast/test_fast',
    'Псевдо - Псевдо - Конкретны': '/wiz/pseudo_fast/opera__one_/',
    'Псевдо - Псевдо - Много': '/wiz/pseudo_fast/browsers_all/',
    'Работа - ': 'snippet/rabota/region/',
    'Работа в компании': 'snippet/rabota/profession/',
    'Работа по специальности': 'snippet/rabota/profession/',
    'Радио.Несколько радиостанци': 'snippet/radio_generic',
    'Радио.Одна радиостанция': 'snippet/radio',
    'Расписание - ': 'snippet/rasp_route/transports/',
    'Расписание - Табло аэропорта': 'snippet/rasp_route/airport_panel/',
    ('Расписание - Фирменны', ' поезд'): 'snippet/rasp_route/directions/',
    ('Расписание - Фирменны', ' поезд - с уточнением маршрута'): 'snippet/rasp_route/transports/', # TODO check aqua
    'Расписание - Электрички': 'snippet/rasp_route/suburban_directions/',
    ('Ре', 'тинги - ', ' в сниппетах'): '/snippet/snip_rating',
    'Рецепты - ': '/snippet/recipe/', # will be '/snippet/foto_recipe' in construct
    'РЛС-факты - ': 'snippet/rlsfacts/',
    'Сериалы - Сериалы': '/snippet/video/',
    'Связанные запросы - ': '/wiz/request_extensions/',
    'Cпецсниппет рецептов - ': '/snippet/recipe/',
    ('Сниппет кинотеатра', 'с кнопко', ' Купить билет'): '/snippet/tickets_venue/',
    'Сниппеты - Серая дата': '/snippet/special_dates/',
    'Сниппеты - Черная дата': '/snippet/special_dates/',
    'Соцсети - ': '/snippet/social_snippet/',
    'Социальные атрибуты - ': '/snippet/social_annotation/',
    'Спецсниппет с цено': '/snippet/prices',
    ('Спецсниппет та', 'мла', 'на -'): '/snippet/timeline',
    'Спецсниппет с документом - ': '/snippet/mime_view/',
    ('Спорт - ', 'турнир'): 'snippet/sport/tournament/football_competition/',
    ('Спорт - Пле'): 'snippet/sport/tournament/football_competition/',
    ('Спорт - ', 'КХЛ турнир'): 'snippet/sport/tournament/hockey_competition/',
    ('Спорт - Матч ', ' футбол'): '/snippet/sport/livescore/football_match/',
    ('Спорт - Матч ', ' хокке'): '/snippet/sport/livescore/hockey_match/',
    'Стихолюб - ': '/wiz/poetry_lover/lyrics/',
    'Страница всех объявлени': '/snippet/yabs_proxy/',
    'Такси - ': 'snippet/taxi/',
    'Телепрограмма - ': '/snippet/tv/timetable/',
    ('Телепрограмма - ', 'телеканала'): '/snippet/tv/channel/',
    ('Телепрограмма - ', 'телепередачи'): '/snippet/tv/program/',
    ('Товарны', ' сниппет - с картинко'): '/snippet/market/',
    ('Фактовые - ', 'Фактовы', ' ответ -', 'entity-fact'): '/snippet/entity-fact',
    ('Фактовые - ', 'Фактовы', ' ответ -', 'long-fact'): '/snippet/long_fact',
    ('Фактовые - ', 'Фактовы', ' ответ -', 'suggect-fact'): '/snippet/suggest_fact',
    ('Фактовые - ', 'Фактовы', ' ответ -', 'Расстояние'): '/snippet/distance_fact',
    'Фактовые - Фактовые - IP': '/wiz/internet/',
    'Фактовые - Фактовые - Бензин': '/snippet/benzin/',
    'Фактовые - Фактовые - Курс акци': '/snippet/quotes/',
    'Фактовые - Фактовые - Психологическая помощь': '/snippet/psycho/',
    'Экстренные': '/snippet/suggest_fact',
    'Цвета - ': '/snippet/colors/',
    'Видео-решения по математике - ': 'snippet/pseudo_fast/solver/',
    ('Объектны', ' ответ - '): '/snippet/entity_search/',
    # snippets
    'Форумы - Список форумов': '/snippet/generic/~forum_forums',
    'Форумы - Сообщения': '/snippet/generic/~forums',
    'Форумы - Статистика точного кол-ва сообщени': '/snippet/generic/~forum_topic',
    'Форумы - Список тем': '/snippet/generic/~forum_topic',
    'Список обычны': '/snippet/generic/~list_snip',
    'Сниппеты - Базовая функциональность': '/snippet/generic/~generic',

    # com.tr
    ('Товарны', ' сниппет - с цено'): '/snippet/turkish_price/',
    ('~COMTR', 'Local search -', 'Карточка оффла', 'организации'): '/snippet/address_ymaps/company/',
    ('~COMTR', 'Local search -', 'Оффла', 'новые организации'): '/snippet/address_ymaps/company/',
    ('~COMTR', 'Игры - '): '/snippet/lists/',
    ('~COMTR', 'Карточка быстрого ответа на p0 - '): '/snippet/turkey_exp/dreams/',
    ('~COMTR', 'Рамазан - '): '/snippet/prayers/',
    ('~COMTR', 'Результаты лотере'): '/snippet/lottery/',
    ('~COMTR', 'Ре', 'тинги - ', ' в сниппетах'): '/snippet/ratings_reviews/',
    ('~COMTR', 'Спецсниппет с цено'): '/snippet/turkish_price/',
    ('~COMTR', 'Стихолюб'): '/wiz/poetry_lover/prayer/',
}

# curl "https://zelo.serp.yandex.ru/yandsearch?json_dump=rdat.flags._flags_allowed
AllowedFlags = { "3docs_newswiz" : 1, "CONTEXT" : "json", "QA_assoc_list" : 1, "QA_card_p0" : 1, "QA_homonyms_list" : 1, "QA_mobile_formula" : 1, "QA_mobile_pos" : 1, "QA_right_card" : 1, "QA_show_assoc" : 1, "QA_upper_list" : 1, "QA_yanswer_touch" : 1, "US_20" : 1, "accept_api" : 1, "accept_pushtocall" : 1, "add_touch_to_counters" : 1, "adjust_serp_size" : 1, "admarketplace" : 1, "adresa_to_right" : 1, "afisha" : 1, "afisha_bilet_pay" : 1, "all_sitelinks" : 1, "altsearch_atom" : 1, "altsearchcounter" : 1, "antipirate" : 1, "antipirate_libels" : 1, "antipirate_wiz" : 1, "antispam_exp_no_images" : 1, "app_host" : "map", "app_host_path" : "map", "app_host_source" : "map", "app_host_srcask" : "map", "app_host_srcskip" : "map", "appsearch_header" : 1, "appsearch_header_tablet" : 1, "atom_proxy" : 1, "atom_relev" : 1, "atom_stripe" : 1, "atom_stripe_show" : 1, "atom_touch" : 1, "atom_touch_old" : 1, "atom_united" : 1, "auto_exp_money" : 1, "auto_serpdata" : 1, "autoregions_suggest_data" : 1, "behavioral_direct" : 1, "between_direct" : 1, "big_thumbnails" : 1, "blog_desktop_report" : 1, "brosearch_splitview" : 1, "browser_search_bar" : 1, "browser_splitview" : 1, "bs_debug" : 1, "buy_tickets_new" : 1, "cache_control_max_age" : 1, "car_brand" : 1, "clck_host" : 1, "company_object_badge" : 1, "construct_as_array" : 1, "content_preview" : 1, "content_preview_images" : 1, "cp_highlight" : 1, "d_mobile_wizards" : 1, "debug_wizard" : 1, "degradation_mode" : 1, "design_exp" : 1, "detected_by_autotest" : 1, "developers_game" : 1, "direct_favicon_navmx" : 1, "direct_label" : 1, "direct_navmx_threshold" : 1, "direct_page" : 1, "direct_raw_parameters" : 1, "disableStylesInLocalStorage" : 1, "disable_adresa_snippet" : 1, "disable_ajax" : 1, "disable_all_experiments" : 1, "disable_doc_snippet" : 1, "disable_encrypt" : 1, "disable_https" : 1, "disable_idle_notification" : 1, "disable_images3" : 1, "disable_infected_snip" : 1, "disable_json_template_adapters" : 1, "disable_lang_detection" : 1, "disable_market_cliding" : 1, "disable_market_snippets" : 1, "disable_mda" : 1, "disable_news_shift_down" : 1, "disable_newsdel_wizextra" : 1, "disable_pseudo" : 1, "disable_recipes" : 1, "disable_report_cache_freshness" : 1, "disable_second_bno" : 1, "disable_set_search_engine" : 1, "disable_sitelinks" : 1, "disable_snippets" : "map", "disable_sport" : 1, "disable_video_snippets" : 1, "disable_video_wizard_by_snippets" : 1, "disable_wizards" : "map", "distr_thematic" : 1, "distribution_and_first_position_wizard" : 1, "distribution_instead_of_pseudo" : 1, "distribution_wizard" : 1, "distribution_wizard_atom" : 1, "distribution_wizard_lower" : 1, "do_images_blend" : 1, "do_video_blend" : 1, "dup_group_size" : 1, "enable_afisha_touch_event" : 1, "enable_bh" : 1, "enable_context_verticals" : 1, "enable_csp" : 1, "enable_ehow" : 1, "enable_full_gamification" : 1, "enable_full_gamification_video" : 1, "enable_hotwater_iframe" : 1, "enable_hotwater_serp" : 1, "enable_hsts" : 1, "enable_https" : 1, "enable_https_nets" : 1, "enable_https_xmlsearch" : 1, "enable_i-m-not-a-hacker" : 1, "enable_images3" : 1, "enable_museum_panoramas_by_type" : 1, "enable_new_format_geov" : 1, "enable_news_rubrics" : 1, "enable_newsp_realtime" : 1, "enable_only_snippets" : "map", "enable_only_wizards" : "map", "enable_quick_dev" : 1, "enable_quick_experimental" : 1, "enable_quick_stg" : 1, "enable_scarab_log" : 1, "enable_scarab_profile_log" : 1, "enable_sdch" : 1, "enable_shadow_sources" : 1, "enable_smart_verticals" : 1, "enable_snippets" : "map", "enable_suggestity" : 1, "enable_tr_docviewer" : 1, "enable_tr_games_wizard" : 1, "enable_web_external" : 1, "enable_web_external2" : 1, "enable_web_misspell_reask" : 1, "enable_wizard_json" : 1, "enable_wizards" : "map", "enable_yaca_games" : 1, "express_opinion" : 1, "extended_auto" : 1, "extended_video" : 1, "extra_docs" : 1, "facts_important" : 1, "facts_instant" : 1, "facts_serpdata" : 1, "fastres_exp_1" : 1, "fastres_exp_10" : 1, "fastres_exp_11" : 1, "fastres_exp_12" : 1, "fastres_exp_13" : 1, "fastres_exp_14" : 1, "fastres_exp_15" : 1, "fastres_exp_16" : 1, "fastres_exp_17" : 1, "fastres_exp_18" : 1, "fastres_exp_19" : 1, "fastres_exp_2" : 1, "fastres_exp_20" : 1, "fastres_exp_3" : 1, "fastres_exp_4" : 1, "fastres_exp_5" : 1, "fastres_exp_6" : 1, "fastres_exp_7" : 1, "fastres_exp_8" : 1, "fastres_exp_9" : 1, "fastres_test_data" : 1, "filters_fixed" : 1, "fines_force_card" : 1, "fix_adblock" : 1, "footer_touch_distribution" : 1, "force_distribution_wizard" : 1, "force_https_msearch" : 1, "force_https" : 1, "force_https_imgs" : 1, "fresh_greenurl_usual" : 1, "full_sitelinks" : 1, "gateway_params_filtering" : 1, "geo_intent_filters" : 1, "geo_intent_with_only" : 1, "geomisc" : 1, "geov_add_trash" : 1, "geov_cancel_minres_maxspn" : 1, "geov_common_with_rubric_formula" : 1, "geov_disable_display_mode_on_tld" : 1, "geov_disable_geoaddr" : 1, "geov_disable_geoshard" : 1, "geov_disable_nav_result" : 1, "geov_disable_nk" : 1, "geov_enable_geoshard" : 1, "geov_enable_geowhere_filter" : 1, "geov_enable_nk" : 1, "geov_enable_quorum" : 1, "geov_enable_rich_content" : 1, "geov_list" : 1, "geov_maxspn_thresh" : 1, "geov_minres" : 1, "geov_new_hide" : 1, "geov_old_setup" : 1, "geov_old_show" : 1, "geov_remove_1_4_7" : 1, "geov_reverse_control" : 1, "geov_reverse_minres_maxspn" : 1, "geov_reverse_ranking" : 1, "geov_rubric_wizard_extensions" : 1, "geov_shift_window_for_small_geo" : 1, "geov_shortname" : 1, "geov_vs_entity" : 1, "gosuslugi_emias" : 1, "gpauto_ttl" : 1, "group_calendar" : 1, "gurulight_offers" : 1, "gurulight_offers_ctrl" : 1, "gw_no_adresa_snippet_filtering" : 1, "head-in-presearch" : 1, "hide_calc_if_suggest" : 1, "hide_mkb_if_alt_browser" : 1, "hide_mkb_if_distribution_wizard" : 1, "hide_pseudo_if_browser_site" : 1, "hide_stripe_universal" : 1, "hide_stripe_universal_if_distribution_wizard" : 1, "hide_teaser_if_distribution_wizard" : 1, "hide_turkish_market" : 1, "hotwater" : 1, "image_series" : 1, "images_adaptive_config" : 1, "images_atom_wizard" : 1, "images_auto_wallpaper_filter" : 1, "images_base64_thumbs_n" : 1, "images_basic_thumbs" : 1, "images_comm_scroll" : 1, "images_commercial" : 1, "images_commercial_marketing_bottom" : 1, "images_commercial_marketing_top" : 1, "images_commercial_marketing_type" : 1, "images_commercial_max_offers" : 1, "images_commercial_max_offers_per_doc" : 1, "images_commercial_money_source" : 1, "images_commercial_organic" : 1, "images_commercial_parallels" : 1, "images_commercial_request" : 1, "images_commercial_series" : 1, "images_delay" : 1, "images_direct_timer_time" : 1, "images_disable_ocr" : 1, "images_enable_mds_avatars" : 1, "images_fake_direct_request" : 1, "images_fake_direct_show" : 1, "images_fml" : 1, "images_force_scheme" : 1, "images_fresh_by_blender" : 1, "images_frozen_templates" : 1, "images_groups_limit" : 1, "images_grunwald" : 1, "images_js_smart" : 1, "images_logotablo" : 1, "images_main_category" : 1, "images_market_request" : 1, "images_market_show" : 1, "images_numdoc" : 1, "images_ocr_split" : 1, "images_opened_related_documentid" : 1, "images_pad_templates" : 1, "images_pdb_wizard" : 1, "images_porno_wizard_ignore_psi" : 1, "images_previews_qty" : 1, "images_proxy_preview" : 1, "images_related_basic_cgi_params" : 1, "images_related_in_wizard" : 1, "images_related_queries" : 1, "images_related_sort" : 1, "images_related_tld_search_attr" : 1, "images_related_tld_suffix" : 1, "images_show_porno_misspell" : 1, "images_snippets_cache" : 1, "images_spdy" : 1, "images_thumbs_lag" : 1, "images_translate" : 1, "images_translate_wizard" : 1, "images_ultra_request" : 1, "images_user_region_as_lr" : 1, "images_wizard_antimarkers_filter" : 1, "images_wizard_page_size" : 1, "img_no_series" : 1, "img_series" : 1, "img_series_tr" : 1, "img_thumb_spd" : 1, "img_touch_counters" : 1, "imginterface_lag" : 1, "imginterface_load_lag" : 1, "imgs_big_thumbs" : 1, "imgs_extra_ads" : 1, "imgs_no_filters" : 1, "imgs_no_fluider" : 1, "imgs_no_fridge" : 1, "imgs_no_fullscreen" : 1, "imgs_no_imgsearch_no_share" : 1, "imgs_no_open" : 1, "imgs_no_polaroid" : 1, "imgs_no_slider" : 1, "imgs_no_tab" : 1, "imgs_small_thumbs" : 1, "imgs_thumbs_dups" : 1, "imgscroll_pages" : 1, "imgsnip_intent" : 1, "imgsnip_ratio" : 1, "imgthumb_break" : 1, "imgthumbs_n" : 1, "important_sport" : 1, "infected_touch" : 1, "inline_distribution" : 1, "internet_or_single_iframe" : 1, "is_wallpaper" : 1, "is_wallpaper_3" : 1, "is_wallpaper_for_com_tr" : 1, "its_location" : 1, "js_plain" : "map", "js_template_dir" : 1, "json_plain" : "map", "json_template" : 1, "json_template_external" : 1, "json_template_profile" : 1, "jsredir_exclude_param" : 1, "kinopoisk" : 1, "kinopoisk_ctrl" : 1, "l10n" : 1, "laas_geopoint" : 1, "link_header" : 1, "local_search" : 1, "log_bs_counter" : 1, "log_post_data" : 1, "log_user_region_laas" : 1, "long_direct" : 1, "look_at_szm" : 1, "lottery_new_data" : 1, "maps_common" : 1, "maps_disable_old_data" : 1, "maps_wizard_text" : 1, "market_brand_ctrl" : 1, "market_clid" : 1, "market_clothes_filter" : 1, "market_implicit_model_ctrl" : 1, "market_model_tab" : 1, "market_model_tab_control" : 1, "market_model_tab_exp" : 1, "market_model_tab_off" : 1, "market_offers_ctrl" : 1, "market_offers_right" : 1, "market_offers_test" : 1, "market_parent_category" : 1, "market_parent_category_ctrl" : 1, "market_rearr" : 1, "market_region_ctrl" : 1, "market_region_test" : 1, "market_right_duplicate" : 1, "market_snippet_clid" : 1, "marketsource" : 1, "max_dup_group_count" : 1, "min_images_amount_for_snippets" : 1, "misspell_noupper" : 1, "mm_enable_csp" : 1, "mmc_unpainted_title_ctrl" : 1, "mmc_unpainted_title_exp" : 1, "mobile_iframe" : 1, "money_test_mode" : 1, "msearch_counters" : 1, "neformat_wiz" : 1, "neguru_offers" : 1, "neguru_offers_ctrl" : 1, "new_distance_wizard" : 1, "new_infected_snip" : 1, "new_request_ext" : 1, "news_cutsnip" : 1, "news_img_top" : 1, "news_is_tr_exp" : 1, "news_nosnip" : 1, "news_rubricsearch" : 1, "news_shift_down" : 1, "news_title_length" : 1, "news_under_blender" : 1, "news_webqtree" : 1, "no_afisha_blend" : 1, "no_app_banner" : 1, "no_aw" : 1, "no_blockstat_report" : 1, "no_direct_favicons" : 1, "no_geo_domain_redirect" : 1, "no_images_blend" : 1, "no_images_grunwald" : 1, "no_jsredir" : 1, "no_laas_geobase_fallback" : 1, "no_ll_spn" : 1, "no_market_clothes_filter" : 1, "no_music_player_blend" : 1, "no_parallel_wizplace" : 1, "no_tv_snip_sd" : 1, "no_video_blend" : 1, "no_wizard_vb" : 1, "noapache_json_req" : 1, "noapache_json_res" : 1, "nobanner" : 1, "nobanners" : 1, "noredirect_com" : 1, "norepeat_direct_banners" : 1, "nosnip" : "map", "nowiz" : "map", "npsdelay" : 1, "npsqueryprob" : 1, "numdoc" : 1, "object_important" : 1, "oblivion_wiz" : 1, "off_templates_flags_allowed_checking" : 1, "old_misspell" : 1, "old_yellow_direct" : 1, "olymp_noexpire" : 1, "padsearch_to_touchsearch" : 1, "people_touch" : 1, "phone_display_mode" : 1, "plate" : 1, "pollresults" : 1, "pollresults_custom_key" : 1, "pollstations" : 1, "pollstations_touch" : 1, "postalcodes_geosup" : 1, "prayers_proxy" : 1, "pre_search_sleep" : 1, "presearch_arrow" : 1, "preview_noquick" : 1, "preview_prod" : 1, "preview_templates" : 1, "pseudo_proxy" : 1, "punto_browser_teaser" : 1, "punto_switcher_teaser" : 1, "rasp_avia" : 1, "rasp_proxy" : 1, "rasp_serpdata" : 1, "rearr" : 1, "redir_to_touch" : 1, "regular_direct" : 1, "reqans_new_marks" : 1, "retina_wiz_img" : 1, "right_docs" : 1, "route_city" : 1, "route_tr" : 1, "routes_proxy" : 1, "rtb_block" : 1, "saas_quick" : 1, "safari_remove_clids" : 1, "screw_up_snippets" : 1, "serp3_granny" : 1, "serp3_granny_https" : 1, "serp3_granny_mob" : 1, "service_pay_iframe" : 1, "show_dots" : 1, "show_em_vthumb" : 1, "show_emvthumb_outside_vrezka" : 1, "show_graph" : 1, "show_images_porno_wizard" : 1, "show_portal_distribution_teaser_with_entity_search" : 1, "show_portal_distribution_teaser_with_stripe_universal_atom" : 1, "show_video_porno_wizard" : 1, "show_vthumb" : 1, "showcase_temp_clothes" : 1, "showcase_touch" : 1, "sitelinks_top3" : 1, "sitesearch_saas_misspell" : 1, "sitesearch_set" : "map", "sleep" : 1, "smart2" : 1, "smart_disable_mkb" : 1, "smart_yandsearch" : 1, "snip_width" : 1, "spok" : 1, "srcask" : "map", "srcparams" : "map_list", "srcrwr" : "map", "srcskip" : "map", "star_ratings" : 1, "suggest_context" : 1, "suggest_context_redirect" : 1, "suggest_domain" : 1, "suggest_host" : 1, "suggest_msearch" : 1, "suggestfacts2" : 1, "tablet_grid_snip_width" : 1, "tablet_target_self" : 1, "tablet_yandsearch" : 1, "taxes" : 1, "template" : 1, "template_data_url" : 1, "thumb_hosts_qty" : 1, "time_suggest" : 1, "touch_grid" : 1, "touch_to_granny" : 1, "tr_local_search_1org" : 1, "transfer" : 1, "translate_ino_only" : 1, "translate_proxy" : 1, "trmarket_model_ru" : 1, "true_context_beh" : 1, "tur_translate_wiz" : 1, "ugc_db" : 1, "ugc_edit_facts" : 1, "ugc_edit_oo" : 1, "ugc_features" : 1, "ugc_proxy" : 1, "ugc_reviews" : 1, "ugc_reviews_oo" : 1, "ugc_reviews_org" : 1, "ugc_reviews_phone" : 1, "ull_spn_size" : 1, "units_converter_graph" : 1, "units_converter_important" : 1, "use_ex_production" : 1, "user_region_laas" : 1, "uslugi_ekb" : 1, "video_10_clips_wizard" : 1, "video_and_entitysearch" : 1, "video_atom_wizard" : 1, "video_blocks_config" : 1, "video_blocks_disabled" : "map", "video_cards" : 1, "video_collect_personal_factors" : 1, "video_commercial" : 1, "video_commercial_to_main" : 1, "video_custom_rvq" : 1, "video_delay" : 1, "video_disable_https" : 1, "video_disk_likes" : 1, "video_distrbanner_page" : 1, "video_dup_group_size" : 1, "video_enable_https" : 1, "video_enable_music_manual_clips" : 1, "video_family" : 1, "video_favorites_num_docs" : 1, "video_frozen_templates" : 1, "video_granny_exp" : 1, "video_group_size" : 1, "video_grouping_attr" : 1, "video_head_stripe" : 1, "video_hide_hd" : 1, "video_hide_track_wizard" : 1, "video_history_filtering" : 1, "video_hosting_env" : 1, "video_html5_only" : 1, "video_ignore_players_scheme" : 1, "video_islands_num_docs" : 1, "video_islands_num_docs_top" : 1, "video_max_dup_group_count" : 1, "video_max_recommended_per_clip" : 1, "video_max_title_length" : 1, "video_merge_misspell" : 1, "video_min_tablet_size" : 1, "video_music_tabs_config" : 1, "video_new_desktop" : 1, "video_new_pads" : 1, "video_no_players_from_hosting" : 1, "video_no_related_filtering" : 1, "video_no_related_first_twowords" : 1, "video_no_related_hosting" : 1, "video_no_related_long" : 1, "video_no_related_restriction" : 1, "video_no_related_runtime" : 1, "video_no_related_short" : 1, "video_no_snippets_with_wizard" : 1, "video_no_top_related_runtime" : 1, "video_objects" : 1, "video_oo" : 1, "video_oo_web_qtree" : 1, "video_related_duplicates" : 1, "video_related_html5_only" : 1, "video_related_next_season" : 1, "video_related_shuffle" : 1, "video_relqueries_pos" : 1, "video_request_dg" : 1, "video_rvq_exp" : 1, "video_search_top" : 1, "video_series_struct" : 1, "video_seriesnav" : 1, "video_seriesnav_report" : 1, "video_serrouting" : 1, "video_serundefined" : 1, "video_show_related" : 1, "video_thumb_badness" : 1, "video_title_rvq_replace" : 1, "video_title_show_query" : 1, "video_top_exp" : 1, "video_top_id" : 1, "video_touch_links_to_islands" : 1, "video_touch_supervideo" : 1, "video_track_wizard" : 1, "video_tv_desktop" : 1, "video_uh_filter" : 1, "video_uh_modify" : 1, "video_uh_sort" : 1, "video_userhistory" : 1, "video_wiz_4lines" : 1, "video_wiz_episodes_title" : 1, "video_wiz_to_top" : 1, "video_wizard_html5_only" : 1, "video_wizard_show_top" : 1, "videosnippet2" : 1, "voice_pad" : 1, "wallpapers_images_version" : 1, "wallpapers_web_version" : 1, "weather_right" : 1, "web3_exp" : 1, "web4_dev" : 1, "web_efir_wizard" : 1, "web_rearr" : 1, "webnpsqueryprob" : 1, "wizard_sms" : 1, "wizard_translate" : 1, "wizextra" : "map", "wizfast" : 1, "wizlog" : 1, "wizreport" : 1, "wiztaxi" : 1, "wizweather" : "map", "yabs_no_unsure_misspell" : 1, "yabs_proxy_single" : 1, "yabs_query_region" : 1, "yabs_region" : 1, "yabs_text_from_wizard" : 1, "yabshostname" : 1, "yellow_down_direct" : 1, "youtube_api" : 1 }

def JSD(data):
    return json.dumps(data, indent=4, sort_keys=True, separators=(',', ': '), ensure_ascii=False) + '\n'

def get_match(Names, name):
    for name_pat in reversed(sorted(Names.keys(), key=lambda k: sum(map(len, k)) if isinstance(k, tuple) else len(k))):
        match = Names[name_pat]
        if isinstance(name_pat, tuple):
            assert tuple
            all_pat = True
            for pat in name_pat:
                if pat not in name:
                    all_pat = False
                    break
            if all_pat:
                return match
        elif name_pat in name:
            return match

def gen_subj(jsubj, merge_arrays):
    s = Schema(merge_arrays=merge_arrays)
    s.add_schema({})
    s.add_object(jsubj)
    return s.to_dict()

def patch_schema(subj, name, location, counter_prefix, testpalm, jsubj=None):
    subj['title'] = name

    if testpalm:
        subj['description'] = 'https://testpalm.yandex-team.ru/testcase/' + testpalm

    counter_prefix_pat = r'^/parallel/result/snippet/' if 'docs_right' in location else r'^/'

    if 'counter_prefix' in subj['properties']:
        assert subj['properties']['counter_prefix']['type'] == 'string'
        subj['properties']['counter_prefix'].pop('enum', None)
        subj['properties']['counter_prefix']['pattern'] = counter_prefix_pat
    elif 'counter' in subj['properties'] and 'path' in subj['properties']['counter']['properties']:
        assert subj['properties']['counter']['properties']['path']['type'] == 'string'
        subj['properties']['counter']['properties']['path'].pop('enum', None)
        subj['properties']['counter']['properties']['path']['pattern'] = counter_prefix_pat
    elif 'market' in subj['properties'] and 'counter_prefix' in subj['properties']['market']['properties']:
        # XXX weird case but cover it
        subj['properties']['market']['properties']['counter_prefix'].pop('enum', None)
        subj['properties']['market']['properties']['counter_prefix']['pattern'] = counter_prefix_pat
    elif counter_prefix in ['reask', 'banner', 'navi']:
        pass
    else:
        print [name, location, counter_prefix, testpalm]
        print JSD(subj)
        raise

    flat = subj['properties'].get('serp_info', {}).get('properties', {}).get('flat')
    if flat and 'enum' in flat:
        enum = [1, "1", True] if 1 in flat['enum'] or True in flat['enum'] or "1" in flat['enum'] else [0, "0", False]
        subj['properties']['serp_info']['properties']['flat'] = {"$comment": "WTF FIXME","type":["integer", "string", "boolean"], 'enum': enum}

    if counter_prefix.startswith('/snippet/generic/'):
        subj['properties']['counter_prefix'].pop('pattern', None)
        subj['properties']['counter_prefix']['enum'] = ['/snippet/generic/']
        # allow any other types for snippet=generic
        if '~' not in counter_prefix:
            subj['properties']['type'].pop('enum', None)
        else:
            # type=forums within generic snippet already covered
            pass

    return subj

def gen_schema(f_path, jsubj, jdoc, jdata, location, counter_prefix, tld, testpalm):
            merge_arrays = True
            subj = gen_subj(jsubj, merge_arrays=merge_arrays)

            patch_schema(subj, f_path, location, counter_prefix, testpalm)

            KEEP_SERPDATA_VALUES = []
            extra = []
            wizard_key = tld + '#' + counter_prefix
            verified = False

            if 'market' in subj['properties'] and 'counter_prefix' in subj['properties']['market']['properties']:
                assert jsubj['type'] == 'serp_data/blocks/default'
                assert subj['properties']['market']['properties']['counter_prefix']['type'] == 'string'

            if 'searchdata' not in location:
                assert not jdata['searchdata']['reask']['show_message'], JSD(jdata['searchdata']['reask']) + '\nsearchdata.reask.show_message is on'

            for k in ['working_time', 'metro']:
                if k in jsubj:
                    subj['properties'][k] = {'type':['string', 'null']}

            if counter_prefix in ['/snippet/suggest_fact']:
                if 'Экстренные' in f_path:
                    subj['properties']['source']['enum'] = [jsubj['source']]
                else:
                    subj['properties']['source']['pattern'] = r'^base:'
                subj['properties'].get('question', {})['minLength'] = 1
                subj['properties']['text']['minLength'] = 1
                KEEP_SERPDATA_VALUES.extend(['source', 'question', 'text'])

            if counter_prefix.startswith('/wiz/web_misspell'):
                subj['properties']['kind']['enum'] = [jsubj['kind']]
                assert 'misspell' in jsubj
                items = jsubj['misspell']['items']
                assert items
                for k in ['orig_penalty', 'flags']:
                    items[0].pop(k) # unused in verstka
                subj['properties']['misspell'] = gen_subj({'items': items}, merge_arrays=True)
                subj['properties']['misspell']['properties']['items']['items'][0]['properties']['clear_text']['minLength'] = 1

            if counter_prefix in ['/wiz/request_filter/']:
                subj['properties']['cause']['enum'] = [jsubj['cause']]

            if counter_prefix in ['/snippet/cold_water/']:
                #print JSD(jsubj)
                #print JSD(subj['properties'])
                container = subj['properties']
                if jsubj['mode'] == 'empty':
                    pass
                elif jsubj['mode'] == 'found':
                    container['address']['minLength'] = 1
                    container['ranges']['minItems'] = 1
                    container['ranges']['items'][0]['properties']['vendors']['minItems'] = 1
                    container['ranges']['items'][0]['properties']['off'] = { '$ref': '../core.json#time' }
                    container['ranges']['items'][0]['properties']['on'] = { '$ref': '../core.json#time' }
                else:
                    raise Exception('/snippet/cold_water/ - unknown mode: ' + jsubj['mode'])
                container['cities']['items'][0]['properties']['region'] = { '$ref': '../core.json#region' }
                container['cities']['minItems'] = 1
                container['mode']['enum'] = [jsubj['mode']]
                KEEP_SERPDATA_VALUES = ['mode', 'citites', 'ranges', 'address']

            if counter_prefix in ['/snippet/mime_view/']:
                jsubj['template'] = jsubj['type'] = 'mime_view' # SERP-42139

            if counter_prefix in ['/snippet/special_dates/']:
                container = subj['properties']

                obj = container.get('green')
                if obj and obj.get('properties', {}).get('date'):
                    container['green']['properties']['date'] = { "onOf": [{"type":"null"},{ "$ref": "../core.json#time" }] }

                obj = container.get('date')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['date'] = { "$ref": "../core.json#time" }

            if counter_prefix in ['/snippet/infected/']:
                container = subj['properties']
                obj = container.get('info_url')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['info_url'] = { 'type': 'object', 'allOf': [
                        { "$ref": "../core.json#cgi" },
                        { "type": "object", "properties": {
                            "data" : {
                                "type": "object",
                                "properties": {
                                    "path": { "type": "string", "enum": [jsubj['info_url']['data']['path']] },
                                },
                            }
                        }},
                    ]}

            if counter_prefix in ['/wiz/request_extensions/']:
                for k in ['show', 'total']:
                    if k in subj['properties']:
                        del subj['properties'][k]
                        if k in subj.get('required', []):
                            subj['required'].remove(k)

                container = subj['properties']['items']['items'][0]
                container['required'] = ['text']
                if 'pagerank' in container['properties']:
                    del container['properties']['pagerank']
                assert container['properties']['text']['type'] == 'array', container
                container['properties']['text']['minItems'] = 1
                verified = True

            # https://github.yandex-team.ru/serp/web4/blob/dev/contribs/z-yaca-games/blocks-common/z-yaca-games/z-yaca-games.priv.js
            if counter_prefix in ['/wiz/yaca_games/']:
                container = subj['properties']['cat']['properties']
                obj = container.get('category')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['category'] = { "$ref": "../core.json#category" }

                container = subj['properties']['cat']['properties']['chld']['items']
                obj = container[0]
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container[0] = {"type": "object", "allOf": [{ "$ref": "../core.json#category" }]}

                container = subj['properties']['games']['items'][0]['properties']
                for k in container.keys():
                    if k in ['title', 'green_url']:
                        container[k] = {"type": "string", "minLength": 1}
                    else:
                        container[k] = {"type": ["null", "string"]}
                assert set(['title', 'descr', 'green_url']) & set(container.keys()), container.keys()
                verified = True

            if counter_prefix in ['/wiz/benzin/', '/wiz/lang_hint/']:
                container = subj['properties']
                obj = container.get('region')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['region'] = { "$ref": "../core.json#region" }

            if counter_prefix in ['/snippet/maps/', '/snippet/route/', '/snippet/metro/', '/snippet/traffic/', '/snippet/panoramas/'] or '/snippet/market/' in counter_prefix or '/snippet/afisha/' in counter_prefix:
                container = subj['properties']
                obj = container.get('region')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['region'] = { "$ref": "../core.json#region" }

            if '/snippet/rabota/' in counter_prefix:
                container = subj['properties']['data']['properties']
                obj = container.get('region')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['region'] = { "$ref": "../core.json#region" }

            if counter_prefix == '/snippet/buy_tickets/' and 'regions' in jsubj:
                KEEP_SERPDATA_VALUES = ['regions']
                container = subj['properties']['regions']['properties']
                for key in ['from', 'to']:
                    obj = container.get(key)
                    if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                        container[key] = { "$ref": "../core.json#region" }

            if counter_prefix.startswith('/snippet/auto_2/'):
                container = subj['properties']['regions']['items']
                obj = container[0]
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container[0] = { "$ref": "../core.json#region" }

            if counter_prefix in ['/snippet/video/', '/parallel/result/snippet/video/']:
                container = subj['properties']['clips']['items'][0]['properties']
                for key in ['mtime','cdt']:
                    obj = container.get(key)
                    if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                        container[key] = { "$ref": "../core.json#time" }
                container = subj['properties']['clips']['items'][0]
                if 'required' in container and 'hosting_info' in container['required']:
                    container['required'].remove('hosting_info')

            if counter_prefix == '/snippet/video_snippet/':
                container = subj['properties']
                for key in ['mtime','cdt']:
                    obj = container.get(key)
                    if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                        container[key] = { "$ref": "../core.json#time" }

            if counter_prefix == '/snippet/news/':
                container = subj['properties']['story']['properties']['docs']['items'][0]['properties']
                obj = container.get('time')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['time'] = { "$ref": "../core.json#time" }

            if counter_prefix in ['/snippet/maps/', '/snippet/route/', '/snippet/maps/coord/']:
                container = subj['properties']
                obj = container.get('url')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['url'] = { "$ref": "../core.json#url" }

            if counter_prefix.startswith('/snippet/post_indexes/'):
                container = subj['properties']
                obj = container.get('selected_region')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['selected_region'] = { "$ref": "../core.json#region" }

            if counter_prefix == '/snippet/afisha/event/':
                container = subj['properties']['events']['items'][0]['properties']['data']['properties']['schedule']['items'][0]['properties']['shows']['items'][0]['properties']
                obj = container.get('time')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['time'] = { "$ref": "../core.json#time" }

            if counter_prefix == '/snippet/afisha/place/':
                container = subj['properties']['events']['items'][0]['properties']['data']['properties']

                d = container['timetable']['items'][0]['properties']
                obj = d.get('day')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    d['day'] = { "$ref": "../core.json#time" }

                d = d['events']['items'][0]['properties']['shows']['items'][0]['properties']
                obj = d.get('time')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    d['time'] = { "$ref": "../core.json#time" }

            if counter_prefix == '/snippet/weather/':
                container = subj['properties']
                container['city_disambiguation'] = {'type':'array'}
                container['city_id'] = {'type':'integer'}
                container['cityid'] = {'type':'string'}
                container['current']['properties']['humidity'] = {'type':'number'}
                container['current']['properties']['image-v2']['properties']['content'] = {'type':'string'}
                container['current']['properties']['image-v2']['required'] = ['content']
                container['current']['properties']['image-v2']['type'] = 'object'
                container['current']['properties']['image-v3']['properties']['content'] = {'type':'string'}
                container['current']['properties']['image-v3']['required'] = ['content']
                container['current']['properties']['image-v3']['type'] = 'object'
                container['current']['properties']['pressure']['properties']['content'] = {'type':'integer', 'maximum':999, 'minimum':500}
                container['current']['properties']['pressure']['properties']['units'] = {'type':'string', 'pattern':r'^mm$'}
                container['current']['properties']['pressure']['required'] = ['content', 'units']
                container['current']['properties']['pressure']['type'] = 'object'
                container['current']['properties']['temperature'] = {'type':'number', 'maximum':100, 'minimum':-273.15}
                container['current']['properties']['temperature-data']['properties']['avg']['properties']['bgcolor'] = {'type':'string', 'pattern':r'^[0-9a-f]{6}$'}
                container['current']['properties']['temperature-data']['properties']['avg']['properties']['content'] = {'type':'integer'}
                container['current']['properties']['temperature-data']['properties']['avg']['required'] = ['bgcolor', 'content']
                container['current']['properties']['temperature-data']['properties']['avg']['type'] = 'object'
                container['current']['properties']['temperature-data']['required'] = ['avg']
                container['current']['properties']['temperature-data']['type'] = 'object'
                container['current']['properties']['uptime'] = {'type':'string', 'pattern':r'^[0-9]{4}\-[0-9]{2}\-[0-9]{2}T[0-9]{2}\:[0-9]{2}\:[0-9]{2}$'}
                container['current']['properties']['weather_condition']['properties']['code'] = {'type':'string', 'minLength':1}
                container['current']['properties']['weather_condition']['required'] = ['code']
                container['current']['properties']['weather_condition']['type'] = 'object'
                container['current']['properties']['weather_type'] = {'type':'string', 'minLength':1}
                container['current']['properties']['wind_direction'] = {'type':'string', 'minLength':1, 'maxLength':2}
                container['current']['properties']['wind_speed'] = {'type':'number', 'minimum':0}
                container['current']['required'] = ['humidity', 'image-v2', 'image-v3', 'pressure', 'temperature', 'temperature-data', 'uptime', 'weather_condition', 'weather_type', 'wind_direction', 'wind_speed']
                container['current']['type'] = 'object'
                container['current_hour'] = {'type':'integer', 'maximum':23, 'minimum':0}
                container['data']['properties'] = {}
                container['data']['type'] = 'object'
                container['forecast']['items'][0]['properties']['current_part'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['_fallback_prec'] = {'type':'boolean'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['_fallback_temp'] = {'type':'boolean'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['condition'] = {'type':'string', 'minLength':1}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['feels_like'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['feels_like_color'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['hour'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['hour_ts'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['humidity'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['icon'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['prec_mm'] = {'type':'number'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['prec_period'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['pressure_mm'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['pressure_pa'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['temp'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['temp_color'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['temp_water'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['wind_dir'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['wind_gust'] = {'type':'number'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['properties']['wind_speed'] = {'type':'number'}
                container['forecast']['items'][0]['properties']['hours']['items'][0]['required'] = ['_fallback_prec', '_fallback_temp', 'condition', 'feels_like', 'feels_like_color', 'hour', 'hour_ts', 'humidity', 'icon', 'prec_mm', 'prec_period', 'pressure_mm', 'pressure_pa', 'temp', 'temp_color', 'wind_dir', 'wind_speed']
                container['forecast']['items'][0]['properties']['hours']['items'][0]['type'] = 'object'
                container['forecast']['items'][0]['properties']['hours']['maxItems'] = 24
                container['forecast']['items'][0]['properties']['hours']['minItems'] = 24
                container['forecast']['items'][0]['properties']['hours']['type'] = 'array'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['humidity'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v2']['properties']['content'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v2']['required'] = ['content']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v2']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v3']['properties']['content'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v3']['required'] = ['content']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['image-v3']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['pressure']['properties']['content'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['pressure']['properties']['units'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['pressure']['required'] = ['content', 'units']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['pressure']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temp_avg'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['avg']['properties']['bgcolor'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['avg']['properties']['content'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['avg']['required'] = ['bgcolor', 'content']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['avg']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['from'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['properties']['to'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature-data']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature_from'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature_max'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature_min'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['temperature_to'] = {'type':'integer'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['type'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['weather_condition']['properties']['code'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['weather_condition']['required'] = ['code']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['weather_condition']['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['weather_type'] = {'type':'string'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['properties']['wind_speed'] = {'type':'number'}
                container['forecast']['items'][0]['properties']['parts']['items'][0]['required'] = ['humidity', 'image-v3', 'pressure', 'temperature-data', 'type', 'weather_condition', 'weather_type', 'wind_speed']
                container['forecast']['items'][0]['properties']['parts']['items'][0]['type'] = 'object'
                container['forecast']['items'][0]['properties']['parts']['maxItems'] = 6
                container['forecast']['items'][0]['properties']['parts']['minItems'] = 6
                container['forecast']['items'][0]['properties']['parts']['type'] = 'array'
                container['forecast']['items'][0]['properties']['type'] = {'type':'string'}
                container['forecast']['items'][0]['required'] = ['date', 'hours', 'parts']
                container['forecast']['items'][0]['type'] = 'object'
                container['forecast']['maxItems'] = 9
                container['forecast']['minItems'] = 9
                container['forecast']['type'] = 'array'
                container['get_wind_type'] = {'type':'null'}
                container['link'] = {'type':'string', 'pattern':r'^https?://yandex\..+'}
                container['voiceInfo']['properties']['ru']['items'][0]['properties']['lang'] = {'type':'string'}
                container['voiceInfo']['properties']['ru']['items'][0]['properties']['text'] = {'type':'string'}
                container['voiceInfo']['properties']['ru']['items'][0]['required'] = ['lang', 'text']
                container['voiceInfo']['properties']['ru']['items'][0]['type'] = 'object'
                container['voiceInfo']['properties']['ru']['type'] = 'array'
                container['voiceInfo']['required'] = ['ru']
                container['voiceInfo']['type'] = 'object'
                container['weather_link'] = {'type':'string', 'pattern':r'^https?://yandex\..+'}
                obj = container.get('city')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['city'] = { "$ref": "../core.json#region" }
                KEEP_SERPDATA_VALUES = ['city', 'city_disambiguation', 'city_id', 'cityid', 'current', 'current_hour', 'data', 'forecast', 'get_wind_type', 'link', 'voiceInfo', 'weather_link']

                container = subj['properties']['forecast']['items'][0]['properties']
                obj = container.get('date')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['date'] = { "$ref": "../core.json#time" }

            if counter_prefix == '/snippet/traffic/':
                container = subj['properties']['traffic']['properties']
                obj = container.get('time')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['time'] = { "$ref": "../core.json#time" }

            if '/snippet/sport/tournament/' in counter_prefix:
                container = subj['properties']['data']['properties']['blocks']['items'][0]['properties']
                obj = container.get('active_match_time')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['active_match_time'] = { "$ref": "../core.json#time" }

                container = container['groups']['items'][0]['properties']['group']['items'][0]['properties']
                container['dt_start'] = { "$ref": "../core.json#time" }
                container['dt_finish'] = { "oneOf": [{"type": "null"}, { "$ref": "../core.json#time" }]}

            if '/snippet/sport/livescore/' in counter_prefix:
                container = subj['properties']['data']['properties']
                container['dt_start'] = { "$ref": "../core.json#time" }
                container['dt_finish'] = { "oneOf": [{"type": "null"}, { "$ref": "../core.json#time" }]}

            if counter_prefix in ['/snippet/adress_button/']:
                container = subj['properties']['item']['properties']['numitems']
                assert container['type'] == 'integer'
                if 'Одна' in f_path and jsubj['item']['numitems'] > 1:
                    return
                elif 'Много' in f_path and jsubj['item']['numitems'] == 1:
                    return
                if jsubj['item']['numitems'] > 1:
                    container['minimum'] = 2
                else:
                    container['enum'] = [1]

            if counter_prefix in ['/snippet/adress_button/', '/snippet/bno/', '/snippet/booking/']:
                container = subj['properties']
                obj = container.get('where')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['where'] = { "$ref": "../core.json#region" }
                obj = container.get('city_ru')
                if obj:
                    assert obj['type'] in ['string', 'null'], JSD(jsubj)
                    obj['type'] = ['null', 'string']

            if counter_prefix in ['/snippet/special_dates/']:
                container = subj['properties']['green']['properties']
                if 'date' in container:
                    container['date'] = { "onOf": [ { "type": "null" }, { "$ref": "../core.json#time" } ] }

            if counter_prefix in ['/snippet/market_snippets/', '/snippet/market/']:
                container = subj['properties']
                obj = container.get('matched_delivery_region')
                if obj and obj['type'] == 'object' and obj['properties']['__is_plain']:
                    container['matched_delivery_region'] = { "$ref": "../core.json#region" }

            if 'snippet/social_snippet' in counter_prefix:
                network = jsubj['network']
                assert network
                subj['properties']['network']['enum'] = [network]

            # improove schemas

            if counter_prefix.startswith('/snippet/generic/'):
                #print JSD(jsubj)
                #print JSD(subj['properties'])
                subj['properties']['passages']['minItems'] = subj['properties']['passage_attrs']['minItems'] = 1
                subj['properties']['headline_src'] = {'type':['string','null']}
                subj['properties'].pop('from_base_searh', None)

            if counter_prefix.startswith('/snippet/generic/~generic'):
                subj['properties']['attrs'] = {'type':'object'}

            if counter_prefix.startswith('/snippet/generic/~forum'):
                #print JSD(jsubj)
                #print JSD(subj['properties'])

                subj['properties']['attrs'] = {'type':'object'}
                subj['properties']['passages']['items'][0]['minLength'] = 1
                subj['properties']['passages']['minItems'] = 1

                container = subj['properties']['passage_attrs']['items'][0]['properties']
                container0 = subj['properties']['passage_attrs']['items'][0]

                container['forum_anchor'] = {'type':'string', 'pattern': r'^#.'}
                container['forum_items'] = {'type':'string', 'pattern': r'^[1-9][0-9]*$'}
                container['forum_url'] = {'type':'string', 'pattern': r'^https?://.'}
                container['forum_date'] = {'type':'string', 'pattern': r'^[0-9]{2}\.[0-9]{2}\.[0-9]{4}$'}

                if jsubj['type'] == 'forums':
                    # serp/web4/blocks-deskpad/forum/_type/forum_type_post.priv.js
                    container0['required'] = ['forum_date']

                    for k in ['forum_lead_ell', 'forum_trail_ell']:
                        container[k] = {'type':'null','$comment':'TODO'}

                else:
                    # serp/web4/blocks-deskpad/forum/_type/forum_type_topic.priv.js
                    container0['required'] = ['forum_url', 'forum_items']
                    container['forum_url']['$comment'] ='forum_url is not really required, remove requireness it if test fails'

                for k in container.keys():
                    if k not in ['forum_date', 'forum_url', 'forum_items', 'forum_anchor', 'forum_lead_ell', 'forum_trail_ell']:
                        container.pop(k, None)

                #verified = True

            if counter_prefix.startswith('/snippet/generic/~list_'):
                subj['properties']['attrs']['properties']['listData']['items'][0] = {
                    'type': 'object', 'required':['if','il','lic'], 'additionalProperties':False, 'properties':
                    {
                        'if': {'type':'integer'},
                        'il': {'type':'integer'},
                        'lic': {'type':'integer'},
                        'lh': {'type':'integer'},
                    }
                }
                verified=True

            if counter_prefix in ['/snippet/graph/currencies/']:
                #print JSD(jsubj)
                container = subj['properties']['data']['properties']['data']['properties']
                container['rel'].pop('required', None) # maybe stub whole obj as {'type':'object'}
                container['src'] = {'type':'object'}
                container['uv']['minItems'] = 1

                container = subj['properties']['data']['properties']
                for k in ['shortFromReadableName', 'toShortReadableName']:
                    container[k]['enum'] = [jsubj['data'][k]]
                container['dataUrl']['pattern'] = r'^https?://yandex\..+/units-converter/'
                if jsubj['data']['historyUrl']:
                    container['historyUrl']['pattern'] = r'^https?://news\.yandex\.'
                #print JSD(subj)
                verified = True

                #TODO if counter_prefix in ['/snippet/graph/currencies/']:
                #print JSD(jsubj)
                subj['properties']['data']['properties']['data']['minItems'] = 1
                container = subj['properties']['data']['properties']
                for k in ['srcName']:
                    container[k]['enum'] = [jsubj['data'][k]]
                #print JSD(subj)
                verified = True

            if counter_prefix in ['/parallel/result/snippet/entity_search/']:
                container = subj['properties']['data']
                for k in ['external']:
                    if k in container['properties']:
                        container['properties'].pop(k)
                        container['required'].remove(k)
                for k in ['client']:
                    if k in container['properties']:
                        container['properties'][k]['enum'] = [jsubj['data'][k]]

                container = subj['properties']['data']['properties']
                if 'related_object' in container:
                    container['related_object']['minItems'] = 1
                    #ro = container['related_object'] = gen_subj(jsubj['data']['related_object'], merge_arrays=False)

            if counter_prefix in ['/snippet/market/offers/']:
                if 'market' in subj['properties']:
                    container = subj['properties']['market']['properties']
                    if 'data' in container:
                        container = container['data']
                        container['properties'].pop('new_formula', None)
                        if 'new_formula' in container['required']:
                            container['required'].remove('new_formula')
                """
                if 'data' in subj['properties']:
                    container = subj['properties']['data']
                    container['properties'].pop('new_formula')
                    container['required'].remove('new_formula')
                """

            if counter_prefix.startswith('/snippet/translate/'):
                if 'empty' in counter_prefix:
                    subj['properties']['lang'] = subj['properties']['text'] = {'type':'string', 'enum':['']}
                else:
                    subj['properties']['text']['minLength'] = 1
                    for k in ['lang', 'from', 'to']:
                        subj['properties'][k]['minLength'] = subj['properties'][k]['maxLength'] = 2

                for k in ['rule','subtype']:
                    subj['properties'][k]['enum'] = [jsubj[k]]

                verified = True

            if counter_prefix in ['/snippet/stackoverflow']:
                subj['properties']['answers']['minItems'] = 1
                subj['properties']['answers']['items'][0]['properties']['url']['pattern'] = r'^http://stackoverflow\.com/.'
                subj['properties']['answers']['items'][0]['properties']['text']['minLength'] = 1
                subj['properties']['answer_count']['minimum'] = 1
                verified = True

            if counter_prefix in ['/snippet/recipe/']:
                assert 'data' in jsubj, JSD(jsubj)
                subj['properties']['data'] = {'type':'object'}

            """ external data, do not cover
            if counter_prefix in ['/snippet/foto_recipe']:
                subj['properties']['ingredients']['minItems'] = 1
                subj['properties']['ingredients']['items'][0]['minLength'] = 1
                subj['properties']['instructions']['minItems'] = 1
                subj['properties']['instructions']['items'][0]['minLength'] = 1
                subj['properties']['name']['minLength'] = 1
                if 'photo' in subj['properties']:
                    subj['properties']['photo']['minItems'] = 1
                    subj['properties']['photo']['maxItems'] = 1
                    verified = True
           """

            if counter_prefix in ['/wiz/internet/']:
                subj['properties']['XFF'] = subj['properties']['XRIP'] ={'type':['string','null']}

            # implement common schema restrictions

            if location.startswith('banner'):
                # https://github.yandex-team.ru/serp/web4/blob/dev/blocks-deskpad/head-stripe/head-stripe.priv.js
                if location.endswith('.stripe_universal'):
                    #print JSD(jsubj)
                    #print JSD(subj)
                    required = subj.get('required', [])
                    for name in ['name', 'type']:
                        if not name in required: required.append(name)
                        subj['properties'][name]['enum'] = [jsubj[name]]
                    if jsubj['name'] == 'geostripe':
                        for name in ['text1', 'text2', 'agreement_url', 'agreement_button']:
                            if not name in required: required.append(name)
                        subj['properties']['agreement_url']['pattern'] = r'^https?://.'
                        for name in ['text1', 'text2', 'agreement_button']:
                            subj['properties'][name]['minLength'] = 1
                    subj['required'] = required
            elif subj['type'] == 'object':
                # remove
                for k in ['package', 'wizplace', 'i18n_domain', 'from_wizard', 'late_on_show', 'relev', 'headline_src']:
                    if k in subj['properties']:
                        subj['properties'].pop(k)
                        if k in subj.get('required', []):
                            subj['required'].remove(k)

                if 'applicable' in jsubj:
                    assert jsubj['applicable'], jsubj
                    subj['properties']['applicable'] = {'type':'integer', 'enum':  [1]}

                if 'slot_rank' in jsubj: # detect SerpData wizards
                    subj['properties'].pop('slot_rank')
                    if 'slot_rank' in subj.get('required', []):
                        subj['required'].remove('slot_rank')

                    # XXX SERPDATA SCHEME CLEANER
                    if jsubj['serp_info'].get('flat') or jsubj['serp_info']['type'] == 'market_vendor':
                        # ... ignore flat serp_data
                        for k in subj['properties'].keys():
                            if k not in ['applicable', 'counter_prefix', 'template', 'type', 'types', 'kind', 'serp_info', 'slot', 'subtype']:
                                if KEEP_SERPDATA_VALUES and k in KEEP_SERPDATA_VALUES: continue
                                subj['properties'].pop(k)
                                if k in subj.get('required', []):
                                    subj['required'].remove(k)
                    else:
                        # ... ignore container 'data'
                        assert 'data' in subj['properties'], JSD(jsubj) + "\nno 'data' container in SerpData wizard"
                        if isinstance(jsubj['data'], dict):
                            subj['properties']['data'] = {'type':'object'}
                        elif isinstance(jsubj['data'], list):
                            subj['properties']['data'] = {'type':'array','minItems':1}
                        else:
                            assert 'THIS KIND OF SERP_DATA NOT IMPLEMENTED', jsubj['data']

                # ignore flat construct data
                if '.construct' in location:
                    for k in subj['properties'].keys():
                        if k not in ['counter', 'type']:
                            if KEEP_SERPDATA_VALUES and k in KEEP_SERPDATA_VALUES: continue
                            subj['properties'].pop(k)
                            if k in subj.get('required', []):
                                subj['required'].remove(k)

                # strict some vital values
                for k in ['type', 'template', 'kind']:
                    if k in subj['properties']:
                        assert subj['properties'][k]['type'] == 'string', subj['properties']
                        subj['properties'][k]['enum'] = [jsubj[k]]

                if 'serp_info' in subj['properties']:
                    container = subj['properties']['serp_info']
                    container['required'] = []
                    for k, v in jsubj['serp_info'].items():
                        if k in ['subtype', 'flat']:
                            if k in ['flat']:
                                enum = [1, "1", True] if jsubj['serp_info'][k] else [0, "0", False]
                                container['properties'][k] = {"$comment": "WTF FIXME", "type":["integer", "string", "boolean"], 'enum': enum }
                            if k in ['subtype']:
                                container['properties'][k]['enum'] = [v]
                            container['required'].append(k)
                        else:
                            container['properties'].pop(k, None)
                    if not container['properties']:
                        del container['properties']
                    if not container['required']:
                        del container['required']

                if 'types' in subj['properties']:
                    container = subj['properties']['types']['properties'].get('main')
                    if container:
                        assert container['type'] == 'string'
                        container['enum'] = [jsubj['types']['main']]

                #if 'subtypes' in subj['properties']:
                #    subj['properties']['subtypes'] = gen_subj(jsubj['subtypes'], merge_arrays=False)

            # implement search paths

            if 'searchdata.docs' in location:
                #subj['properties']['url_parts'] = { "$ref": "../core.json#url" }
                #subj['properties']['saved_copy_url'] = { "$ref": "../types.json#saved_copy_url" }

                #if 'download_saved_copy_url' in subj['properties']:
                #    subj['properties']['download_saved_copy_url'] = { "$ref": "../types.json#saved_copy_url" } # TODO check specific params

                if counter_prefix in AllowMultiple:
                    # TODO support multiple response_scheme
                    # 'snippet/entity_search' => jsubj['data']['display_options']
                    extra.append('%03d' % int(jdoc['num']))
            elif location == 'searchdata': # reask
                # https://github.yandex-team.ru/serp/web4/blob/dev/blocks-common/misspell/_type/misspell_type_reask.priv.js
                subj['properties']['docs_right'] = {'items': [], 'type': 'array'}
                docs = subj['properties']['docs'] = {'items': [], 'type': 'array'}
                if jsubj['docs']:
                    docs['minItems'] = 1
                    subj['properties']['numdocs']['minimum'] = 1
                    subj['properties']['numitems']['minimum'] = 1
                else:
                    docs['maxItems'] = 0
                    subj['properties']['numdocs']['maximum'] = 0
                    subj['properties']['numitems']['maximum'] = 0

                reask = {'type': 'object', 'required': [], 'properties': {}}

                for k, jv in jsubj['reask'].items():
                    if k.startswith('__'):
                        continue
                    reask['required'].append(k)

                    v = reask['properties'][k] = subj['properties']['reask']['properties'][k]

                    if k in ['show_message', 'orig_doc_count', 'enabled']:
                        v['type'] = 'integer'
                        jv = int(0 if jv is None else jv)

                    if v['type'] != 'null':
                        if 'text' in k:
                            assert v['type'] == 'string'
                            if jv:
                                v['minLength'] = 1
                            else:
                                v['enum'] = [jv]
                        elif k == 'orig_doc_count' and jv:
                            v['minimum'] = 1
                        else:
                            v['enum'] = [jv]

                subj['properties']['reask'] = {'type': 'object', 'allOf': [{ "$ref": "../types.json#reask" }, reask]}

                if 'err_code' in jsubj:
                    v = subj['properties']['err_code'] = {'type': 'integer', 'enum': [int(jsubj['err_code'])]}

                if 'err_text' in jsubj:
                    v = subj['properties']['err_text']
                    assert v['type'] == 'string', v
                    v['minLength'] = 1
            elif location == 'navi':
                subj['properties']['ajax_params']['properties']['session_info']['minItems'] = 1
                subj['properties']['cur_page']['enum'] = [0]
                subj['properties']['min_page']['enum'] = [0]
                subj['properties']['first_item']['minimum'] = 1
                subj['properties']['items_on_page']['minimum'] = 1
                subj['properties']['links']['additionalProperties'] = False
                if jsubj['max_page']:
                    subj['properties']['last_page']['minimum'] = 1
                    subj['properties']['max_page']['minimum'] = 1
                    subj['properties']['links']['properties']['pages']['minItems'] = 2
                else:
                    subj['properties']['last_page']['maximum'] = 0
                    subj['properties']['max_page']['maximum'] = 0
                    subj['properties']['links']['properties']['pages']['minItems'] = 1
                    subj['properties']['links']['properties']['pages']['maxItems'] = 1
                    subj['properties']['links']['properties']['pages']['items'][0]['additionalProperties'] = False
                    subj['properties']['links']['properties']['pages']['items'][0]['properties']['num']['maximum'] = 0
                verified = True
            elif location.startswith('banner'):
                pass
            elif location in ['wizplaces', 'wizplaces.*.construct']:
                pass
            else:
                assert False, location + ' NOT IMPLEMENTED'

            if extra:
                wizard_key += '#' + '#'.join(extra)

            str_subj = JSD(subj)
            assert '"__is_plain"' not in str_subj, str_subj + '\n' + f_path + ' - ' + counter_prefix + ' - found uncovered report core items'

            return (subj, wizard_key, verified)

def get_schema_file(marker, testpalm, num, tld):
            if testpalm:
                assert '/' not in testpalm, testpalm
                assert ' ' not in testpalm, testpalm
                assert '.' not in testpalm, testpalm
            marker = re.sub(r'(^/+|/+$)', '', marker)
            marker = re.sub(r'[~#/-]+', '_', marker)
            marker = re.sub(r'(^[_]+|[_]+$)', '', marker)
            marker = marker.replace('__', '_')
            fname = testpalm or 'schema_' + str(i)
            res = marker.lower() + '/' + fname + '.' + tld + '.json'
            assert ' ' not in res, res
            return res

def http_get(url, timeout=10, headers=None):
    try:
        r = requests.request('GET', url, timeout=timeout, headers=headers)
    except (socket.error, requests.exceptions.Timeout, requests.exceptions.HTTPError, requests.exceptions.ConnectionError) as e:
        raise Exception('ERROR: error while fetching: %s\n\t%s' % (e, url))
    else:
        assert r.status_code == 200, 'HTTP code %d\n\t%s' % (r.status_code, url)
        content = r.content
        assert content, 'No content: ' + url
        return content

TESTPALM_TOKEN = None

def testpalm_api(uri):
    global TESTPALM_TOKEN

    assert uri and uri.startswith('/'), uri

    if not TESTPALM_TOKEN:
        testpalm_token_file = os.path.join(os.getenv("HOME"), '.testpalm')
        assert os.path.exists(testpalm_token_file), '\n\nNo TestPalm oauth token\nGo to https://testpalm.yandex-team.ru/internal/profile\nauthorize via passport.yandex-team.ru\nthen read apiToken\nthen save it via running command in shell:\necho TOKEN > ~/.testpalm'
        with open(testpalm_token_file, 'r') as f:
            TESTPALM_TOKEN = f.read().strip()
        assert TESTPALM_TOKEN, 'no testpalm token in ' + testpalm_token_file

    url = 'https://testpalm.yandex-team.ru/api' + uri
    content = http_get(url, headers={ 'TestPalm-Api-Token': TESTPALM_TOKEN })
    assert content, 'No content from' + url
    return json.loads(content)

def get_case_key(f_path, testpalm, tld):
    return (testpalm or f_path) + '~' + (tld or 'ru').upper().replace('.', '')

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--schema_dir", required=True, help="Path to schema_dir")
    parser.add_argument("--test_file_dir", required=True, help="File with test define")
    parser.add_argument("--testpalm", required=False, help="Testpalm testcase key. Example: serp-3252")
    parser.add_argument("--testpalm_type", required=False, default="Desktop_ru", help="Type of SERP from testpalm case")
    parser.add_argument("--counter_prefix", required=False, help="Define id to search object within serp")
    parser.add_argument("--location", required=False, choices=['', 'docs', 'docs_right', 'wizplaces', 'searchdata', 'navi', 'banner.*TODO'], help="precise location if counter_prefix has a name clash")
    args = parser.parse_args()

    schema_dir_root = args.schema_dir
    test_file_dir = args.test_file_dir
    test_file_name = os.path.join(test_file_dir, 'tests_autogen.py')
    testpalm = args.testpalm
    testpalm_type = args.testpalm_type

    DupBySchema = {}
    DupByRequest = {}
    WrittenFiles = {}
    TestPalm = {}
    Tests = {'main': {}}
    Cases = {} # like Tests from generate_wizard_schema.py

    serp_schema_file = os.path.join(schema_dir_root, 'main.json')
    print 'LOADING serp super schema: ' + serp_schema_file
    with open(serp_schema_file) as f:
        serp_schema = json.loads(f.read())
        serp_schema_containers = {
            'navi':
                serp_schema["definitions"]["navi"],
            'wizplaces':
                serp_schema["definitions"]["wizard"],
            'wizplaces.*.construct':
                serp_schema["definitions"]["wizard_construct"]["properties"]["construct"]["items"],
            'banner.data.stripe_universal':
                serp_schema["definitions"]["banner_stripe_universal"]["items"],
            'searchdata':
                serp_schema["definitions"]["searchdata_reask"],
            'searchdata.docs.*.construct':
                serp_schema["definitions"]["doc"]["properties"]["construct"]["items"],
            'searchdata.docs_right.*.snippets.full':
                serp_schema["definitions"]["doc_right"]["properties"]["snippets"]["properties"]["full"],
            'searchdata.docs.*.snippets.main':
                serp_schema["definitions"]["doc"]["properties"]["snippets"]["properties"]["main"],
            'searchdata.docs.*.snippets.full':
                serp_schema["definitions"]["doc"]["properties"]["snippets"]["properties"]["full"],
            'searchdata.docs.*.snippets.pre':
                serp_schema["definitions"]["doc"]["properties"]["snippets"]["properties"]["pre"]["items"],
            'searchdata.docs.*.snippets.inline_pre':
                serp_schema["definitions"]["doc"]["properties"]["snippets"]["properties"]["inline_pre"]["items"],
            'searchdata.docs.*.snippets.post':
                serp_schema["definitions"]["doc"]["properties"]["snippets"]["properties"]["post"]["items"],
        }
        for container in serp_schema_containers.values():
            container["anyOf"] = filter(lambda o: not o.get('$ref', ''), container["anyOf"])

    # WEIRD read python by hand :(
    with open(test_file_name) as f:
        in_block = False
        in_test = False
        test_raw = ''
        for l in f.readlines():
            if l.strip().startswith('TESTS_AUTOGEN'):
                in_block = True
                in_test = False
                continue
            if in_block:
                if l.strip().startswith('{'):
                    in_test = True
                    test_raw = l.rstrip()
                    continue
                if in_test:
                    assert l, 'TODO handle empty line'
                    test_raw += "\n" + l.rstrip()
                    if l.strip().endswith(".json'},") or l.strip().endswith(".json'}"):
                        in_test = False
                        test = eval(test_raw)[0]
                        assert test, test_raw
                        response_schema_abs = os.path.join(schema_dir_root, test['response_schema'])
                        if not os.path.exists(response_schema_abs):
                            continue
                        case_key = get_case_key(test['name'], test.get('testpalm'), test.get('tld'))
                        if case_key in Tests['main']:
                            print test_raw
                            print 'WARNING: removed this case duplicate'
                            continue

                        with open(response_schema_abs) as f:
                            try:
                                sch = json.loads(f.read())
                            except Exception as e:
                                print response_schema_abs
                                raise

                        patch_schema(sch, test['name'], test['location'], test['counter_prefix'], test.get('testpalm'))

                        with open(response_schema_abs, 'w+') as f:
                            f.write(JSD(sch))

                        Tests['main'][case_key] = { "location": test['location'], 'schema_file': test['response_schema'], 'content': test_raw}
                        test_raw = ''
                if l.strip().startswith(']'):
                    break

    """ TODO implement this code vvvvvvv instead ^^^^
    from report.integration.web_manual.tests_autogen import TESTS_AUTOGEN
    for test in TESTS_AUTOGEN:
        case_key = get_case_key(test['name'], test.get('testpalm'), test.get('tld'))
        assert case_key not in Cases, 'case "' + key + '" already added'
        Tests['main'][case_key] = test
    """

    serp_url = None
    testpalm_url = None
    if testpalm:
        assert testpalm.startswith('serp-'), "Invalid testpalm key: " + str(testpalm)

        testpalm_url = 'https://testpalm.yandex-team.ru/testcase/' + testpalm
        testcase = testpalm_api('/testcases/' + testpalm.replace('-', '/'))
        schema_name = testcase['name']
        f_path = schema_name + '.json'
        assert schema_name, JSD(testcase) + '\n\nNo schema found in TestPalm: ' + testpalm_url
        assert testcase['status'] == 'actual', "Invalid testcase status '" + testcase['status'] + "'\nSee: " + testpalm_url

        for st in testcase['properties']:
            stype, surl = st['key'], st['value']
            if stype == testpalm_type:
                serp_url = surl
                print "Found '" + testpalm_type + "' serp url in Testpalm: " + surl
                break

        assert serp_url, JSD(testcase['properties']) + '\n\nNo serp url found for "' + testpalm_type + '" in TestPalm: ' + testpalm_url

        parsed_serp_url = urlparse.urlparse(serp_url)
        parsed_host = parsed_serp_url.netloc
        assert parsed_host.startswith('yandex.'), parsed_serp_url + "\nwrong hostname " + parsed_host
        parsed_tld = parsed_host.split('yandex.')[-1]

        current_case_key = get_case_key(schema_name, testpalm, parsed_tld)
        for k in Tests['main'].keys():
            if k == current_case_key:
                del Tests['main'][k]

        primary_cnt_prefix = args.counter_prefix or get_match(PrimaryWizard, schema_name)
        assert primary_cnt_prefix, 'Set counter_prefix for your object via command line param --counter_prefix or stick it in PrimaryWizard constant here'

        excluded = get_match(ExcludedCasesDict, f_path)
        assert not excluded, "FIXME: " + schema_name + " marked as excluded. It may be obsolete info. Review ExcludedCases item: " + str(excluded)

        forced_docs_kind = args.location or get_match(DocsKind, case_key)

        # redundant hacks
        errmsg = 'Name: ' + schema_name + '\nURL: ' + serp_url + '\nTestPalm: ' + testpalm_url
        i = 0
    else:
        print 'WARNING: NOTHING TO ADD. THERE WILL BE RENEW AUTOGEN TESTS ONLY'

    def parse_jdata():
                    serp_url_json = serp_url + '&export=json'
                    print "FETCHING " + serp_url_json
                    content = http_get(serp_url_json)

                    print 'PARSE EXPORTED JSON'
                    jdata = json.loads(content)
                    assert jdata, 'No jdata'

                    url = 'https://hamster.yandex.' + jdata['reqdata']['tld'] + jdata['reqdata']['unparsed_uri']

                    # setup tld
                    tld = jdata['cgidata']['hostname'].split('.')[-1]
                    tld = 'com.tr' if tld == 'tr' else tld.lower()

                    case_key = get_case_key(schema_name, testpalm, tld)
                    assert case_key not in Cases, 'FIXME: ' + case_key + ' already exists, implement overwrite here'

                    generic_subtype = get_match(GenericSnippetsDict, case_key)

                    ParsedWizards = {}

                    # clean cgi
                    params = OrderedDict()
                    jparams = urlparse.parse_qsl(jdata['cgidata']['raw_text'].encode('utf8'))

                    for k, v in sorted(jparams, key=lambda x: '^^^^' if x[0] == 'text' else x[0]):
                        if k.startswith('geo_promo_') or k.startswith('GEO_'):
                            continue

                        if k in ['export', 'wiztimeout', 'promo', 'test-mode', 'timeout', 'waitall', 'no-tests', 'redircnt']:
                            continue

                        v = urllib.unquote(v.decode('utf8'))

                        if k == 'wizextra':
                            if v.split('=')[0] == 'misspell_timeout':
                                continue
                        elif k in ['exp_flags', 'flag', 'flags']:
                            k = 'flag'
                            v = v.split(';')
                            v = filter(lambda x: x.split('=')[0] not in ['detected_by_autotest', 'noapache_json_req', 'template_data_url'] and AllowedFlags.get(x.split('=')[0]), v)
                        elif k in ['rearr', 'relev', 'snip']:
                            v = v.split(';')
                        elif k in ['suggest_portal_from']:
                            # just take first value
                            if k in params:
                                continue

                        if k in params:
                            if not isinstance(v, list):
                                v = [v]
                            v = params[k] + v if isinstance(params[k], list) else [params[k]] + v

                        if isinstance(v, list):
                            if k != 'text' and not v: continue
                            params[k] = v[0] if len(v) == 1 else sorted(v)
                        else:
                            if k != 'text' and not len(v): continue
                            params[k] = v

                        if k in ['rearr', 'relev', 'snip'] and isinstance(params[k], list):
                            params[k] = ';'.join(sorted(params[k]))

                        if k == 'l10n' and params[k] == 'ru':
                            del params[k]
                            continue

                        assert not re.search('(?:metahost|wizhosts)', k), 'Forbidden param: ' + k + '=' + params[k]
                        assert k != 'text' or 'fake' not in params[k], k + '=' + params[k] + ' is forbidden'

                    #if k not in ['l10n', 'text', 'lr', 'suggest_portal_from', 'noreask']:
                    if 'Конвертер' in f_path: # WIZARD-9336
                        assert 'flag' not in params
                        params['flag'] = 'enable_yaca_games'

                    num = -1

                    print "SEARCHING " + primary_cnt_prefix + " in " + (forced_docs_kind or 'searchdata.docs')

                    for docs_kind in ['docs', 'docs_right']:
                      for jdoc in jdata['searchdata'][docs_kind]:
                        num += 1

                        if forced_docs_kind:
                            if forced_docs_kind not in ['docs', 'docs_right']:
                                break
                            if forced_docs_kind != docs_kind:
                                print ' .... ' + docs_kind + ' postponed '
                                continue

                        #print ' .... ' + docs_kind + '=' + str(num)

                        subjects = []

                        if generic_subtype:
                            s = jdoc['snippets'].get('main')
                            if s and s['counter_prefix'] == '/snippet/generic/':
                                if 'forum' in s['type'] or s['type'] in ['list_snip'] or s['type'] == 'generic':
                                    subjects.append((s, 'searchdata.' + docs_kind + '.*.snippets.main', s['counter_prefix'] + '~' + s['type']))
                                elif s['type'] in ['robots_txt_all_stub', 'yaca', 'dmoz', 'meta_descr', 'mediawiki_fast', 'news', 'mediawiki_snip', 'creativework_snip']:
                                    pass
                                else:
                                    print JSD(s)
                                    raise Exception('Snippet type: ' + s['type'] + ' NOT IMPLEMENTED')

                        if 'full' in jdoc['snippets']:
                            subjects.append((jdoc['snippets']['full'], 'searchdata.' + docs_kind + '.*.snippets.full', jdoc['snippets']['full']['counter_prefix']))

                        if 'construct' in jdoc and jdoc['construct']:
                            if not isinstance(jdoc['construct'], list):
                                jdoc['construct'] = [jdoc['construct']]

                            for construct in jdoc['construct']:
                                subjects.append((construct, 'searchdata.' + docs_kind + '.*.construct', construct['counter']['path']))

                        if 'main' in jdoc['snippets']:
                            subjects.append((jdoc['snippets']['main'], 'searchdata.docs.*.snippets.main', jdoc['snippets']['main']['counter_prefix']))

                        for kind in ['post', 'pre', 'inline_pre']:
                            if kind in jdoc['snippets'] and jdoc['snippets'][kind]:
                                for snip in jdoc['snippets'][kind]:
                                    subjects.append((snip, 'searchdata.docs.*.snippets.' + kind, snip['counter_prefix']))

                        if not subjects:
                            continue

                        subj = -1
                        for subject, location, counter_prefix in subjects:
                            subj += 1

                            assert counter_prefix and isinstance(counter_prefix, basestring)

                            schema_file = get_schema_file(counter_prefix, testpalm, i, tld)
                            out = gen_schema(f_path, subject, jdoc, jdata, location, counter_prefix, tld, testpalm)
                            if not out: continue
                            data, wizard_key, verified = out

                            #print '\t' + str(num) + ': ' + counter_prefix
                            assert wizard_key not in ParsedWizards, wizard_key + ' already in ' + str(ParsedWizards.keys())
                            ParsedWizards[wizard_key] = [f_path, schema_name, schema_file, counter_prefix, jdata['cgidata']['path'], params, tld, data, verified, testpalm, errmsg, location]

                    if 'wizplaces' in jdata and forced_docs_kind and forced_docs_kind == 'wizplaces':
                      num = 99
                      for wp_name, wp in jdata['wizplaces'].items():
                          jwiz_all = []
                          for jo in wp:
                              if 'construct' in jo:
                                assert len(jo) == 1, wp_name + ': expected only construct here\n' + JSD(jo)
                                for jwiz in jo['construct']:
                                    jwiz_all.append(('wizplaces.*.construct', jwiz['counter']['path'], jwiz))
                              else:
                                    jwiz = jo
                                    if 'counter_prefix' in jo:
                                        counter_prefix = jo['counter_prefix']
                                    elif jo.get('type', '') == 'serp_data/blocks/default':
                                        counter_prefix = jwiz.get('market', {}).get('counter_prefix')
                                    jwiz_all.append(('wizplaces', counter_prefix, jwiz))

                          for jo in jwiz_all:
                            num += 1
                            location, counter_prefix, jwiz = jo

                            assert counter_prefix, "%s\ncounter_prefix not found in wizplace '%s': %s" % (str(jwiz), wp_name, str(sorted(jwiz.keys())))
                            assert isinstance(counter_prefix, basestring), counter_prefix

                            schema_file = get_schema_file(counter_prefix, testpalm, i, tld)
                            out = gen_schema(f_path, jwiz, jwiz, jdata, location, counter_prefix, tld, testpalm)
                            if not out: continue
                            data, wizard_key, verified = out

                            #print '\t' + str(num) + ': ' + counter_prefix
                            assert wizard_key not in ParsedWizards, counter_prefix + ' already in ' + str(ParsedWizards.keys())
                            ParsedWizards[wizard_key] = [f_path, schema_name, schema_file, counter_prefix, jdata['cgidata']['path'], params, tld, data, verified, testpalm, errmsg, location]

                    if 'searchdata' in jdata and forced_docs_kind and forced_docs_kind == 'searchdata':
                        location = 'searchdata'
                        counter_prefix = 'reask'
                        schema_file = get_schema_file(counter_prefix, testpalm, i, tld)
                        out = gen_schema(f_path, jdata['searchdata'], jdata['searchdata'], jdata, location, counter_prefix, tld, testpalm)
                        if out:
                            data, wizard_key, verified = out
                            assert wizard_key not in ParsedWizards, counter_prefix + ' already in ' + str(ParsedWizards.keys())
                            ParsedWizards[wizard_key] = [f_path, schema_name, schema_file, counter_prefix, jdata['cgidata']['path'], params, tld, data, verified, testpalm, errmsg, location]

                    if 'navi' in jdata and forced_docs_kind and forced_docs_kind == 'navi':
                        location = 'navi'
                        counter_prefix = 'navi'
                        schema_file = get_schema_file(counter_prefix, testpalm, i, tld)
                        out = gen_schema(f_path, jdata['navi'], jdata['navi'], jdata, location, counter_prefix, tld, testpalm)
                        if out:
                            data, wizard_key, verified = out
                            assert wizard_key not in ParsedWizards, counter_prefix + ' already in ' + str(ParsedWizards.keys())
                            ParsedWizards[wizard_key] = [f_path, schema_name, schema_file, counter_prefix, jdata['cgidata']['path'], params, tld, data, verified, testpalm, errmsg, location]

                    if 'banner' in jdata and forced_docs_kind and forced_docs_kind.startswith('banner'):
                        name = forced_docs_kind.split('.')[1]
                        if name:
                            assert name in jdata['banner']['data'], JSD(jdata['banner']['data'])
                            objs = jdata['banner']['data'][name]
                            assert objs, 'empty ' + forced_docs_kind
                            assert isinstance(objs, list), JSD(objs)
                            jsubj = objs[0]
                            location = 'banner.data.' + name
                        else:
                            location = 'banner'
                            jsubj = jdata['banner']

                        counter_prefix = 'banner'
                        schema_file = get_schema_file(counter_prefix, testpalm, i, tld)
                        out = gen_schema(f_path, jsubj, jdata['banner'], jdata, location, counter_prefix, tld, testpalm)
                        if out:
                            data, wizard_key, verified = out
                            assert wizard_key not in ParsedWizards, counter_prefix + ' already in ' + str(ParsedWizards.keys())
                            ParsedWizards[wizard_key] = [f_path, schema_name, schema_file, counter_prefix, jdata['cgidata']['path'], params, tld, data, verified, testpalm, errmsg, location]

                    assert ParsedWizards, "TODO parse anything there or ignore it in PostponedCases"

                    assert case_key not in Cases, 'Duplicated case: ' + f_path
                    Cases[case_key] = ParsedWizards

                    #print ' .... ' + str(len(ParsedWizards)) + ' - ' + str(sorted(ParsedWizards.keys()))

                    """
                    except AssertionError as e:
                        print traceback.format_exc()
                        raise SystemExit(1)
                    """
                    return ParsedWizards

    def process_case(f_path, schema_name, schema_file, counter_prefix, path, params, tld, data, verified, testpalm, errmsg, location):
            str_name = schema_name.replace("'", '')
            str_path = ", 'path': '/" + path + "'" if path not in ['search/', 'yandsearch'] else ''
            str_tld = ", 'tld': '" + tld + "'" if tld != 'ru' else ''
            str_tp = ", 'testpalm': '" + testpalm + "'" if testpalm else ''
            if tld == 'ru' and int(params.get('lr', 0)) == 213:
                params.pop('lr')
            str_params = re.sub(r'\bu"', '"', re.sub(r"\bu'", "'", str(dict(params.items())).decode('unicode_escape').encode('utf-8')))

            case_key = get_case_key(f_path, testpalm, tld)

            hp = hashlib.md5(tld + '~' + str_params).hexdigest()
            hd = deepcopy(data)
            hd.pop('counter_prefix', None)
            hd.get('counter', {}).pop('path', None)
            hs = hashlib.md5(tld + '~' + counter_prefix + '~' + json.dumps(hd, sort_keys=True).replace(' ', '')).hexdigest()

            reasons = []
            for wiz_cur in DupBySchema.values():
                if hp == wiz_cur[2]:
                    reasons.append('REQUEST')

            if hs in DupBySchema:
                reasons.append('SCHEME')
            else:
                DupBySchema[hs] = (schema_name, schema_file, hp, testpalm)

            if hp in DupByRequest:
                reasons.append('REQUEST')
            else:
                DupByRequest[hp] = (schema_name, schema_file, hp, testpalm)

            for r in set(reasons):
                Reasons[r] = Reasons.get(r, 0) + 1

            def write_schema(f_schema, data):
                assert f_schema not in WrittenFiles, f_schema + ' already exists'
                WrittenFiles[f_schema] = True

                str_data = JSD(data)
                str_data = re.sub(r'{(\s+)("type": "array"\s+)}', r'{\1"items": [{"type": "object"}],\1\2}', str_data, flags=re.S)
                str_data = str_data.replace("[]", "[{\"type\": \"object\"}]")

                d = os.path.dirname(f_schema)
                if not os.path.exists(d):
                    try:
                        os.makedirs(d)
                    except OSError as exc: # Python >2.5
                        if exc.errno == errno.EEXIST and os.path.isdir(path):
                            pass
                        else:
                            raise

                with open(f_schema, 'w+') as f:
                    f.write(str_data)

                call(['/usr/bin/svn', 'add', '--parents', '--force', f_schema])

            def str_reasons(reasons):
                if not reasons: return ''
                res = []
                for r in sorted(set(reasons)):
                    assert r in ['REQUEST', 'SCHEME']
                    dup = DupByRequest if r == 'REQUEST' else DupBySchema
                    hv = hp if r == 'REQUEST' else hs
                    res.append('"%s | %s | %s | %s"' % (r, dup[hv][0], dup[hv][1], dup[hv][3]))
                return ", ".join(res)

            def str_test(reasons=None):
                comment = str_reasons(reasons)
                str_comment = "'comments': [" + comment + "],\n     " if comment else ''
                return "{'name':'" + str_name + "'" + str_tp + ",\n     " + str_comment + "'params':" + str_params + str_path + str_tld + ",\n     'location': '" + location + "', 'counter_prefix': '" + counter_prefix + "', 'response_schema': '" + schema_file + "'},"

            if not reasons or reasons == ['SCHEME']:
                write_schema(os.path.join(schema_dir_root, schema_file), data)
                Tests['main'][case_key] = { "location": location, "schema_file": schema_file, "content": "    " + str_test(reasons) }
            else:
                assert 'REQUEST' in reasons
                raise Exception('FIXME: duplicated case via request. Implement overwrite here')
                #re.sub(r'^(\s*)', r'\1#', str_test(reasons), flags=re.M)

    if serp_url:
        parse_jdata()

        print '\nPROCESSING SCHEMAS'
        assert Cases, 'No cases here'

        for key1 in sorted(Cases.keys(), key=lambda k: re.sub(r'(\.\d+)+', '', k) + '#%04d' % len(k)):
            wizards = Cases[key1]
            found_primary = []

            for key2 in sorted(wizards.keys()):
                f_path, schema_name, schema_file, counter_prefix, path, params, tld, data, verified, testpalm, errmsg, location = wizards[key2]

                if primary_cnt_prefix and counter_prefix.endswith(primary_cnt_prefix):
                    if found_primary and primary_cnt_prefix in AllowMultiple:
                        # TODO check multiple wizards => /snippet/entity_search validate their 'display_options'
                        continue

                    process_case(f_path, schema_name, schema_file, counter_prefix, path, params, tld, data, verified, testpalm, errmsg, location)
                    found_primary.append(counter_prefix)

            assert found_primary, "%s: object with counter_prefix '%s' not found in:\n%s%s\n\nHINT: if object outside searchdata, try define --location cmd line param or stick it in DocsKind constant" % (key1, primary_cnt_prefix, JSD(sorted(wizards.keys())), errmsg)
            assert len(found_primary) == 1, "%s: found multiple counter_prefixes: %s\n\n%s" % (key1, str(found_primary), errmsg)

    print 'WRITING TESTS in ' + test_file_name
    f_test = open(test_file_name, 'w+')
    f_test.write("# -*- coding: utf-8 -*-\n\n")

    f_test.write("TESTS_AUTOGEN = [\n")
    for case_key in sorted(Tests['main'].keys()):
        test = Tests['main'][case_key]
        f_test.write("\n" + test['content'])
    f_test.write("\n]\n")

    f_test.close()

    print 'PATCHING SERP SUPER SCHEMA '
    for test in Tests['main'].values():
        location = test['location']
        assert location in serp_schema_containers, str(test) + "\nIMPLEMENT location=" + location
        container = serp_schema_containers[location]
        container['anyOf'].append({'$ref': os.path.join(test['schema_file']) + '#' })

    seen = set()
    for name, container in serp_schema_containers.items():
        uniq = []
        for o in container["anyOf"]:
            key = JSD(o)
            if key in seen: continue
            seen.add(key)
            uniq.append(o)

        container["anyOf"] = sorted(uniq, key=lambda o: o.get('$ref'))
        assert container["anyOf"], JSD(container) + "\n" + name + ": empty container anyOf!!"

    print 'WRITING SERP SUPER SCHEMA ' + serp_schema_file
    with open(serp_schema_file, 'w+') as f:
        f.write(JSD(serp_schema))

    print 'DONE, please review diff and commit your changes'
