from hamcrest import assert_that, contains, has_properties

from alice.megamind.protos.common.frame_pb2 import TSemanticFrame


def assert_requests_slot(response, frame, slot_name, accepted_types, contains_sensitive_data, analytics_info=None):
    analytics_info = analytics_info or {}

    assert_that(
        response,
        has_properties({
            'ResponseBody': has_properties({
                'SemanticFrame': has_properties({
                    'Name': frame,
                    'Slots': contains(has_properties({
                        'Name': slot_name,
                        'AcceptedTypes': contains(*accepted_types),
                        'IsRequested': True,
                    }))
                }),
                'Layout': has_properties({
                    'ContainsSensitiveData': contains_sensitive_data,
                }),
                'AnalyticsInfo': has_properties({
                    **analytics_info
                })
            })
        })
    )


def set_slot(frame, slot_name, slot_type, slot_value):
    slot = frame.Slots.add()
    slot.Name = slot_name
    slot.Type = slot_type
    slot.Value = slot_value
    return frame


def create_frame(frame_name, slots=None):
    frame = TSemanticFrame()
    frame.Name = frame_name
    slots = slots or []
    for slot_name, slot_type, slot_value in slots:
        set_slot(frame, slot_name, slot_type, slot_value)
    return frame
