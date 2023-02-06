#pragma once

#include <gmock/gmock.h>
#include <macs/tabs_repository.h>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winconsistent-missing-override"
#endif

struct MockTabsRepository: public macs::TabsRepository {
    MOCK_METHOD(void, asyncGetTabs, (macs::OnTabs), (const, override));
    MOCK_METHOD(void, asyncGetOrCreateTab, (const macs::Tab::Type&,
                                                 macs::OnUpdateTab), (const, override));
    MOCK_METHOD(void, asyncResetFresh, (const macs::Tab::Type&,
                                             macs::OnUpdate), (const, override));
    MOCK_METHOD(void, asyncCanReadTabs, (macs::Hook<bool>), (const, override));

    macs::TabFactory factory(void) {
        return getTabFactory();
    }

    macs::Tab tab(const macs::Tab::Type& type,
                  size_t freshCount = 0) {
        return factory().type(type).freshMessagesCount(freshCount).release();
    }
};

struct TabsRepositoryTest: public testing::Test {
    typedef testing::StrictMock<MockTabsRepository> Repository;
    std::shared_ptr<Repository> tabsPtr;
    Repository& tabs;

    typedef decltype(testing::InvokeArgument<0>(macs::error_code(), macs::TabSet())) TabsInvoker;
    typedef std::vector<macs::Tab> TestData;

    TabsRepositoryTest() : tabsPtr(new Repository), tabs(*tabsPtr) {}

    static TabsInvoker GiveTabs(const TestData& args) {
        macs::TabsMap ret;
        for( const auto & i : args ) {
            ret[i.type()] = i;
        }
        return testing::InvokeArgument<0>(macs::error_code(),
                macs::TabSet(ret));
    }

    static TabsInvoker GiveTabs(std::initializer_list<macs::Tab> tabs) {
        return GiveTabs(TestData{ tabs });
    }
};

namespace macs {
inline bool operator==(const Tab& lhs, const Tab& rhs) {
    return lhs.type() == rhs.type()
            && lhs.revision() == rhs.revision()
            && lhs.messagesCount() == rhs.messagesCount()
            && lhs.newMessagesCount() == rhs.newMessagesCount()
            && lhs.freshMessagesCount() == rhs.freshMessagesCount()
            && lhs.bytes() == rhs.bytes();
}

inline bool operator==(const Tab& lhs, const Tab::Type& rhs) {
    return lhs.type() == rhs;
}
} // namespace macs

#ifdef __clang__
#pragma clang diagnostic pop
#endif
