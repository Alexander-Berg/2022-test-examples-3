from faker import Faker

from travel.avia.flight_status_fetcher.tests.test_sources.test_dme.factories import factory_xml_flight


def test_get_flights_by_nat(importer):
    faker = Faker()
    flights = list()

    flights.append(factory_xml_flight(faker, faker.pystr()))
    flights.append(factory_xml_flight(faker, faker.pystr(), NAT='PAX', FL_NUM_PUB='SU 100'))
    flights.append(factory_xml_flight(faker, faker.pystr(), NAT='CGO'))
    flights.append(factory_xml_flight(faker, faker.pystr(), FL_NUM_PUB='SU 100'))
    flights.append(factory_xml_flight(faker, faker.pystr(), NAT='CGO', FL_NUM_PUB='SU 100'))

    flights = importer._filter_flights(flights)
    assert len(list(flights)) == 2


def test_filter_with_many_filters(importer):
    sequence = [1, 2, 3, 4, 5]
    filters = [
        lambda i: i != 1,
        lambda i: i != 2,
    ]

    assert list(importer._filter_flights(sequence, filters=filters)) == [3, 4, 5]
