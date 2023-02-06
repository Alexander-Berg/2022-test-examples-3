# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils import translation
from hamcrest import has_entries

from common.models.transport import TransportType
from common.tester.factories import (
    create_rthread_segment, create_transport_model, create_transport_subtype, create_thread,
    create_rtstation, create_station
)
from travel.rasp.morda_backend.morda_backend.serialization.segment_transport import (
    TransportSubTypeSchema, build_transport
)


@pytest.mark.dbuser
def test_get_title_color():
    subtype_defaults = dict(t_type='train', title_ru='Заголовок подтипа')

    # не возвращает поле titleColor, если у подтипа не задан цвет
    result = TransportSubTypeSchema().dump(
        create_transport_subtype(code='subtype_1', color=None, **subtype_defaults)
    )

    assert not result.errors
    assert 'titleColor' not in result.data

    # возвращает значение из поля color в поле titleColor
    color = '#ffffff'
    result = TransportSubTypeSchema().dump(
        create_transport_subtype(code='subtype_2', color={'color': color}, **subtype_defaults)
    )

    assert not result.errors
    assert result.data['titleColor'] == color


@pytest.mark.dbuser
def test_build_transport():
    """
    Тестируем генерацию структуры, описывающей транспорт
    """
    lang = 'ru'
    segment = create_rthread_segment(
        t_type=TransportType.get_train_type(),
        t_model=create_transport_model(),
        thread=create_thread(t_subtype=create_transport_subtype(**{'t_type_id': 10, 'title_{}'.format(lang): 'Test'}))
    )
    with translation.override(lang):
        transport = build_transport(segment)
    assert has_entries(
        code=segment.t_type.code,
        title=segment.t_type.L_title(lang=lang),
        model=has_entries(title=segment.t_model.L_title(lang=lang)),
        subtype=has_entries(
            id=segment.thread.t_subtype.id,
            code=segment.thread.t_subtype.code,
            title=segment.thread.t_subtype.L_title(lang=lang)
        )
    ).matches(transport)


@pytest.mark.dbuser
def test_build_transport_rtstation_model():
    """
    Тестируем генерацию структуры, описывающей транспорт
    """
    lang = 'ru'
    thread = create_thread(t_subtype=create_transport_subtype(**{'t_type_id': 10, 'title_{}'.format(lang): 'Test'}))
    rtstation_from = create_rtstation(thread=thread, station=create_station(),
                                      departure_t_model=create_transport_model(title='B-999 Transatlantic'))
    segment = create_rthread_segment(
        t_type=TransportType.get_train_type(),
        t_model=create_transport_model(),
        thread=thread,
        rtstation_from=rtstation_from
    )
    with translation.override(lang):
        transport = build_transport(segment)
    assert has_entries(
        code=segment.t_type.code,
        title=segment.t_type.L_title(lang=lang),
        model=has_entries(title='B-999 Transatlantic'),
        subtype=has_entries(
            id=segment.thread.t_subtype.id,
            code=segment.thread.t_subtype.code,
            title=segment.thread.t_subtype.L_title(lang=lang)
        )
    ).matches(transport)


@pytest.mark.dbuser
def test_build_transport_no_model_no_subtype():
    """
    Должен вернуть словарь с model=None, subtype=None
    """
    segment = create_rthread_segment(
        t_type=TransportType.get_train_type(),
        t_model=None,
        thread=create_thread(t_subtype=None)
    )
    transport = build_transport(segment)
    assert has_entries(
        code=segment.t_type.code,
        title=segment.t_type.L_title(),
    ).matches(transport)
    assert 'model' not in transport
    assert 'subtype' not in transport
