import json
import math
import itertools
import re
import logging

from pytest_bdd import given, when, then, parsers

from extsearch.geo.meta.tests.requester.request import Span

FILTER_MAP = {
    'Wi-Fi': 'wi_fi:1',
    'круглосуточно': 'open_24h:1',
    'кухня: кыргызская': 'type_cuisine:kyrgyz_cuisine',
}


def cgi_params(context):
    return context.request.params


# Conditions (@given) {{{1


@given(parsers.parse('выбрана локаль {locale}'))
def step_user_locale(context, locale):
    context.request.set_locale(locale)


@given(parsers.parse('запрос пользователя равен "{user_query}"'))
def step_user_query_is(context, user_query):
    context.request.set_text(user_query)


@given(parsers.parse('запрос задаётся откуда-то из региона "{region}"'))
def step_request_is_from(context, region):
    context.request.set_span_by_name(region)


@given('запрос задаётся из Мраморного моря')
def step_request_is_from_sea_of_marmara(context):
    context.request.set_span(Span.SeaOfMarmara)


@given('включена детекция команд')
def step_request_with_layers(context):
    context.request.enable_layer_detection()


@given('это запрос с passport_uid')
def step_request_has_passport_uid(context):
    context.request.set_hardcoded_passport_uid()


@given(parsers.parse('пользователь поискал номер телефона "{telephone_number}"'))
def step_user_searched_telephone_number(context, telephone_number):
    context.request.set_telephone_number(telephone_number)


@given(parsers.parse('пользователь поискал урл "{url}"'))
def step_user_searched_url(context, url):
    context.request.set_url(url)


@given(parsers.parse('пользователь поискал "{user_query}" в регионе "{region}"'))
def step_user_searched_text_in_region(context, user_query, region):
    context.request.set_text(user_query)
    context.request.set_span_by_name(region)
    search_result = context.search(cgi_params(context))
    one_response = next(iter(search_result.values()))
    context.request.set_context(one_response.context())
    context.request.set_span(one_response.bounded_by())
    context.search_result = search_result


@given(parsers.parse('запрос из региона "{region_id}"'))
def step_query_from_region_id(context, region_id):
    context.request.set_region(region_id)


@given(parsers.parse('запомнил {N}-й ответ'))
def step_memorized_nth_answer(context, N):
    context.memorized_geo_objects = {
        report: response.geo_objects[int(N) - 1] for report, response in context.search_result.items()
    }
    # To be sure that `search_result` is overwritten
    context.search_result = None


@given(parsers.parse('запомнил все ответы'))
def step_memorized_all_answers(context):
    context.memorized_geo_objects = {report: response.geo_objects for report, response in context.search_result.items()}
    context.search_result = None


@given(parsers.parse('запрос на обратное геокодирование содержит координаты "{ll}"'))
def step_reverse_geocoding_request_contains_coordinates(context, ll):
    context.request.set_reverse_mode_request(ll)


@given(parsers.parse('тип обратного геокодирования "{reverse_geocode_type}"'))
def step_reverse_geocoding_type_is(context, reverse_geocode_type):
    context.request.set_type(reverse_geocode_type)


@given('для поиска доступны обе вертикали')
def step_both_verticals_available(context):
    pass


@given(parsers.parse('для поиска доступна только вертикаль {vertical}'))
def step_only_vertical_available(context, vertical):
    VERTICAL_TO_TYPE = {
        'геокодера': 'geo',
        'ППО': 'biz',
        'коллекций': 'collections',
    }
    context.request.set_type(VERTICAL_TO_TYPE[vertical])


@given(parsers.parse('запрошен сниппет "{snippet}"'))
def step_snippet_is_requested(context, snippet):
    context.request.ask_snippet(snippet)


@given(parsers.parse('запрошены сниппеты\n{snippets}'))
def step_snippets_are_requested(context, snippets):
    context.request.ask_snippet(*snippets.splitlines())


@given(parsers.parse('запрошена рекламная страница "{page}"'))
def step_advert_info_are_requested(context, page):
    context.request.ask_gta('advert')
    context.request.set_advert_page(page)


