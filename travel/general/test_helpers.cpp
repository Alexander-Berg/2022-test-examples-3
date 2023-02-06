#include "test_helpers.h"

using namespace NRasp;

TVector<TSettlement> GetSettlements() {
    TVector<TSettlement> settlements;
    TVector<object_id_t> ids = {2, 5, 7, 10, 15, 16, 17};
    for (auto id : ids) {
        TSettlement settlement;
        settlement.set_title("ГОРОД-" + IntToString<10>(id));
        settlement.set_id(id);
        settlements.push_back(settlement);
    }
    return settlements;
}

TVector<TStation> GetStations() {
    TVector<TStation> stations;
    TVector<std::pair<object_id_t, object_id_t>> data = {
        {15, 2},
        {20, 2},
        {25, 5},
        {30, 7},
        {35, 10},
        {40, 15},
        {41, 5},
        {42, 5},
    };
    for (object_id_t id = 43, settlementId = 16; id < 49; id++) {
        data.emplace_back(id, settlementId);
    }

    for (object_id_t id = 49, settlementId = 17; id < 55; id++) {
        data.emplace_back(id, settlementId);
    }

    for (auto& info : data) {
        TStation station;
        auto id = info.first;
        auto settlementId = info.second;
        station.set_id(id);
        station.set_settlement_id(settlementId);
        station.set_majority_id(TStation::IN_TABLO_ID);
        stations.push_back(station);
    }
    return stations;
}

TVector<TRThread> GetRThreads() {
    TVector<TRThread> rthreads;

    TVector<object_id_t> ids = {51, 55, 58, 59, 60, 61, 63, 72, 75, 78, 79, 80};
    TVector<TRThread::ETransportType> transportTypes = {
        TRThread::BUS,
        TRThread::TRAIN,
        TRThread::TRAIN,
        TRThread::SUBURBAN,
        TRThread::TRAIN,
        TRThread::BUS,
        TRThread::BUS,
        TRThread::SUBURBAN,
        TRThread::SUBURBAN,
        TRThread::TRAIN,
        TRThread::TRAIN,
        TRThread::TRAIN};
    TVector<TRThread::ETypeId> typeIds(ids.size(), TRThread::BASIC_ID);
    typeIds[9] = typeIds[10] = TRThread::THROUGH_TRAIN_ID;

    TVector<TVector<ui32>> yearDays(ids.size(), TVector<ui32>(12, (~0) ^ (1 << 31)));

    TVector<ui32> startTimes(ids.size());

    for (ui32 i = 0, j = 0; i < ids.size(); i++, j += 3600)
        startTimes[i] = j;

    startTimes[9] = startTimes[2];

    for (size_t i = 0; i < ids.size(); i++) {
        TRThread thread;
        thread.set_id(ids[i]);
        thread.set_t_type_id(transportTypes[i]);
        thread.set_type_id(typeIds[i]);
        thread.set_tz_start_time(startTimes[i]);
        for (size_t j = 0; j < 12; j++)
            thread.add_year_days(yearDays[i][j]);
        rthreads.push_back(thread);
    }
    return rthreads;
}

THashMap<object_id_t, TString> GetUids() {
    TVector<object_id_t> ids{51, 55, 58, 59, 60, 61, 63, 72, 75, 78, 79, 80};
    THashMap<object_id_t, TString> uids;
    for (auto id : ids) {
        uids[id] = ToString(id) + "uid";
    }

    return uids;
}

