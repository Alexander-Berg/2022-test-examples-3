#pragma once

#include <mail/notsolitesrv/src/context.h>

#include <memory>
#include <string>

using TConfigParams = std::map<std::string, std::string>;
NNotSoLiteSrv::TConfigPtr GetConfig(const TConfigParams& params);

inline NNotSoLiteSrv::TContextPtr GetContext(const TConfigParams& params = {},
    const std::string& sessionId = "", const std::string& envelopeId = "")
{
    return std::make_shared<NNotSoLiteSrv::TContext>(GetConfig(params), sessionId, envelopeId);
}