@given(parsers.parse('передан параметр "{p_name}" со значением "{p_value}"'))
def step_parameter_is_passed(context, p_name, p_value):
    context.request.add_param(p_name, p_value)


@given(parsers.parse('переданы параметры\n{kv}'))
def step_parameters_are_passed(context, kv):
    for row in kv.splitlines():
        p_name, p_value = row.strip().split('=', 1)
        context.request.add_param(p_name, p_value)


@given('пользователь находится в центре окна поиска')
def step_user_is_located_at_span_center(context):
    context.request.set_ull_to_ll()


@given(parsers.parse('пользователь находится в точке "{ll}"'))
def step_user_is_located_at_point(context, ll):
    context.request.set_ull(ll)


@given(parsers.parse('пользователь запрашивает {n} результатов'))
def step_user_requests_n_results(context, n):
    context.request.set_results(n)


@given(parsers.parse('включена реклама на конкурентах'))
def step_injection_by_same_rubric_enabled(context):
    context.request.add_rearr('scheme_Local/Geo/Adverts/InjectionBySameRubric/Enabled=1')


@given(parsers.parse('включено возвращение сниппета конкурентов'))
def step_related_adverts_snippet_enabled(context):
    context.request.add_rearr('scheme_Local/Geo/Adverts/InjectionBySameRubric/RelatedAdvertsSnippetEnabled=1')


@given(parsers.parse('ограничение на рекламу равно {limit}'))
def step_set_adv_limit(context, limit):
    context.request.set_maxadv(limit)


@given(parsers.parse('advert_page_id равен {page_id}'))
def step_set_page_id(context, page_id):
    context.request.set_advert_page(page_id)


@given(parsers.parse('пользователь запрашивает gta с ключом "{key}"'))
def step_ask_gta(context, key):
    context.request.ask_gta(key)


@given(parsers.parse('платформа пользователя "{platform}"'))
def step_set_platform(context, platform):
    context.request.set_maps_platform(platform)


@given('включена персонализация меню')
def step_enable_recommender(context, platform):
    context.request.add_rearr('scheme_Local/Geo/MenuDiscovery/EnableRecommender=true')


# Actions (@when) {{{1


@when('пользователь запускает поиск')
def step_user_starts_search(context):
    context.search_result = context.search(cgi_params(context))


@when('пользователь запускает повторный поиск')
def step_user_starts_repeated_search(context):
    context.search_result = context.search(cgi_params(context))


@when(parsers.parse('пользователь задаёт фильтр "{filter_}"'))
def step_user_sets_filter(context, filter_):
    value = FILTER_MAP.get(filter_, filter_)
    context.request.add_business_filter(value)


@when(parsers.parse('пользователь выбирает ранжирование по {sort_type}'))
def step_user_chooses_sorting_by(context, sort_type):
    SORT_TYPES = {'расстоянию': 'distance', 'рейтингу': 'rank'}
    context.request.set_sort(SORT_TYPES[sort_type])


@when('пользователь задаёт точку ранжирования')
def step_user_sets_ranking_point(context):
    context.request.set_sort_origin(Span.Moscow['ll'])


@when(parsers.parse('пользователь запускает поиск по пермалинку "{oid}"'))
def step_user_starts_search_of_permalink(context, oid):
    context.request.clear_context()
    context.request.set_business_oid(oid)
    context.search_result = context.search(cgi_params(context))


@when(parsers.parse('пользователь запускает поиск по URI "{uri}"'))
def step_user_starts_search_of_uri(context, uri):
    context.request.clear_context()
    context.request.set_uri(uri)
    context.search_result = context.search(cgi_params(context))


@when('пользователь запускает поиск по URI запомненного ответа')
def step_user_starts_search_of_memorized_uri(context):
    uris = []
    report = next(iter(context.memorized_geo_objects.values()))
    if isinstance(report, list):
        for item in report:
            uris.append(item.uri())
    else:
        uris.append(report.uri())
    context.request.clear_context()
    context.request.set_uri(*uris)
    context.search_result = context.search(cgi_params(context))


