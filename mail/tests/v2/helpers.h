#pragma once

#include <type_traits>

#include <mail/hound/include/internal/v2/folders_tree/folder_reflection.h>
#include <mail/hound/include/internal/v2/folders/folder_reflection.h>
#include <mail/hound/include/internal/v2/tabs/tab_reflection.h>
#include <mail/hound/include/internal/v2/messages/reflection.h>

namespace hound::testing {

template <typename Container, typename F, typename Result = std::invoke_result_t<F, const typename Container::value_type&>>
std::vector<Result> map(const Container& range, F func) {
    std::vector<Result> result;
    std::transform(range.begin(), range.end(), std::back_inserter(result), func);
    return result;
}

inline macs::Fid idExtractor(const hound::server::handlers::v2::folders_tree::Folder& folder) {
    return folder.id();
}

inline macs::Fid fidExtractor(const hound::server::handlers::v2::folders::Folder& folder) {
    return folder->fid();
}

inline std::string displayNameExtractor(const hound::server::handlers::v2::folders_tree::Folder& folder) {
    return folder.displayName();
}

inline std::string creationTimeExtractor(const hound::server::handlers::v2::folders_tree::Folder& folder) {
    return folder._folder->creationTime();
}

inline std::string tabTypeExtractor(const hound::server::handlers::v2::tabs::Tab& tab) {
    return tab->type().toString();
}

inline std::string midExtractor(const hound::server::handlers::v2::messages::Envelope& envelope) {
    return envelope->mid();
}

inline std::string shortMidExtractor(const hound::server::handlers::v2::messages::ShortMessage& message) {
    return message->mid();
}

} // namespace hound::testing
