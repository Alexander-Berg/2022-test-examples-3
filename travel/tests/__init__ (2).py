from travel.avia.flight_status_fetcher.utils.import_helper import ImportHelper


def basic_status_asserts(
    importer: ImportHelper,
    statuses: list,
    success_excepted: int,
    company_not_found: int = 0,
    data_size: int = None,
):
    if data_size is not None:
        assert importer._last_run_statistics['datasize'] == data_size
    assert importer._last_run_statistics['company_not_found'] == company_not_found
    assert importer._last_run_statistics['success'] == success_excepted
    assert importer._last_run_statistics['failure'] == 0

    assert len(statuses) == success_excepted
