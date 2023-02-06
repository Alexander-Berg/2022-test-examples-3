#ifndef DOBERMAN_TESTS_SHARED_FOLDER_MOCK_H_
#define DOBERMAN_TESTS_SHARED_FOLDER_MOCK_H_

#include <src/logic/shared_folder.h>
#include <src/meta/labels.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

struct SharedFolderMock {
    MOCK_METHOD(Revision, revision, (), (const));
    MOCK_METHOD(Fid, fid, (), (const));
    MOCK_METHOD(Uid, uid, (), (const));
    MOCK_METHOD(std::vector<EnvelopeWithMimes>, envelopesWithMimes, (int64_t), (const));
    MOCK_METHOD(meta::labels::LabelsCache, labels, (), (const));
};

struct SharedFolderAccessMock {
    auto makeContext(const ::doberman::logic::SharedFolderCoordinates& c) { return c.owner.uid; }
    MOCK_METHOD(Revision, revision, (Uid), (const));
    MOCK_METHOD(std::vector<EnvelopeWithMimes>, envelopesWithMimes, (Uid, int64_t), (const));
    MOCK_METHOD(meta::labels::LabelsCache, labels, (Uid), (const));
};

inline auto ReturnRevision(Revision r = Revision{}) {
    return Return(doberman::Revision{r});
}

} // namespace test
} // namespace doberman


#endif /* DOBERMAN_TESTS_SHARED_FOLDER_MOCK_H_ */
