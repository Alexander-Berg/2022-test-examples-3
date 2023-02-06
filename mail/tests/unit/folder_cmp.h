#ifndef DOBERMAN_TESTS_FOLDER_CMP_H_
#define DOBERMAN_TESTS_FOLDER_CMP_H_

#include <macs/folder.h>

namespace macs {

inline bool operator == (const macs::Folder& lf, const macs::Folder& rf) {
    return lf.fid() == rf.fid() && lf.name() == rf.name()
            && lf.type() == rf.type();
}

}

#endif /* DOBERMAN_TESTS_FOLDER_CMP_H_ */
