#pragma once

#include <src/logic/interface/add_emails.hpp>

#include <gmock/gmock.h>

namespace collie::tests {

using namespace testing;

using collie::logic::AddEmails;
using collie::logic::CreatedContacts;
using collie::logic::Recipients;
using collie::logic::Uid;
using collie::TaskContextPtr;

struct AddEmailsMock : AddEmails {
    MOCK_METHOD(expected<CreatedContacts>, call, (const TaskContextPtr&, const Uid&, Recipients), (const));

    expected<CreatedContacts> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            Recipients recipients) const override {
        return call(context, uid, std::move(recipients));
    }
};

} // namespace collie::tests
