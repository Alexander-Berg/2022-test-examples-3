#pragma once

#include <Python.h>
#include <memory>

namespace NDetail {
    struct PyObjectDeleter {
        void operator()(PyObject* obj) const noexcept {
            Py_DECREF(obj);
        }
    };
} // namespace

using PyObjectHolder = std::unique_ptr<PyObject, NDetail::PyObjectDeleter>;