@when('пользователь запускает поиск с middle_snippets_oid из запомненного ответа')
def step_user_starts_search_with_middle_snippets_oid_from_memorized_answer(context):
    permalink = next(iter(context.memorized_geo_objects.values())).permalinks()[0]
    context.request.set_middle_snippets_oid(permalink)
    context.search_result = context.search(cgi_params(context))


@when('пользователь запускает обратное геокодирование')
def step_user_starts_reverse_geocoding(context):
    context.search_result = context.search(cgi_params(context))


@when('пользователь запрашивает меню')
def step_get_menu(context):
    context.search_result = context.get_menu(cgi_params(context))


# Result checks (@then) {{{1


@then(parsers.parse('имя первой организации должно быть "{company_name}"'))
def step_name_of_first_company_is(context, company_name):
    for response in context.search_result.values():
        assert response.first_doc.is_company()
        response.first_doc.check_name_equals(company_name)


@then(parsers.parse('имя первой организации должно быть либо "{company_name_1}" либо "{company_name_2}"'))
def step_name_of_first_company_is_or(context, company_name_1, company_name_2):
    for response in context.search_result.values():
        assert response.first_doc.is_company()
        response.first_doc.check_name_equals(company_name_1, company_name_2)


@then(
    parsers.parse(
        'имя первой организации должно быть одно из "{company_name_1}", "{company_name_2}", "{company_name_3}"'
    )
)
def step_name_of_first_company_is_or(context, company_name_1, company_name_2, company_name_3):  # noqa: F811
    for response in context.search_result.values():
        assert response.first_doc.is_company()
        response.first_doc.check_name_equals(company_name_1, company_name_2, company_name_3)


@then(parsers.parse('имя всех организаций должно быть в списке "{names}"'))
def step_name_of_first_company_is_or(context, names):  # noqa: F811
    for response in context.search_result.values():
        for geo_object in response.geo_objects:
            assert geo_object.is_company()
            geo_object.check_name_equals(names.split(","))


@then(parsers.parse('первая организация содержит признак "{feature_id}"'))
def step_first_company_has_feature(context, feature_id):
    for response in context.search_result.values():
        geo_object = response.first_doc
        assert geo_object.has_feature(feature_id)


@then(parsers.parse('все организации в ответе содержат признак "{feature_id}"'))
def step_all_companies_in_response_have_feature(context, feature_id):
    for response in context.search_result.values():
        for geo_object in response.geo_objects:
            assert geo_object.has_feature(feature_id)


@then(parsers.parse('у первых "{org_count}" организаций одна из ссылок матчится с "{regex}"'))
def step_first_n_companies_have_links_starting_with(context, org_count, regex):
    org_count = int(org_count)
    for response in context.search_result.values():
        geo_objects = response.geo_objects
        if response.doc_count > org_count:
            geo_objects = response.geo_objects[:org_count]
        for geo_object in geo_objects:
            logging.info(geo_object.name)
            assert geo_object.has_matched_link(regex)


@then(parsers.parse('все организации в ответе содержат признак начинающийся с "{feature_regex}"'))
def step_all_companies_have_features_starting_with(context, feature_regex):
    for response in context.search_result.values():
        for geo_object in response.geo_objects:
            assert geo_object.has_feature_startswith(feature_regex)


@then(parsers.parse('хотя бы одна организация содержит признак начинающийся с "{feature_regex}"'))
def step_any_company_have_features_starting_with(context, feature_regex):
    for response in context.search_result.values():
        assert any(geo_object.has_feature_startswith(feature_regex) for geo_object in response.geo_objects)


@then(parsers.parse('количество организаций в ответе равно "{org_count}"'))
def step_number_of_companies_in_response_is_equal_to(context, org_count):
    for response in context.search_result.values():
        logging.info(len(response.geo_objects))
        assert len(response.geo_objects) == int(org_count)


@then(parsers.parse('первая организация относится к рубрике "{category}"'))
def step_first_company_has_rubric(context, category):
    for response in context.search_result.values():
        assert response.first_doc.has_category(category)


