import json

from library.python import resource


def get_schema(name):
    return json.loads(resource.find(name.encode()))


user_app_car_list = get_schema("/schemas/user_app_car_list.json")
user_app_radar_areas = get_schema("/schemas/user_app_radar_areas.json")
user_app_user_session = get_schema("/schemas/user_app_user_session.json")
user_app_sessions_history = get_schema("/schemas/user_app_sessions_history.json")
user_app_offers_create = get_schema("/schemas/user_app_offers_create.json")
user_app_drop_areas = get_schema("/schemas/user_app_drop_areas.json")
user_app_areas_operation_info = get_schema("/schemas/user_app_areas_operation_info.json")
support_api_chat_history_unread = get_schema("/schemas/support_api_chat_history_unread.json")
support_api_chats_list = get_schema("/schemas/support_api_chats_list.json")
