# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import mock
from hamcrest import assert_that, contains, contains_inanyorder, has_entries, has_properties, has_property, contains
from six.moves.urllib.parse import urljoin

import travel.rasp.library.python.api_clients.chelyabinsk_bus.client as client_module


BUSES_RESPONSE = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
	<soap:Body>
		<m:GetTheBusResponse xmlns:m="http://www.YaTickets.ru">
			<m:return xmlns:xs="http://www.w3.org/2001/XMLSchema"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xsi:type="m:GetTheBus">
				<m:TheBusLines>
					<m:Id>f34d4f0a-f7df-11e0-876b-f6a059620446</m:Id>
					<m:Name>Златоуст АВ</m:Name>
					<m:BanOnlineSales>false</m:BanOnlineSales>
					<m:latitude/>
					<m:longitude/>
					<m:thecity>Златоуст</m:thecity>
				</m:TheBusLines>
				<m:TheBusLines>
					<m:Id>d778c058-1805-11e4-9ac9-001e674ad315</m:Id>
					<m:Name>Магнитогорск АС (У памятника Паровозу)</m:Name>
					<m:BanOnlineSales>false</m:BanOnlineSales>
					<m:latitude/>
					<m:longitude/>
					<m:thecity>Магнитогорск</m:thecity>
				</m:TheBusLines>
			</m:return>
		</m:GetTheBusResponse>
	</soap:Body>
</soap:Envelope>
"""


THREADS_RESPONSE = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
	<soap:Body>
		<m:GetFlightsToBusStationResponse xmlns:m="http://www.YaTickets.ru">
			<m:return xmlns:xs="http://www.w3.org/2001/XMLSchema"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xsi:type="m:GetFlightsToBusStation">
		<m:TheFlightsList>
					<m:FlightId>9fb8cdb7-4afe-11ec-9bea-0025b3ae173b</m:FlightId>
					<m:DateTimePlan>2021-12-02T04:30:00</m:DateTimePlan>
					<m:NPlatform>0</m:NPlatform>
					<m:RoutingNumber>549/</m:RoutingNumber>
					<m:TrackList>
						<m:Track>Нязепетровск - Челябинск Синегорье  549/</m:Track>
						<m:Point>Нязепетровск</m:Point>
						<m:Denipoti>0</m:Denipoti>
						<m:ArrivalTime>0001-01-01T04:20:00</m:ArrivalTime>
						<m:Dlitelnosti>10</m:Dlitelnosti>
						<m:DepartureTime>0001-01-01T04:30:00</m:DepartureTime>
						<m:DistanceFromTheFirstStop>0</m:DistanceFromTheFirstStop>
						<m:OffsetDays>0</m:OffsetDays>
						<m:TicketPrice></m:TicketPrice>
						<m:TicketPriceBaggage></m:TicketPriceBaggage>
						<m:BanTheSaleToStopUsingTheInternet>false</m:BanTheSaleToStopUsingTheInternet>
						<m:ACBPDP>false</m:ACBPDP>
						<m:SpecifyPersonalData>true</m:SpecifyPersonalData>
						<m:BanSalesFromTheSpot>false</m:BanSalesFromTheSpot>
					</m:TrackList>
					<m:TrackList>
						<m:Track>Нязепетровск - Челябинск Синегорье  549/</m:Track>
						<m:Point>Челябинск Синегорье</m:Point>
						<m:Denipoti>0</m:Denipoti>
						<m:ArrivalTime>0001-01-01T09:15:00</m:ArrivalTime>
						<m:Dlitelnosti>0</m:Dlitelnosti>
						<m:DepartureTime>0001-01-01T09:15:00</m:DepartureTime>
						<m:DistanceFromTheFirstStop>231.1</m:DistanceFromTheFirstStop>
						<m:OffsetDays>0</m:OffsetDays>
						<m:TicketPrice>650</m:TicketPrice>
						<m:TicketPriceBaggage>325</m:TicketPriceBaggage>
						<m:BanTheSaleToStopUsingTheInternet>false</m:BanTheSaleToStopUsingTheInternet>
						<m:ACBPDP>false</m:ACBPDP>
						<m:SpecifyPersonalData>true</m:SpecifyPersonalData>
						<m:BanSalesFromTheSpot>false</m:BanSalesFromTheSpot>
					</m:TrackList>
					<m:AutotransportCompany>Нязепетровское АТП</m:AutotransportCompany>
					<m:ModelBus>Mersedes benz M-19 В 459 УЕ 174 РУ</m:ModelBus>
					<m:FreeSeats>18</m:FreeSeats>
					<m:CourtesyPlaces>19</m:CourtesyPlaces>
					<m:RegularityMovement>Еженедельно: пн вт ср чт пт</m:RegularityMovement>
					<m:BanOnlineSales>false</m:BanOnlineSales>
					<m:BanOnSaleChildrenTickets>false</m:BanOnSaleChildrenTickets>
					<m:TransitSeatsInstalled>false</m:TransitSeatsInstalled>
					<m:CharterFlights>false</m:CharterFlights>
					<m:Flight>02.12.2021 4:30:00  Нязепетровск - Челябинск Синегорье  549/</m:Flight>
					<m:AdditionalFlight>false</m:AdditionalFlight>
					<m:Status>1</m:Status>
				</m:TheFlightsList>
			</m:return>
		</m:GetFlightsToBusStationResponse>
	</soap:Body>
</soap:Envelope>
"""


