from psycopg2 import extras
import pytest
import requests

TEST_SEGMENT = 'segment-test'


def save_segment_export_by_id(segment_id, export_id, postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute(f"""
                INSERT INTO api_segment_exports (segment_id, type, export_keyword_id, export_segment_id, id) VALUES
                    ('{segment_id}', 'HEURISTIC', '557', '0', '{export_id}');
            """)


def get_segment_export_by_id(export_id, postgres):
    with postgres.connect() as connection, connection.cursor(cursor_factory=extras.RealDictCursor) as cursor:
        cursor.execute(f"""
            SELECT * FROM api_segment_exports WHERE id='{export_id}';
        """)
        return cursor.fetchone()


@pytest.mark.parametrize("export_crypta_id", ["false", "true"])
def test_add_segment_export(api, postgres, init_db, export_crypta_id):
    segment_id = TEST_SEGMENT
    response = requests.post(f"http://localhost:{api.port}/lab/segment/export",
                             params={'segmentId': segment_id,
                                     'type': 'DEFAULT',
                                     'exportKeywordId': '557',
                                     'exportCryptaId': export_crypta_id
                                     }).json()

    export_id = response['id']
    export = get_segment_export_by_id(export_id, postgres)

    export['id'] = "export-id"

    return export


def test_get_segment_export(api, postgres, init_db):
    export_id = 'export-test0001'
    save_segment_export_by_id(TEST_SEGMENT, export_id, postgres)

    return requests.get(f"http://localhost:{api.port}/lab/segment/export/{export_id}").json()
