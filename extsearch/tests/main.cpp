#include <library/cpp/testing/unittest/registar.h>

#include <extsearch/geo/kernel/pb2json/geocoder.h>
#include <extsearch/geo/kernel/pb2json/pb2json.h>

using namespace NGeosearch::NPb2Json;

namespace NPbSearch = ::yandex::maps::proto::search;

Y_UNIT_TEST_SUITE(pb2json) {
    Y_UNIT_TEST(TestException) {
        UNIT_ASSERT_EXCEPTION(MakeJson("Test"), yexception);
        UNIT_ASSERT_NO_EXCEPTION(MakeJson(""));

        UNIT_ASSERT_EXCEPTION(MakeResponseMetadata("Test"), yexception);
        UNIT_ASSERT_NO_EXCEPTION(MakeResponseMetadata(""));
    }

    Y_UNIT_TEST(TestGeocoderMetaDataKind) {
        NPbSearch::geocoder::GeoObjectMetadata metadata;
        auto& address = *metadata.mutable_address();
        address.set_formatted_address("tro-lo-lo");
        UNIT_ASSERT(Convert(metadata)["kind"].IsNull());

        {
            auto& component = *address.add_component();
            component.set_name("first");
            UNIT_ASSERT(Convert(metadata)["kind"].IsNull());

            component.add_kind(NPbSearch::kind::METRO_STATION);
            UNIT_ASSERT_EQUAL(Convert(metadata)["kind"].GetString(), "metro");

            component.add_kind(NPbSearch::kind::STATION);
            UNIT_ASSERT_EQUAL(Convert(metadata)["kind"].GetString(), "metro");
        }

        {
            auto& component = *address.add_component();
            component.set_name("second");
            UNIT_ASSERT(Convert(metadata)["kind"].IsNull());

            component.add_kind(NPbSearch::kind::AIRPORT);
            UNIT_ASSERT_EQUAL(Convert(metadata)["kind"].GetString(), "airport");
        }

        {
            auto& component = *address.add_component();
            component.set_name("third");
            UNIT_ASSERT(Convert(metadata)["kind"].IsNull());
        }

        UNIT_ASSERT_EQUAL(Convert(metadata).TrySelect("Address/Components/0/kind").GetString(), "metro");
        UNIT_ASSERT_EQUAL(Convert(metadata).TrySelect("Address/Components/1/kind").GetString(), "airport");

        UNIT_ASSERT_EQUAL(Convert(metadata).TrySelect("Address/Components/2/name").GetString(), "third");
        UNIT_ASSERT(Convert(metadata).TrySelect("Address/Components/2/kind").IsNull());
    }
}