@then(parsers.parse('все организации относится к рубрике "{category}"'))
def step_all_companies_has_rubric(context, category):
    for response in context.search_result.values():
        assert all(o.has_category(category) for o in response.geo_objects)


@then(parsers.parse('какая-то организация относится к рубрике "{category}"'))
def step_all_companies_has_rubric(context, category):  # noqa: F811
    for response in context.search_result.values():
        assert any(o.has_category(category) for o in response.geo_objects)


@then('ответ содержит непустой набор фильтров')
def step_response_has_nonempty_set_of_filters(context):
    for response in context.search_result.values():
        assert response.has_filters()


@then('он получает пустой ответ')
def step_he_receives_empty_response(context):
    for ms, response in context.search_result.items():
        assert len(response.geo_objects) == 0, "Expected no geo object in '{}' response".format(ms)


@then('он получает непустой ответ')
def step_he_receives_nonempty_response(context):
    for ms, response in context.search_result.items():
        assert response.is_non_empty(), "Response '{}' is empty (no geo objects, found == 0 or no bounded_by)".format(
            ms
        )


@then('он получает непустой ответ от ППО')
def step_he_receives_nonempty_response_from_business(context):
    for ms, response in context.search_result.items():
        assert response.is_business_result()
        assert response.is_non_empty(), "Response '{}' is empty (no geo objects, found == 0 or no bounded_by)".format(
            ms
        )


@then('он получает непустой ответ от геокодера')
def step_he_receives_nonempty_response_from_geocoder(context):
    for ms, response in context.search_result.items():
        assert response.is_geocoder_result()
        assert response.is_non_empty(), "Response '{}' is empty (no geo objects, found == 0 or no bounded_by)".format(
            ms
        )


@then(parsers.parse('в списке возможных фильтров есть "{filter_id}"'))
def step_filter_is_listed(context, filter_id):
    for response in context.search_result.values():
        assert response.filter_by_id(filter_id) is not None


@then(parsers.parse('в списке возможных фильтров "{filter_id}" выбран'))
def step_filter_is_selected(context, filter_id):
    for response in context.search_result.values():
        filter_ = response.filter_by_id(filter_id)
        assert filter_.value


@then(parsers.parse('в списке возможных фильтров "{filter_id}" отключен'))
def step_filter_is_disabled(context, filter_id):
    for response in context.search_result.values():
        filter_ = response.filter_by_id(filter_id)
        assert filter_.disabled


@then(parsers.parse('на первом месте находится объект с именем "{name}"'))
def step_first_geoobject_name_is(context, name):
    for response in context.search_result.values():
        response.first_doc.check_name_equals(name)


@then(parsers.parse('на первом месте находится объект с именем либо "{nameA}" либо "{nameB}"'))
def step_first_geoobject_name_is_or(context, nameA, nameB):
    for response in context.search_result.values():
        response.first_doc.check_name_equals(nameA, nameB)


@then(parsers.parse('на первом месте находится один из объектов "{nameA}", "{nameB}", "{nameC}"'))
def step_first_geoobject_name_is_or(context, nameA, nameB, nameC):  # noqa: F811
    for response in context.search_result.values():
        response.first_doc.check_name_equals(nameA, nameB, nameC)


@then(parsers.parse('на первом месте должен быть топоним с адресом "{address}"'))
def step_first_geoobject_is_toponym_having_address(context, address):
    for response in context.search_result.values():
        geo_object = response.first_doc
        assert geo_object.is_toponym(), "Geo object is not a toponym"
        geo_object_address = geo_object.formatted_address()
        expected_address = address
        prefix = 'Россия, '
        if (geo_object_address[: len(prefix)] != prefix) and (expected_address[: len(prefix)] == prefix):
            expected_address = expected_address[len(prefix) :]
        assert geo_object_address == expected_address


@then(parsers.parse('в ответе есть объект с именем "{name}"'))
def step_response_contains_geoobject_with_name(context, name):
    for response in context.search_result.values():
        response.check_contains_item(name)


