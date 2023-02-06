#include <extsearch/audio/kernel/recoglib/wav.h>

#include <util/stream/input.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NRecogTools;

namespace {

    Y_UNIT_TEST_SUITE(TWAVTests) {
        Y_UNIT_TEST(TestLoadWavWithHeaderThen4BytesDataReturnsFalse) {
            TWAV wav;
            TString fakeWavData("RIFF\xde\xad\xbe\xaf");
            TStringInput fakeWavDataInput(fakeWavData);
            UNIT_ASSERT_EQUAL(wav.LoadWav(fakeWavDataInput), false);
        }
    }
}
