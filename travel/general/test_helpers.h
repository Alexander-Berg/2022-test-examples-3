#pragma once

#include <travel/rasp/route-search-api/rasp_database.h>
#include <travel/rasp/route-search-api/helpers.h>

#include <library/cpp/timezone_conversion/civil.h>
#include <util/generic/vector.h>

class TRTStationBuilder {
    NRasp::object_id_t Id;
    NRasp::object_id_t StationId;
    NRasp::object_id_t ThreadId;
    bool HasArrival;
    i64 Arrival;
    bool HasDeparture;
    i64 Departure;
    bool IsSearchableFrom = true;
    bool IsSearchableTo = true;
    ui64 TimezoneId = 1;

public:
    TThreadStation Build() const;
    TRTStationBuilder& SetId(NRasp::object_id_t id);
    TRTStationBuilder& SetStationId(NRasp::object_id_t id);
    TRTStationBuilder& SetThreadId(NRasp::object_id_t id);
    TRTStationBuilder& SetHasArrival(bool has_arrival);
    TRTStationBuilder& SetArrival(i64 time);
    TRTStationBuilder& SetHasDeparture(bool has_departure);
    TRTStationBuilder& SetDeparture(i64 time);
    TRTStationBuilder& SetSearchableFrom(bool searchable);
    TRTStationBuilder& SetSearchableTo(bool searchable);
    TRTStationBuilder& SetTimezoneId(ui64 id);
};

TVector<TSettlement> GetSettlements();

TVector<TStation> GetStations();

TVector<TRThread> GetRThreads();

TVector<TThreadStation> GetRTStations();

TVector<TStation2Settlement> GetStationToSettlements();

THashMap<int, NDatetime::TTimeZone> GetTimezones();

THashMap<NRasp::object_id_t, TString> GetUids();

NRasp::TRaspDatabase GetDatabase();

bool operator==(const TSettlement& first, const TSettlement& second);

bool operator==(const TStation& first, const TStation& second);

bool operator==(const TRThread& first, const TRThread& second);

bool operator==(const TThreadStation& first, const TThreadStation& second);

bool operator==(const TStation2Settlement& first, const TStation2Settlement& second);