@then('первая организация работает круглосуточно')
def step_first_company_works_24h(context):
    for response in context.search_result.values():
        assert response.first_doc.works_all_day()


@then('все организации в ответе работают круглосуточно')
def step_all_companies_work_24h(context):
    for response in context.search_result.values():
        for geo_object in response.geo_objects:
            assert geo_object.works_all_day()


@then('в ответе нет временно закрытых организаций')
def step_no_temporary_closed_companies(context):
    for response in context.search_result.values():
        for geo_object in response.geo_objects:
            assert not geo_object.is_temporary_closed()


@then('все организации в ответе содержат расстояние')
def step_all_companies_have_distance(context):
    for response in context.search_result.values():
        assert all(o.distance() for o in response.geo_objects)


@then('организации в ответе упорядочены по расстоянию')
def step_all_companies_are_ordered_by_distance(context):
    for response in context.search_result.values():
        distance_list = [go.distance() for go in response.geo_objects]
        assert sorted(distance_list) == distance_list


@then('организации в ответе упорядочены по интервалам рейтинга (9-10, 8-9, etc.)')
def step_all_companies_are_ordered_by_rating_interals(context):
    for response in context.search_result.values():
        rating_list = list(o.rating_score() for o in response.geo_objects)
        logging.info('%s -> %s' % (sorted(rating_list, reverse=True), rating_list))
        assert sorted(rating_list, reverse=True) == rating_list


@then('ответ содержит единственный объект')
def step_response_has_single_geoobject(context):
    for response in context.search_result.values():
        assert response.is_non_empty(), "Response is empty (no geo objects, found == 0 or no bounded_by)"
        assert response.doc_count == 1


@then(parsers.parse('адрес всех объектов в ответе соответствует регулярному выражению "{regex}"'))
def step_all_geoobject_addresses_match_regex(context, regex):
    response = context.search_result['pb']  # только для pb-репорта
    all_match = True
    for o in response.geo_objects:
        if not re.match(regex, o.formatted_address()):
            print('No match for "{regex}" in {o.description}'.format(**locals()))
            all_match = False
    assert all_match


@then('все объекты в ответе содержат непустой URI')
def step_all_geoobjects_have_nonempty_uris(context):
    for response in context.search_result.values():
        assert all(o.uri() is not None for o in response.geo_objects)


@then('первый объект в ответе совпадает с запомненным')
def step_first_geoobject_matches_memorized_one(context):
    for report, response in context.search_result.items():
        assert response.doc_count > 0
        assert context.memorized_geo_objects[report].name == response.first_doc.name
        assert context.memorized_geo_objects[report].description == response.first_doc.description


@then('все объекты в ответе совпадают с запомненными')
def step_all_geoobjects_matches_memorized(context):
    for report, response in context.search_result.items():
        memorized_response = context.memorized_geo_objects[report]
        assert len(response.geo_objects) == len(memorized_response)
        for i in range(len(response.geo_objects)):
            assert memorized_response[i].name == response.geo_objects[i].name
            assert memorized_response[i].description == response.geo_objects[i].description


@then(parsers.parse('на первом месте находится топоним с типом "{kind}"'))
def step_first_geoobject_is_toponym_of_type(context, kind):
    for response in context.search_result.values():
        geo_object = response.first_doc
        logging.info('geo object name = "%s"' % geo_object.name)
        assert geo_object.is_toponym()
        assert geo_object.kind() == kind


@then(parsers.parse('в ответе есть организация с именем "{name}"'))
def step_response_contains_company_named(context, name):
    for response in context.search_result.values():
        response.check_contains_item(name)


@then('все организации в ответе имеют точность геокодирования "EXACT"')
def step_all_companies_are_exact(context):
    for response in context.search_result.values():
        assert all(o.is_exact_point() for o in response.geo_objects)


