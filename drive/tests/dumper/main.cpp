#include <drive/telematics/api/sensor/client.h>
#include <drive/telematics/server/location/names.h>
#include <drive/telematics/server/pusher/local.h>
#include <drive/tests/library/database.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/small/modchooser.h>
#include <library/cpp/logger/global/global.h>

#include <util/stream/file.h>

int main_sensors(int argc, const char** argv) {
    NLastGetopt::TOpts options = NLastGetopt::TOpts::Default();
    options.AddHelpOption();
    options.AddLongOption("sensor-host", "Sensor API host").RequiredArgument("HOST").DefaultValue("saas-searchproxy-maps-prestable.yandex.net");
    options.AddLongOption("sensor-port", "Sensor API port").RequiredArgument("PORT").DefaultValue("17000");
    options.AddLongOption("sensor-service", "Sensor API service").RequiredArgument("SERVICE").DefaultValue("drive_cache");
    options.AddLongOption('o', "output", "Output file").RequiredArgument("PATH").Optional();
    options.SetFreeArgsNum(0);
    NLastGetopt::TOptsParseResult res(&options, argc, argv);

    NRTLine::TNehSearchClient searchClient(res.Get("sensor-service"), res.Get("sensor-host"), FromString<ui16>(res.Get("sensor-port")));
    NDrive::TSensorApi sensorsApi("dumper", searchClient);

    auto heartbeats = sensorsApi.GetHeartbeats();
    TLocalSensorStorage localSensorStorage;
    for (auto&&[imei, heartbeat] : heartbeats.GetValueSync()) {
        localSensorStorage.Push(imei, heartbeat);
        auto sensors = sensorsApi.GetSensors(imei);
        for (auto&& sensor : sensors.GetValueSync()) {
            localSensorStorage.Push(imei, sensor);
        }
    }

    auto names = {
        NDrive::RawLocationName,
        NDrive::LinkedLocationName,
        NDrive::LBSLocationName,
        NDrive::HeadLocationName,
        NDrive::GeocodedLocationName,
    };
    for (auto&& name : names) {
        auto locations = sensorsApi.GetLocations(name);
        for (auto&&[imei, location] : locations.GetValueSync()) {
            localSensorStorage.Push(imei, location);
        }
    }

    auto serialized = localSensorStorage.ToJson();
    if (res.Has("output")) {
        const TString& file = res.Get("output");
        TOFStream output(file);
        output.Write(serialized.GetStringRobust());
    } else {
        Cout << serialized.GetStringRobust() << Endl;
    }

    TLocalSensorStorage deserialized;
    Y_ENSURE(deserialized.TryFromJson(serialized));

    return EXIT_SUCCESS;
}

int main_transfer(int argc, const char** argv) {
    NLastGetopt::TOpts options = NLastGetopt::TOpts::Default();
    options.AddHelpOption();
    options.AddLongOption("from", "Configuration file for database from which data will be transfered")
           .RequiredArgument("FILE");
    options.AddLongOption("to", "Configuration file for databse to which data will be transfered")
           .RequiredArgument("FILE");
    options.AddLongOption("history-age", "How much history to keep (e.g. 30d (30 days), 2w (2 weeks))")
           .OptionalArgument("DURATION")
           .DefaultValue("30d");
    options.AddLongOption("match-fields", "Try to match fields before fetch, fallback to \'SELECT *\' on fail.")
           .NoArgument();
    options.SetFreeArgsNum(0);
    NLastGetopt::TOptsParseResult res(&options, argc, argv);

    NSQL::IDatabase::TPtr from = NDrive::CreateDatabase(res.Get("from"));
    Y_ENSURE(from, "cannot create FROM database");
    NSQL::IDatabase::TPtr to = NDrive::CreateDatabase(res.Get("to"));

    Y_ENSURE(to, "cannot create TO database");
    TString ageString = res.Get("history-age");
    TDuration ageDuration;
    if (ageString != "inf" && !TDuration::TryParse(ageString, ageDuration)) {
        Cerr << "Wrong duration format: " << ageString << Endl;
        return EXIT_FAILURE;
    }
    TInstant sinceInstant;
    if (ageString != "inf") {
        sinceInstant = TInstant::Now() - ageDuration;
        INFO_LOG << "history truncated to " << ToString(sinceInstant) << Endl;
    }

    NDrive::CreateStructure(*to);
    NDrive::TransferData(*from, *to, sinceInstant, res.Has("match-fields"));

    return EXIT_SUCCESS;
}

int main(int argc, const char** argv) {
    DoInitGlobalLog("cerr", TLOG_INFO, false, false);

    TModChooser modChooser;
    modChooser.AddMode("sensors", main_sensors, "Dump sensors storage");
    modChooser.AddMode("transfer", main_transfer, "Transfer data between databases");
    try {
        return modChooser.Run(argc, argv);
    } catch (const std::exception& e) {
        Cerr << "An exception has occurred: " << FormatExc(e) << Endl;
        return EXIT_FAILURE;
    }
}
