#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/folder_set.h>
#include <macs/label_set.h>
#include <macs/tab_set.h>

using namespace ::testing;
using macs::TabFactory;

namespace hound::testing {

struct QueryMock {
    MOCK_METHOD(void, inTab, (macs::Tab::Type), (const));
    MOCK_METHOD(void, from, (int), (const));
    MOCK_METHOD(void, count, (int), (const));
    MOCK_METHOD(void, sortBy, (macs::EnvelopesSorting), (const));
    MOCK_METHOD(void, withinInterval, ((std::pair<std::time_t, std::time_t>)), (const));
    MOCK_METHOD(void, groupByThreads, (), (const));
    MOCK_METHOD(void, withoutLabel, (macs::Lid), (const));
    MOCK_METHOD(std::vector<macs::Envelope>, get, (), (const));
};

struct TestQuery {
    TestQuery() : mock(std::make_shared<StrictMock<QueryMock>>()) {}

    std::shared_ptr<StrictMock<QueryMock>> mock;

    const TestQuery& inTab(macs::Tab::Type type) const {
        mock->inTab(type);
        return *this;
    }
    const TestQuery& from(int from) const {
        mock->from(from);
        return *this;
    }
    const TestQuery& count(int count) const {
        mock->count(count);
        return *this;
    }
    const TestQuery& sortBy(macs::EnvelopesSorting sortType) const {
        mock->sortBy(sortType);
        return *this;
    }
    const TestQuery& withinInterval(std::pair<std::time_t, std::time_t> interval) const {
        mock->withinInterval(interval);
        return *this;
    }
    const TestQuery& groupByThreads() const {
        mock->groupByThreads();
        return *this;
    }
    const TestQuery& withoutLabel(macs::Label label) const {
        mock->withoutLabel(label.lid());
        return *this;
    }
    auto get() const {
        return mock->get();
    }
};

struct MailboxMock {
    TestQuery query_;

    const TestQuery& query() const { return query_; }

    template <class R, class Q>
    R fetch(Q&&) const {
        auto result = query_.get();
        return R{std::begin(result), std::end(result)};
    }

    MOCK_METHOD(macs::ThreadLabelsList, threadLabels, (std::vector<std::string>), (const));

    MOCK_METHOD(macs::FolderSet, folders, (), (const));
    MOCK_METHOD(macs::LabelSet, labels, (), (const));
    MOCK_METHOD(macs::TabSet, tabs, (), (const));
};

}
