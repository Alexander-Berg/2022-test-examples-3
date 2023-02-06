from travel.proto.dicts.rasp.settlement_pb2 import TSettlement
from travel.proto.dicts.rasp.station_pb2 import TStation
from travel.proto.dicts.rasp.timezone_pb2 import TTimeZone


def create_station_proto(s_id, country_id, title, timezone_id, settlement_id, lon, lat, ttype, majority, type_choices):
    station = TStation()
    station.Id = s_id
    station.CountryId = country_id
    station.TitleDefault = title
    station.TimeZoneId = timezone_id
    station.SettlementId = settlement_id
    station.Longitude = lon
    station.Latitude = lat
    station.TransportType = ttype
    station.Majority = majority
    station.TypeChoices = type_choices
    return station


def create_settlement_proto(s_id, title, lon, lat, majority):
    settlement = TSettlement()
    settlement.Id = s_id
    settlement.TitleDefault = title
    settlement.Longitude = lon
    settlement.Latitude = lat
    settlement.Majority = majority
    return settlement


def create_timezone_proto(t_id, code):
    timezone = TTimeZone()
    timezone.Id = t_id
    timezone.Code = code
    return timezone
