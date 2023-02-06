#include <crypta/lib/native/juggler/juggler_client.h>

#include <crypta/lib/proto/endpoint_options/endpoint_options.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/system/env.h>

using namespace NCrypta;

namespace {
    TJugglerClient GetClient() {
        TJugglerClientOptions options;

        auto& endpointOptions = *options.MutableEndpointOptions();
        endpointOptions.SetUrlPrefix(GetEnv("JUGGLER_PUSH_URL_PREFIX"));
        endpointOptions.SetTimeoutSec(1);
        options.SetSource("source");

        return TJugglerClient(options);
    }
}

Y_UNIT_TEST_SUITE(TClient) {
    Y_UNIT_TEST(ReportOk) {
        auto client = GetClient();
        client.ReportOk("host", "service", "description");
    }

    Y_UNIT_TEST(ReportWarn) {
        auto client = GetClient();
        client.ReportWarn("host", "service", "description");
    }

    Y_UNIT_TEST(ReportCrit) {
        auto client = GetClient();
        client.ReportCrit("host", "service", "description");
    }
}