@then(
    parsers.parse('все организации в ответе находятся на расстоянии не более {max_distance} метров от точки "{point}"')
)
def step_all_companies_are_close_to_point(context, max_distance, point):
    for response in context.search_result.values():
        WGS_84_RADIUS = 6378137.0
        lon, lat = map(math.radians, map(float, point.split(",")))

        def distance(p):
            return WGS_84_RADIUS * math.acos(
                math.sin(math.radians(p.lat)) * math.sin(lat)
                + math.cos(math.radians(p.lat)) * math.cos(lat) * math.cos(math.radians(p.lon) - lon)
            )

        for obj in response.geo_objects:
            assert distance(obj.point()) < float(max_distance)


@then('cписок похожих организаций не пуст')
def step_similar_list_is_nonempty(context):
    for response in context.search_result.values():
        assert response.first_doc.similar_places_count() > 0


@then('есть похожие с фотками')
def step_there_are_similars_with_photos(context):
    for response in context.search_result.values():
        assert response.first_doc.similar_places_with_photo_count() > 0


@then(parsers.parse('хотя бы одна организация в ответе содержит номер телефона "{telephone_number}"'))
def step_there_is_company_with_telephone_number(context, telephone_number):
    for response in context.search_result.values():
        assert any(geo_object.has_telephone_number(telephone_number) for geo_object in response.geo_objects)


@then(parsers.parse('хотя бы одна организация в ответе содержит урл "{url}"'))
def step_there_is_company_with_url(context, url):
    for response in context.search_result.values():
        assert any(geo_object.has_url(url) for geo_object in response.geo_objects)


@then(parsers.parse('ни одна организация в ответе не содержит урл "{url}"'))
def step_there_is_no_company_with_url(context, url):
    for response in context.search_result.values():
        assert all(not geo_object.has_url(url) for geo_object in response.geo_objects)


@then('в ответе представлены факторы изнанки')
def step_response_contains_iznanka_factors(context):
    proto_response = context.search_result['proto']
    resp_host_factors = json.loads(proto_response.searcher_props['iznanka_host_factors'][0])
    assert any(resp_host_factors['serp_wiz_url_clicks'])
    assert any(resp_host_factors['host_categories'])
    assert any(resp_host_factors['mascot'])

    resp_url_factors = json.loads(proto_response.searcher_props['iznanka_url_factors'][0])
    assert any(resp_url_factors['address_snippet_ctr'])


@then(parsers.parse('в ответе представлены факторы изнанки для урла "{url}"'))
def step_response_contains_iznanka_factors_for_url(context, url):
    proto_response = context.search_result['proto']
    proto_geo_object = filter(lambda geo_object: geo_object.has_url(url), proto_response.geo_objects)[0]
    doc_factors = json.loads(proto_geo_object.gta['iznanka_factors'][0])

    assert any(doc_factors['id_serp_wiz_url_clicks'])
    assert any(doc_factors['id_wiz_site_button_ctr'])
    assert any(doc_factors['url_id_serp_wiz_url_clicks'])


@then(parsers.parse('хотя бы один документ из top{top_size} является товаром'))
def step_any_topn_geoobject_is_goods(context, top_size):
    proto_response = context.search_result['pb']
    assert any(obj.is_goods() for obj in proto_response.geo_objects)


@then(parsers.parse('все организации в ответе содержат товар с тегом "{tag_name}"'))
def step_all_geobjects_contain_goods_with_tag(context, tag_name):
    assert all(
        any(tag == tag_name for good in obj.get_goods().goods for tag in good.tag)
        for obj in context.search_result['pb'].geo_objects
    )


@then(parsers.parse('хотя бы один документ из top{top_size} имеет непустой сниппет "{snippet_name}"'))
def step_any_topn_geoobject_has_nonempty_snippet(context, top_size, snippet_name):
    proto_response = context.search_result['proto']
    assert any(bool(obj.get_snippet(snippet_name)) for obj in proto_response.geo_objects)


@then(parsers.parse('ни один документ из top{top_size} не содержит сниппет "{snippet_name}"'))
def step_any_topn_geoobject_has_no_snippet(context, top_size, snippet_name):
    proto_response = context.search_result['proto']
    assert not any(bool(obj.get_snippet(snippet_name)) for obj in proto_response.geo_objects)


