#include "test_helpers.h"

#include <crypta/lib/native/tvm/create_tvm_client.h>
#include <crypta/lib/native/tvm/headers.h>
#include <crypta/lib/native/tvm/proto/tvm_config.pb.h>

#include <util/stream/file.h>

NTvmAuth::TTvmClient NCrypta::CreateRecipeTvmClient(ui32 selfTvmId, const TString& secret) {
    const auto& port = FromString<ui32>(TUnbufferedFileInput("tvmapi.port").ReadAll());

    NCrypta::TTvmApiConfig config;
    config.SetSelfClientId(selfTvmId);
    config.SetSecret(secret);
    config.SetTvmTestPort(port);
    auto* userTicketChecking = config.MutableUserTicketChecking();
    userTicketChecking->SetEnabled(true);
    userTicketChecking->SetBlackBoxEnv("ProdYaTeam");

    return NCrypta::CreateTvmClient(config);
}

NCrypta::THashMapHeaders NCrypta::GetTestTvmServiceHeaders(const TString& serviceTicket) {
    return NCrypta::THashMapHeaders{
        {NCrypta::SERVICE_TICKET_HEADER, serviceTicket},
    };
}

NCrypta::THashMapHeaders NCrypta::GetTestTvmUserHeaders(const TString& userTicket) {
    return NCrypta::THashMapHeaders{
        {NCrypta::USER_TICKET_HEADER, userTicket},
    };
}
