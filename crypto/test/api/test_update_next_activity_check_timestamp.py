import requests


def save_segment(segment_id, postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute(f"""
            INSERT INTO api_segments (id, type, scope, ticket, author, parent_id) VALUES
                ('{segment_id}', 'CRYPTA_SEGMENT', 'INTERNAL', 'CRYPTA-123', 'terekhinam', 'root');
        """)


def save_export(export_id, segment_id, postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute(f"""
            INSERT INTO api_segment_exports (id, segment_id, type, export_keyword_id, export_segment_id, next_activity_check_timestamp) VALUES
                ('{export_id}', '{segment_id}', 'HEURISTIC', '557', '12000001', 1657547965);
        """)


def test_update_next_activity_check_timestamp(api, postgres, init_db):
    segment_id = 'segment-1'
    export_id = 'export-1'

    save_segment(segment_id, postgres)
    save_export(export_id, segment_id, postgres)

    return requests.put(
        f"http://localhost:{api.port}/lab/segment/export/{export_id}/update_next_activity_check_ts?timestamp=1657549375",
    ).json()
