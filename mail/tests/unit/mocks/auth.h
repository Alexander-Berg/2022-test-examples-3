#include <src/backend/backend.h>

#include <sstream>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace yimap::backend;

class TestAuthBackend : public AuthBackend
{
public:
    Future<AuthResult> asyncLogin(string /*login*/, string /*pass*/) override
    {
        promises.push_back({});
        return promises.back();
    }

    Future<AuthResult> asyncLoginOAuth(string /*token*/) override
    {
        promises.push_back({});
        return promises.back();
    }

    std::vector<Promise<AuthResult>> promises;
};