TVector<TThreadStation> GetRTStations() {
    TVector<TThreadStation> rtstations = {
        TRTStationBuilder().SetId(100).SetStationId(15).SetThreadId(51).SetHasDeparture(true).SetDeparture(0).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(101).SetStationId(15).SetThreadId(55).SetHasDeparture(true).SetDeparture(20).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(102).SetStationId(20).SetThreadId(51).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(20).Build(),
        TRTStationBuilder().SetId(103).SetStationId(20).SetThreadId(55).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(30).Build(),
        TRTStationBuilder().SetId(104).SetStationId(25).SetThreadId(58).SetHasDeparture(true).SetDeparture(50).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(105).SetStationId(25).SetThreadId(59).SetHasDeparture(true).SetDeparture(60).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(106).SetStationId(30).SetThreadId(58).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(60).Build(),
        TRTStationBuilder().SetId(107).SetStationId(30).SetThreadId(59).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(70).Build(),
        TRTStationBuilder().SetId(108).SetStationId(35).SetThreadId(60).SetHasDeparture(true).SetDeparture(90).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(109).SetStationId(35).SetThreadId(61).SetHasDeparture(true).SetDeparture(100).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(110).SetStationId(40).SetThreadId(60).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(100).Build(),
        TRTStationBuilder().SetId(111).SetStationId(40).SetThreadId(61).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(110).Build(),

        TRTStationBuilder().SetId(112).SetStationId(20).SetThreadId(58).SetHasDeparture(true).SetDeparture(55).SetHasArrival(true).SetArrival(55).Build(),

        TRTStationBuilder().SetId(113).SetStationId(43).SetThreadId(63).SetHasDeparture(true).SetDeparture(10).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(114).SetStationId(49).SetThreadId(63).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(30).Build(),

        TRTStationBuilder().SetId(115).SetStationId(44).SetThreadId(72).SetHasDeparture(true).SetDeparture(15).SetHasArrival(false).SetArrival(0).SetSearchableFrom(false).Build(),
        TRTStationBuilder().SetId(116).SetStationId(45).SetThreadId(72).SetHasDeparture(true).SetDeparture(25).SetHasArrival(true).SetArrival(20).Build(),
        TRTStationBuilder().SetId(117).SetStationId(50).SetThreadId(72).SetHasDeparture(true).SetDeparture(55).SetHasArrival(true).SetArrival(50).Build(),
        TRTStationBuilder().SetId(118).SetStationId(51).SetThreadId(72).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(77).Build(),

        TRTStationBuilder().SetId(119).SetStationId(46).SetThreadId(75).SetHasDeparture(true).SetDeparture(10).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(120).SetStationId(47).SetThreadId(75).SetHasDeparture(true).SetDeparture(25).SetHasArrival(true).SetArrival(20).Build(),
        TRTStationBuilder().SetId(121).SetStationId(48).SetThreadId(75).SetHasDeparture(true).SetDeparture(32).SetHasArrival(true).SetArrival(30).Build(),
        TRTStationBuilder().SetId(122).SetStationId(52).SetThreadId(75).SetHasDeparture(true).SetDeparture(50).SetHasArrival(true).SetArrival(40).SetSearchableTo(false).Build(),
        TRTStationBuilder().SetId(123).SetStationId(53).SetThreadId(75).SetHasDeparture(true).SetDeparture(102).SetHasArrival(true).SetArrival(100).Build(),
        TRTStationBuilder().SetId(124).SetStationId(54).SetThreadId(75).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(120).Build(),

        TRTStationBuilder().SetId(125).SetStationId(25).SetThreadId(78).SetHasDeparture(true).SetDeparture(50).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(126).SetStationId(30).SetThreadId(78).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(60).Build(),

        TRTStationBuilder().SetId(127).SetStationId(25).SetThreadId(79).SetHasDeparture(true).SetDeparture(10).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(128).SetStationId(30).SetThreadId(79).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(40).Build(),

        TRTStationBuilder().SetId(129).SetStationId(35).SetThreadId(80).SetHasDeparture(true).SetDeparture(10).SetHasArrival(false).SetArrival(0).Build(),
        TRTStationBuilder().SetId(130).SetStationId(40).SetThreadId(80).SetHasDeparture(true).SetDeparture(850).SetHasArrival(true).SetArrival(840).Build(),
        TRTStationBuilder().SetId(131).SetStationId(35).SetThreadId(80).SetHasDeparture(false).SetDeparture(0).SetHasArrival(true).SetArrival(1700).Build()};

    return rtstations;
}

TVector<TStation2Settlement> GetStationToSettlements() {
    TVector<TStation2Settlement> stationToSettlements;

    TStation2Settlement station2Settlement;
    station2Settlement.set_station_id(15);
    station2Settlement.set_settlement_id(5);

    stationToSettlements.push_back(station2Settlement);
    return stationToSettlements;
}

THashMap<int, NDatetime::TTimeZone> GetTimezones() {
    return {
        {1, NDatetime::GetTimeZone("Europe/Moscow")},
        {2, NDatetime::GetTimeZone("Asia/Yekaterinburg")},
        {0, NDatetime::GetTimeZone("Europe/Minsk")}};
}

TRaspDatabase GetDatabase() {
    auto rthreads = GetRThreads();
    auto rtstations = GetRTStations();
    auto settlements = GetSettlements();
    auto stations = GetStations();
    auto stationToSettlements = GetStationToSettlements();
    auto timezones = GetTimezones();
    auto uids = GetUids();
    return TRaspDatabase(timezones, rthreads, rtstations, settlements, stations, stationToSettlements, {}, {}, uids);
}

bool operator==(const TSettlement& first, const TSettlement& second) {
    return first.SerializeAsString() == second.SerializeAsString();
}

bool operator==(const TStation& first, const TStation& second) {
    return first.SerializeAsString() == second.SerializeAsString();
}

bool operator==(const TRThread& first, const TRThread& second) {
    return first.SerializeAsString() == second.SerializeAsString();
}

bool operator==(const TThreadStation& first, const TThreadStation& second) {
    return first.SerializeAsString() == second.SerializeAsString();
}

bool operator==(const TStation2Settlement& first, const TStation2Settlement& second) {
    return first.SerializeAsString() == second.SerializeAsString();
}

TThreadStation TRTStationBuilder::Build() const {
    TThreadStation rtstation;
    rtstation.set_id(Id);
    rtstation.set_has_arrival(HasArrival);
    rtstation.set_tz_arrival(Arrival);
    rtstation.set_has_departure(HasDeparture);
    rtstation.set_tz_departure(Departure);
    rtstation.set_thread_id(ThreadId);
    rtstation.set_station_id(StationId);

    rtstation.set_is_searchable_from(IsSearchableFrom);
    rtstation.set_is_searchable_to(IsSearchableTo);
    rtstation.set_time_zone(TimezoneId);
    return rtstation;
}

TRTStationBuilder& TRTStationBuilder::SetId(object_id_t id) {
    Id = id;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetThreadId(object_id_t id) {
    ThreadId = id;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetStationId(object_id_t id) {
    StationId = id;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetHasArrival(bool has_arrival) {
    HasArrival = has_arrival;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetHasDeparture(bool has_departure) {
    HasDeparture = has_departure;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetArrival(i64 time) {
    Arrival = time;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetDeparture(i64 time) {
    Departure = time;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetSearchableFrom(bool searchable) {
    IsSearchableFrom = searchable;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetSearchableTo(bool searchable) {
    IsSearchableTo = searchable;
    return *this;
}

TRTStationBuilder& TRTStationBuilder::SetTimezoneId(ui64 id) {
    TimezoneId = id;
    return *this;
}
