#include "fake_linguistic_provider.h"
#include "json.h"

#include <travel/rasp/rasp_data/dumper/lib/dumpers/settlement_dumper.h>
#include <travel/rasp/rasp_data/dumper/lib/fetcher/fake_fetcher.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NRasp::NData;
using namespace NRasp::NDumper;

Y_UNIT_TEST_SUITE(TFakeLinguisticProvider){
    Y_UNIT_TEST(Constructor){
        TList<TVector<TString>> queries;
TFakeLinguisticProvider linguisticProvider;
{
    NGeobase::TLinguistics l;
    l.NominativeCase = "москва по русски";
    linguisticProvider.AddLinguistics(l, {1, "ru"});
}
{
    NGeobase::TLinguistics l;
    l.NominativeCase = "киев по украински";
    linguisticProvider.AddLinguistics(l, {10, "uk"});
}
{
    NGeobase::TLinguistics l;
    l.NominativeCase = "берлин по русски";
    linguisticProvider.AddLinguistics(l, {100, "ru"});
}
{
    NGeobase::TLinguistics l;
    l.NominativeCase = "берлин по украински";
    linguisticProvider.AddLinguistics(l, {100, "uk"});
}

UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(0, "ru").NominativeCase, "");
UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(0, "uk").NominativeCase, "");

UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(1, "ru").NominativeCase, "москва по русски");
UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(1, "uk").NominativeCase, "");

UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(10, "ru").NominativeCase, "");
UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(10, "uk").NominativeCase, "киев по украински");

UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(100, "ru").NominativeCase, "берлин по русски");
UNIT_ASSERT_EQUAL(linguisticProvider.GetLinguistics(100, "uk").NominativeCase, "берлин по украински");
}
}
;
