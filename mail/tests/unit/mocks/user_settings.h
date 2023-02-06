#include <src/backend/backend.h>

#include <sstream>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace yimap::backend;

class TestUserSettingsBackend : public UserSettingsBackend
{
public:
    Future<UserSettings> loadSettings() override
    {
        promises.push_back({});
        return promises.back();
    }

    std::vector<Promise<UserSettings>> promises;
};