def get_client(response):
    url = urljoin(client_module.CHELYABINSK_BUS_API_HOST, client_module.CHELYABINSK_BUS_API_URL)
    httpretty.register_uri(httpretty.POST, url, body=response)

    client = client_module.ChelyabinskBusClient(login='login', password='password')
    return client


@httpretty.activate
@mock.patch.object(client_module, 'CHELYABINSK_BUS_API_URL', 'bus')
def test_chelyabinsk_buses():
    client = get_client(BUSES_RESPONSE)
    result = client.get_buses()

    assert_that(httpretty.last_request(), has_properties({
        'body': '''<?xml version='1.0' encoding='utf-8'?>
        <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
        <soap-env:Body>
        <ns0:GetTheBus xmlns:ns0="http://www.YaTickets.ru"/>
        </soap-env:Body>
        </soap-env:Envelope>'''
    }))

    assert_that(result, contains(
        {'id': 'f34d4f0a-f7df-11e0-876b-f6a059620446', 'name': 'Златоуст АВ'},
        {'id': 'd778c058-1805-11e4-9ac9-001e674ad315', 'name': 'Магнитогорск АС (У памятника Паровозу)'}
    ))


@httpretty.activate
@mock.patch.object(client_module, 'CHELYABINSK_BUS_API_URL', 'bus')
def test_chelyabinsk_threads():
    client = get_client(THREADS_RESPONSE)
    result = client.get_threads(1, '2021-12-12')

    assert_that(httpretty.last_request(), has_properties({
        'body': '''<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
        <soap-env:Body>
        <ns0:GetFlightsToBusStation xmlns:ns0="http://www.YaTickets.ru">
        <ns0:BusStationId>1</ns0:BusStationId>
        <ns0:DepartureData>2021-12-12</ns0:DepartureData>
        <ns0:Active>true</ns0:Active>
        <ns0:AdditionalFlight>false</ns0:AdditionalFlight>
        </ns0:GetFlightsToBusStation>
        </soap-env:Body>
        </soap-env:Envelope>'''
    }))

    assert_that(result, contains(
        {
            'TrackList': [
                {
                    'TicketPrice': None,
                    'DepartureTime': '0001-01-01T04:30:00',
                    'OffsetDays': '0',
                    'Point': 'Нязепетровск',
                    'DistanceFromTheFirstStop': '0',
                    'ArrivalTime': '0001-01-01T04:20:00'
                },
                {
                    'TicketPrice': '650',
                    'DepartureTime': '0001-01-01T09:15:00',
                    'OffsetDays': '0',
                    'Point': 'Челябинск Синегорье',
                    'DistanceFromTheFirstStop': '231.1',
                    'ArrivalTime': '0001-01-01T09:15:00'
                }
            ],
            'RoutingNumber': '549/',
            'ModelBus': 'Mersedes benz M-19 В 459 УЕ 174 РУ',
            'Flight': '02.12.2021 4:30:00  Нязепетровск - Челябинск Синегорье  549/',
            'AutotransportCompany': 'Нязепетровское АТП'
        }
    ))
