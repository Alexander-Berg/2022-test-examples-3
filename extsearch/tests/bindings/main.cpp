#include "linkdb_bindings.h"
#include "usersession_bindings.h"
#include "contrib/python/py3c/py3c.h"

#include <library/cpp/pybind/v2.h>

#include <yt/yt/core/misc/shutdown.h>

#include <util/system/atexit.h>

#include <Python.h>

MODULE_INIT_FUNC(bindings) {
    ::NPyBind::TPyModuleDefinition::InitModule("bindings");
    NImages::NPyLib::ExportLinkDBBindings();
    NImages::NPyLib::ExportUsersessionBindings();
    return ::NPyBind::TPyModuleDefinition::GetModule().M.RefGet();
}
