#pragma once

#include <src/services/db/contacts/query.hpp>

#include <yamail/data/serialization/yajl.h>

#include <boost/fusion/algorithm/query/all.hpp>
#include <boost/fusion/algorithm/transformation/zip.hpp>
#include <boost/fusion/sequence/intrinsic/at_c.hpp>


namespace collie::tests {

template <class T, class = std::void_t<>>
struct is_fusion_adapted_struct : std::false_type {};

template <class T>
struct is_fusion_adapted_struct<T,
    std::void_t<decltype(boost::fusion::extension::struct_member_name<T, 0>::call())>
> : std::true_type {};

template <class T>
constexpr auto FusionAdaptedStruct = is_fusion_adapted_struct<std::decay_t<T>>::value;

template <class T>
static std::enable_if_t<FusionAdaptedStruct<T>, bool> operator ==(const T& lhs, const T& rhs) {
    using namespace boost::fusion;
    return all(zip(lhs, rhs), [] (const auto& v) { return at_c<0>(v) == at_c<1>(v); });
}

template <class T>
static std::enable_if_t<FusionAdaptedStruct<T>, std::ostream&> operator <<(std::ostream& stream, const T& value) {
    using yamail::data::serialization::writeJson;
    return writeJson(stream, value);
}

} // namespace collie::tests

namespace collie::services::db::contacts::query {

template <class T, class = std::void_t<>>
struct is_query: std::false_type { };

template <class T>
struct is_query<T, std::enable_if_t<
    std::is_same_v<T, IsUserExists> ||
    std::is_same_v<T, RestoreContacts> ||
    std::is_same_v<T, GetChanges> ||
    std::is_same_v<T, GetContacts> ||
    std::is_same_v<T, GetContactsWithEmails> ||
    std::is_same_v<T, GetContactsByTagId> ||
    std::is_same_v<T, RemoveContacts> ||
    std::is_same_v<T, CreateTag> ||
    std::is_same_v<T, GetTags> ||
    std::is_same_v<T, RemoveTag> ||
    std::is_same_v<T, UpdateTag> ||
    std::is_same_v<T, GetContactsCount> ||
    std::is_same_v<T, GetContactsWithEmailsCount> ||
    std::is_same_v<T, CreateContacts> ||
    std::is_same_v<T, CreateContactsEmails> ||
    std::is_same_v<T, TagContacts> ||
    std::is_same_v<T, TagContactsEmails> ||
    std::is_same_v<T, AcquireRevision> ||
    std::is_same_v<T, UpdateContacts> ||
    std::is_same_v<T, GetContactsEmailIdsByContactsIds> ||
    std::is_same_v<T, GetContactsIdsByContactsEmailIds> ||
    std::is_same_v<T, GetContactsEmailIdsByTagIds> ||
    std::is_same_v<T, GetContactsEmailIdsByContactIdAndTagIds> ||
    std::is_same_v<T, GetContactsEmailsByTagIds> ||
    std::is_same_v<T, GetEmailIdsTagIdsByEmailIds> ||
    std::is_same_v<T, GetContactIdsByTagIds> ||
    std::is_same_v<T, GetContactIdsTagIdsByContactIds> ||
    std::is_same_v<T, UpdateContactsEmails> ||
    std::is_same_v<T, RemoveContactsEmails> ||
    std::is_same_v<T, UntagContacts> ||
    std::is_same_v<T, UntagContactsCompletely> ||
    std::is_same_v<T, UntagContactsEmails> ||
    std::is_same_v<T, UntagContactsEmailsCompletely> ||
    std::is_same_v<T, GetDefaultListId> ||
    std::is_same_v<T, GetOnlyNewEmails> ||
    std::is_same_v<T, GetContactsByTagNameAndTagTypeAndUris> ||
    std::is_same_v<T, GetTagIdByTagNameAndTagType> ||
    std::is_same_v<T, GetUserTypeLists> ||
    std::is_same_v<T, GetSharedContacts> ||
    std::is_same_v<T, SubscribeToContactsList> ||
    std::is_same_v<T, RevokeSubscribedContactsList> ||
    std::is_same_v<T, ShareContactsList> ||
    std::is_same_v<T, RevokeContactsList> ||
    std::is_same_v<T, GetSubscribedList> ||
    std::is_same_v<T, GetSubscribedLists> ||
    std::is_same_v<T, GetDirectoryEntities> ||
    std::is_same_v<T, CreateDirectoryEntities> ||
    std::is_same_v<T, RemoveDirectoryEntities> ||
    std::is_same_v<T, CreateContactsUser> ||
    std::is_same_v<T, GetSharedContactsCount> ||
    std::is_same_v<T, GetSharedContactsCountWithEmails> ||
    std::is_same_v<T, GetSharedLists> ||
    std::is_same_v<T, CreateContactsList> ||
    std::is_same_v<T, DeleteContactsList> ||
    std::is_same_v<T, GetSubscribedListIdsByOwner> ||
    std::is_same_v<T, GetTagById> ||
    std::is_same_v<T, GetContactIdsByListIds>
>> : std::true_type { };

template<class T , class = typename std::enable_if_t<is_query<T>::value>>
static bool operator == (const T& lhs,const T& rhs) {
    return boost::hana::to_tuple(lhs) == boost::hana::to_tuple(rhs);
}

} //collie::services::db::contacts::query
