#pragma once

#include <src/logic/interface/create_contacts.hpp>

#include <gmock/gmock.h>

namespace collie::tests {

using logic::CreateContacts;
using logic::CreatedContacts;
using logic::NewContact;
using logic::Uid;

struct CreateContactsMock : CreateContacts {
    MOCK_METHOD(expected<CreatedContacts>, call, (const TaskContextPtr&, const Uid&,
            std::vector<NewContact>), (const));

    expected<CreatedContacts> operator()(const TaskContextPtr& context, const Uid& uid,
            std::vector<NewContact> newContacts) const override {
        return call(context, uid, std::move(newContacts));
    }
};

} // collie::tests