@then(parsers.parse('каждый сниппет "{snippet_name}" содержит "{match}"'))
def step_every_snippet_contains_match(context, snippet_name, match):
    objs = context.search_result['proto'].geo_objects
    assert all((not snippet) or (match in snippet) for snippet in [obj.get_snippet(snippet_name) for obj in objs])


@then(parsers.parse('{N}-й объект в ответе имеет непустой сниппет "{snippet_name}"'))
def step_nth_geoobject_has_nonempty_snippet(context, N, snippet_name):
    proto_response = context.search_result['proto']
    assert proto_response.geo_objects[int(N) - 1].get_snippet(snippet_name)


@then(parsers.parse('в ответе есть непустой searcher prop "{prop_name}"'))
def step_response_has_searcher_prop(context, prop_name):
    proto_response = context.search_result['proto']
    assert proto_response.searcher_props[prop_name][0]


@then(parsers.parse('в выдаче содержится организация с пермалинком "{permalink}"'))
def step_response_contains_permalink(context, permalink):
    for response in context.search_result.values():
        assert permalink in itertools.chain(*[o.permalinks() for o in response.geo_objects])


@then('все объекты в ответе являются коллекциями')
def step_all_geoobjects_are_collections(context):
    pb_response = context.search_result['pb']
    assert all(o.is_collection() for o in pb_response.geo_objects)


@then(parsers.parse('{N}-й объект в ответе является коллекцией'))
def step_nth_geoobject_is_collection(context, N):
    pb_response = context.search_result['pb']
    assert pb_response.geo_objects[int(N) - 1].is_collection()


@then(parsers.parse('у всех документов есть gta с ключом "{key}"'))
def step_all_documents_have_gta(context, key):
    pb_response = context.search_result['pb']
    assert all(bool(o.get_gta(key)) for o in pb_response.geo_objects)


@then(parsers.parse('у всех коллекций gta с ключом "{key}" равно "{value}"'))
def step_all_collections_have_gta(context, key, value):
    pb_response = context.search_result['pb']
    assert all(o.get_gta(key) == value for o in pb_response.geo_objects if o.is_collection())


@then(parsers.parse('хотя бы у одного документа из top{top_size} gta с ключом "{key}" не равно "{value}"'))
def step_any_documents_have_gta_not_equal_value(context, top_size, key, value):
    pb_response = context.search_result['pb']
    assert any(o.get_gta(key) != value for o in pb_response.geo_objects)


@then(parsers.parse('на первом месте находится станция метро с именем "{name}"'))
def step_metro_station_is_first(context, name):
    for response in context.search_result.values():
        geo_object = response.first_doc
        if geo_object.is_company():
            assert geo_object.has_category('Станция метро'), 'Нет категории "Станция метро"'
            geo_object.check_name_contains(name)
        if geo_object.is_toponym():
            geo_object.check_name_contains(name)


@then(parsers.parse('первые {count} организаций - геопродукты'))
def step_first_orgs_is_adv(context, count):
    count = int(count)
    for response in context.search_result.values():
        assert response.doc_count >= count
        for i in range(count):
            assert response.geo_objects[i].is_advert()


@then(parsers.parse('хотя бы одна организация в ответе - геопродукт'))
def step_has_adv_orgs(context):
    for response in context.search_result.values():
        assert any(geo_object.is_advert() for geo_object in response.geo_objects)


@then(parsers.parse('геопродукт в ответе отсутствует'))
def step_has_no_adv_orgs(context):
    for response in context.search_result.values():
        assert all(not geo_object.is_advert() for geo_object in response.geo_objects)


@then(parsers.parse('буст геопродуктовых организаций в ответе отсутствует'))
def step_has_no_adv_boost_orgs(context):
    pb_response = context.search_result['pb']
    assert all(not geo_object.is_boosted_advert() for geo_object in pb_response.geo_objects)


@then(parsers.parse('в ответе есть буст геопродуктовых организаций'))
def step_has_adv_boost_orgs(context):
    pb_response = context.search_result['pb']
    assert any(geo_object.is_boosted_advert() for geo_object in pb_response.geo_objects)


