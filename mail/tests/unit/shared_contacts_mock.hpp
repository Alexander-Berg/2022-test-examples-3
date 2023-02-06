#pragma once

#include <src/logic/db/shared_contacts_impl.hpp>

namespace collie::logic::db {

template<class ... Ts>
struct SharedContactsImpl<collie::tests::MakeConnectionProvider<Ts ...>> {

    SharedContactsImpl(collie::tests::MakeConnectionProvider<Ts ...>) { }

    template<typename Connection> expected<ExistingContacts> getSharedContactsFromOrganisation(
        Connection&& provider,
        const TaskContextPtr&,
        const Uid& uid
    ) const {
        std::int64_t numericUid;
        if (!boost::conversion::try_lexical_convert<std::int64_t>(uid, numericUid)) {
            return make_unexpected(error_code(Error::userNotFound));
        }
        return  contacts::getSharedContacts(
            provider,
            numericUid,
            "passport_user",
            {1, 2, 3}
        );
    }

    template<typename Connection> expected<ContactsCountersResult> getSharedContactsCountFromOrganisation(
        Connection&&,
        const TaskContextPtr&,
        const Uid&
    ) const {
        return {};
    }
};

} //collie::logic::db
