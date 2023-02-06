#include <src/directory_sync/event_queue/module_interface.hpp>
#include <src/logic/db/add_directory_event_impl.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/logic/interface/types/new_event.hpp>

#include <gmock/gmock.h>

namespace collie::logic {

static bool operator==(const NewEvent& left, const NewEvent& right) {
    return std::tie(left.org_id, left.event, left.revision) == std::tie(right.org_id, right.event, right.revision);
}

}

namespace {

using collie::directory_sync::event_queue::ModuleInterface;
using collie::logic::NewEvent;
using collie::logic::EventType;
using collie::logic::OrgId;
using collie::make_expected;

struct ModuleInterfaceMock : ModuleInterface {
    MOCK_METHOD(void, addEvent, (NewEvent), (override));
};

using GetEventQueueModule = std::function<std::shared_ptr<ModuleInterface>()>;
using AddDirectoryEventImpl = collie::logic::db::AddDirectoryEventImpl<GetEventQueueModule>;
struct TestLogicDbAddDirectoryEventImpl : public TestWithTaskContext {
    TestLogicDbAddDirectoryEventImpl()
        : moduleInterfaceMock(std::make_shared<StrictMock<ModuleInterfaceMock>>())
        , addDirectoryEventImpl([&]{return moduleInterfaceMock;}) {
    }

    std::shared_ptr<StrictMock<ModuleInterfaceMock>> moduleInterfaceMock;
    const AddDirectoryEventImpl addDirectoryEventImpl;
};

TEST_F(TestLogicDbAddDirectoryEventImpl, must_add_event) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        const OrgId org_id{1};
        NewEvent newEvent{org_id, EventType::updateOrg, 42};
        EXPECT_CALL(*moduleInterfaceMock, addEvent(newEvent));
        EXPECT_EQ(make_expected(), addDirectoryEventImpl(context, std::move(newEvent)));
    });
}

} // namespace