@then(parsers.parse('после органики есть буст геопродукта'))
def step_has_boost_geoproduct_after_organic(context):
    for response in context.search_result.values():
        first_organic_position = next((i for i, obj in enumerate(response.geo_objects) if not obj.is_advert()), None)
        assert first_organic_position is not None
        assert any(obj.is_boosted_advert() for obj in response.geo_objects[first_organic_position + 1 :])


@then(parsers.parse('cписок конкурентов {emptiness}'))
def step_nonempty_bottom_inject_list(context, emptiness):
    for response in context.search_result.values():
        has_competitors = False
        for geo_object in response.geo_objects:
            if geo_object.is_injected_as_competitor():
                has_competitors = True
                break
        assert emptiness == "не пуст" and has_competitors or emptiness == "пуст" and not has_competitors


@then(parsers.parse('ровно у одной организации в ответе есть сниппет конкурентов уровня выдачи'))
def step_has_one_serp_competitors_snippet(context):
    assert sum(1 for obj in context.search_result['pb'].geo_objects if obj.has_serp_competitors_snippet()) == 1


@then(parsers.parse('у всех организаций в ответе есть сниппет конкурентов уровня организации'))
def step_has_org_competitors_snippet(context):
    assert all(obj.has_org_competitors_snippet() for obj in context.search_result['pb'].geo_objects)


@then(parsers.parse('в ответе есть команда "{command}"'))
def has_command_in_answer(context, command):
    response = context.search_result['pb']
    assert len(response.commands().command) != 0
    assert response.commands().command[0] == command, '%s не равно %s' % (response.commands().command, command)


@then(parsers.parse('организация "{name}" в ответе {contains} рекламу'))
def step_org_has_advert(context, name, contains):
    for geo_object in context.search_result['pb'].geo_objects:
        if geo_object.name == name:
            assert (
                contains == "содержит"
                and geo_object.has_advert()
                or contains == "не содержит"
                and not geo_object.has_advert()
            )
            return
    assert False


def _find_banner_in_answer(context):
    response = context.search_result['pb']
    banner_found = False
    banner_value = ''
    metadata = response.experimental_metadata()
    if metadata:
        for item in metadata.experimental_storage.item:
            if item.key == 'banner/1.x':
                banner_found = True
                banner_value = item.value
    return banner_found, banner_value


@then(parsers.parse('в ответе есть баннер "{banner}"'))
def has_banner_in_answer(context, banner):
    banner_found, banner_value = _find_banner_in_answer(context)
    assert banner_found
    assert banner_value.find(banner) != -1


@then(parsers.parse('в ответе нет баннера "{banner}"'))
def has_no_banner_in_answer(context, banner):
    banner_found, banner_value = _find_banner_in_answer(context)
    assert not banner_found or banner_value.find(banner) == -1


@then(parsers.parse('первый объект в выдаче содержит полигональную геометрию'))
def step_first_geoobject_has_polygonal_geometry(context, name):
    response = context.search_result['pb']  # только для pb-репорта
    contains = False
    for geometry in response.first_doc.message.geometry:
        if geometry.HasField('polygon'):
            contains = True
            break
    assert contains


@then(parsers.parse('ни один документ из top{top_size} не содержит полигональную геометрию'))
def step_any_topn_geoobject_has_no_polygonal_geometry(context, top_size):
    response = context.search_result['pb']  # только для pb-репорта
    not_contains = True
    for geo_object in response.geo_objects:
        for geometry in geo_object.message.geometry:
            if geometry.HasField('polygon'):
                not_contains = False
                break
    assert not_contains


@then('в ответе есть пункты меню')
def step_has_menu_items(context):
    assert context.search_result.is_not_empty()


@then(parsers.parse('в ответе bounded_by "{bb}"'))
def step_check_bounded_by(context, bb):
    print('expected', bb)
    print('real', context.search_result.bounded_by())
    assert context.search_result.bounded_by() == bb
