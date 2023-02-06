#include "usersession_bindings.h"

#include "contrib/python/py3c/py3c.h"

#include <extsearch/images/robot/library/identifier/image.h>
#include <library/cpp/pybind/cast.h>
#include <library/cpp/pybind/ptr.h>
#include <library/cpp/pybind/v2.h>

#include <util/string/cast.h>

namespace {
    class TUsersessionBindings {
    public:
        ui64 GetCrcHash(const TString& crc) {
            const auto imageId = NImages::NIndex::TImageId(FromString<ui64>(crc));
            return THash<NImages::NIndex::TImageId>{}(imageId);
        }
    };

    void ExportUsersessionBindingsImpl() {
        ::NPyBind::TPyClass<TUsersessionBindings, NPyBind::TPyClassConfigTraits<true>>("TUsersessionBindings")
            .Def("get_hash_crc", &TUsersessionBindings::GetCrcHash)
            .Complete();
    }
}

void NImages::NPyLib::ExportUsersessionBindings() {
    ExportUsersessionBindingsImpl();
}
