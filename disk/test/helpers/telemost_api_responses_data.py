# -*- coding: utf-8 -*-
class CreatedConfData:
    DEFAULT = {'uri': 'https://telemost.yandex.ru/j/12345678901234',
               'ws_uri': 'https://push.yandex.ru/ws/socket-wo-auth',
               'room_id': '17',
               'safe_room_id': '4523540f1504cd17100c4835e85b7eefd49911580f8efff0599a8f283be6b9e3',
               'peer_id': '1234567',
               'peer_token': 'dsfsffds',
               'media_session_id': 'sdfrw3',
               'client_configuration': {'room_config': {'forceOpenSctp': True,
                                                        'openBridgeChannel': 'websocket'},
                                        'connection_config': {'turbo_acceleration': 107}}}

    WO_USER_ID = {'uri': 'https://telemost.yandex.ru/j/12345678901234',
                  'ws_uri': 'https://push.yandex.ru/ws/socket-wo-auth',
                  'room_id': '17'}

    WO_CLIENT_CONFIG = {'uri': 'https://telemost.yandex.ru/j/12345678901234',
                        'ws_uri': 'https://push.yandex.ru/ws/socket-wo-auth',
                        'room_id': '17',
                        'peer_id': '1234567'}

    BAD_REQUEST = {'error': {'name': 'validate',
                             'message': 'failed to process'}}

    GONE = {'error': {'name': 'ConferenceLinkExpired',
                      'message': 'link expired'}}

    NOT_COME = {'error': {'name': 'ConferenceLinkNotCome',
                          'message': '{\"start_event_time\": 7826382342}'}}

    STREAM_NOT_STARTED = {'error': {'name': 'StreamNotStarted',
                                    'message': 'The stream for the specified broadcast is not running.'}}

    FORBIDDEN_TO_PRIVATE = {'error': {'name': 'ForbiddenAccessToPrivateConference',
                                      'message': 'access to the private conference is forbidden'}}

    NOT_FOUND = {'error': {'name': 'not-found',
                           'message': 'not found'}}

    NOT_AVAILABLE = {'error': {'name': 'YaTeamConferenceNotAvailable',
                               'message': 'not available'}}

    WO_ERROR_NAME = {'message': 'nginx error'}


class ConfShortInfoData:
    DEFAULT = {'uri': 'https://telemost.yandex.ru/j/12345678901234',
               'room_id': '17',
               'safe_room_id': '4523540f1504cd17100c4835e85b7eefd49911580f8efff0599a8f283be6b9e3',}

    GONE = {'error': {'name': 'ConferenceLinkExpired',
                      'message': 'link expired'}}

    NOT_FOUND = {'error': {'name': 'not-found',
                           'message': 'not found'}}

    FAILED_TO_ACCESS_YANDEX_TEAM_CONF = {'error': {'name': 'YaTeamTokenAccessDenied',
                                                   'message': 'link expired'}}

    FORBIDDEN_YANDEX_TEAM_CONF = {'error': {'name': 'YaTeamConferenceNotAvailable',
                                            'message': 'this is yandex-team conferece with staff_only=True, you are not welcome here'}}


class UserInfoData:
    DEFAULT = {'uid': '12345',
               'display_name': 'fluffy raccoon',
               'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/0/1-2/islands-777',
               'is_yandex_staff': True}

    WO_AVATAR_URL = {'uid': '12345',
                     'display_name': 'fluffy raccoon',
                     'is_yandex_staff': True}

    WO_UID = {'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/0/1-2/islands-777',
              'display_name': 'fluffy raccoon',
              'is_yandex_staff': False}


class PeersData:
    DEFAULT = {'items': [{'uid': '12345',
                          'display_name': 'fluffy raccoon',
                          'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/0/1-2/islands-777',
                          'peer_id': '777'},
                         {'uid': '12346',
                          'display_name': 'fluffy foxy',
                          'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/0/1-3/islands-777',
                          'peer_id': '555'},
                         {'uid': '12347',
                          'display_name': 'fluffy homyak',
                          'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/0/1-4/islands-777',
                          'peer_id': '999'}]}

    WO_OPTIONAL = {'items': [{'display_name': 'fluffy raccoon',
                              'peer_id': '777'},
                             {'display_name': 'fluffy foxy',
                              'peer_id': '555'}]}

    GONE = {'error': {'name': 'ConferenceLinkExpired',
                      'message': 'link expired'}}

    NOT_FOUND = {'error': {'name': 'not-found',
                           'message': 'not found'}}


class AuthURIData:
    DEFAULT = {'uri': 'https://telemost.yandex.ru/j/12345678901234'}

    BAD_REQUEST = {'error': {'name': 'validate',
                             'message':'failed to process'}}

    GONE = {'error': {'name': 'ConferenceLinkExpired',
                      'message': 'link expired'}}

    NOT_FOUND = {'error': {'name': 'not-found',
                           'message': 'not found'}}


class LeaveRoomData:
    DEFAULT = {}

    CONFLICT = {'error': {'name': 'SessionAlreadyDisconnected',
                          'message': 'Session has been already disconnected'}}

    NOT_FOUND = {'error': {'name': 'PeerNotFound',
                           'message': 'not found'}}


class BroadcastData:
    DEFAULT = {'broadcast_uri': 'https://telemost.yandex.ru/live/f20fe8a49ceb4c888d4465b8c7541a50',
               'broadcast_chat_path': '00000000-0000-0000-0000-000000000000'}
    # TODO: Здесь должен отдаваться путь до чата, но чаты делаем позднее
    #       Поэтому Телемост пока отдает только default guid
    #       После надо будет поправить тест


class StreamData:
    START = {'owner_uid': '123',
             'started_at': 7826382342,
             'ugc_live_slug': 'v8n88m9-qSXo'}

    STOP = {'owner_uid': '123',
            'started_at': 7826382342,
            'stopped_at': 7826382345}


class StreamConnectionData:
    DEFAULT = {'stream_uri': 'https://vh.test.yandex.ru/live/player/vwloq97pFXj0'}
