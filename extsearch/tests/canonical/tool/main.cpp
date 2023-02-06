#include <extsearch/geo/kernel/pb2json/address.h>
#include <extsearch/geo/kernel/pb2json/advert.h>
#include <extsearch/geo/kernel/pb2json/arrival.h>
#include <extsearch/geo/kernel/pb2json/geocoder.h>
#include <extsearch/geo/kernel/pb2json/hours.h>
#include <extsearch/geo/kernel/pb2json/phone.h>
#include <extsearch/geo/kernel/pb2json/references.h>
#include <extsearch/geo/kernel/pb2json/transit.h>
#include <extsearch/geo/kernel/pb2json/uri.h>

#include <library/cpp/getopt/small/modchooser.h>
#include <library/cpp/protobuf/util/pb_io.h>

using namespace NGeosearch::NPb2Json;
namespace NPbProto = ::yandex::maps::proto;
namespace NPbSearch = ::yandex::maps::proto::search;

template <typename TMessage, auto convert>
int ConvertMain(int, const char**) {
    TMessage message;
    Y_ENSURE(TryParseFromTextFormat(Cin, message));

    NSc::TValue result;
    convert(message, result);
    Cout << result.ToJsonPretty() << Endl;
    return 0;
}

template <typename TMessage>
int ConvertMain(int, const char**) {
    TMessage message;
    Y_ENSURE(TryParseFromTextFormat(Cin, message));
    Cout << Convert(message).ToJsonPretty() << Endl;
    return 0;
}

int main(int argc, const char** argv) {
    TModChooser modChooser;
    modChooser.AddMode("Address", ConvertMain<NPbSearch::address::Address, ConvertAddress>, "");
    modChooser.AddMode("Advert", ConvertMain<NPbSearch::advert::Advert, ConvertAdvert>, "");
    modChooser.AddMode("ArrivalEntrance", ConvertMain<NPbProto::entrance::EntranceMetadata>, "");
    modChooser.AddMode("ArrivalRoutePoint", ConvertMain<NPbSearch::route_point::RoutePointMetadata>, "");
    modChooser.AddMode("GeocoderMetadata", ConvertMain<NPbSearch::geocoder::GeoObjectMetadata>, "");
    modChooser.AddMode("GeocoderResponseMetadata", ConvertMain<NPbSearch::geocoder::ResponseMetadata>, "");
    modChooser.AddMode("Hours", ConvertMain<NPbSearch::hours::OpenHours, ConvertOpenHours>, "");
    modChooser.AddMode("Phone", ConvertMain<NPbSearch::business::Phone, ConvertPhone>, "");
    modChooser.AddMode("References", ConvertMain<NPbSearch::references::References>, "");
    modChooser.AddMode("TransitRouteMetadata", ConvertMain<NPbSearch::transit::TransitRouteMetadata>, "");
    modChooser.AddMode("Uri", ConvertMain<NPbProto::uri::URIMetadata>, "");
    return modChooser.Run(argc, argv);
}
