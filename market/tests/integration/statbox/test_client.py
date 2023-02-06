# TODO: uncomment all tests after granting access to Statbox for all TeamCity agents
# def test_client_can_create_report(statbox_client, report, report_settings):
#     assert not statbox_client.report_exists(report)
#     statbox_client.create(report, report_settings, "create_report")
#     assert statbox_client.report_exists(report)
#
#
# def test_client_can_delete_report(statbox_client, report, report_settings):
#     statbox_client.create(report, report_settings, "delete_report")
#     statbox_client.delete(report)
#     assert not statbox_client.report_exists(report)


# TODO: uncomment after resolving https://st.yandex-team.ru/STATFACE-5276
# def test_client_can_truncate_uploaded_data(statbox_client, report, report_settings, report_data):
#     statbox_client.create(report, report_settings, "truncate_uploaded_data")
#     statbox_client.upload(report, report_data, 'd')
#     statbox_client.truncate(report, "d")
#     assert statbox_client.report_exists(report)
#     assert not statbox_client.data_exists(report, "2018-01-02", "d")


# def test_client_can_upload_data(statbox_client, report, report_settings, report_data):
#     statbox_client.create(report, report_settings, "upload_data")
#     assert not statbox_client.data_exists(report, "2018-01-01", "d")
#     statbox_client.upload(report, report_data, 'd')
#     assert statbox_client.data_exists(report, "2018-01-01", "d")
