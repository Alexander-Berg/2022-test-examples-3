#include <search/base_search/inverted_index_storage/shard/inverted_index_writer.h>

#include <util/folder/path.h>
#include <util/generic/xrange.h>
#include <util/generic/yexception.h>

#include <Python.h>

#include "common.h"

namespace {
    struct FmtObj {
        FmtObj(PyObject* obj)
            : Obj(obj)
        {
        }

        friend
        IOutputStream& operator<<(IOutputStream& out, const FmtObj& obj) {
            PyObjectHolder repr(PyObject_Str(obj.Obj));
            PyObjectHolder type(PyObject_Type(obj.Obj));
            PyObjectHolder typeStr(PyObject_Str(type.get()));
            out << "type: '" << PyString_AsString(typeStr.get()) << "'; as string: '" << PyString_AsString(repr.get()) << "'";
            return out;
        }

    private:
        PyObject* Obj;
    };

    struct PyError: public yexception {
        PyError(PyObject* ex)
            : Ex(ex)
        {
        }

        PyObject* Ex;
    };

    bool IsIndexable(PyObject* obj) {
        return PyTuple_Check(obj) || PyList_Check(obj);
    }

    PyObject* GetElementAtIndex(PyObject* indexable, Py_ssize_t index) {
        Y_VERIFY(index >= 0);
        if (PyTuple_Check(indexable)) {
            return PyTuple_GET_ITEM(indexable, index);
        } else if (PyList_Check(indexable)) {
            return PyList_GET_ITEM(indexable, index);
        }
        Y_VERIFY(false);
    }

    Py_ssize_t GetSize(PyObject* indexable) {
        if (PyTuple_Check(indexable)) {
            return PyTuple_GET_SIZE(indexable);
        } else if (PyList_Check(indexable)) {
            return PyList_GET_SIZE(indexable);
        }
        Y_VERIFY(false);
    }

    bool IsIndexableOfSize(PyObject* obj, Py_ssize_t size) {
        return IsIndexable(obj) && GetSize(obj) == size;
    }

    template <typename T>
    T GetAs(PyObject* obj);

    template <>
    ui32 GetAs<ui32>(PyObject* obj) {
        if (!PyInt_Check(obj)) {
            ythrow PyError(PyExc_TypeError) << "Expected an integer, got " << FmtObj(obj);
        }
        return static_cast<ui32>(PyInt_AsUnsignedLongMask(obj));
    }

    template <>
    ui64 GetAs<ui64>(PyObject* obj) {
        static_assert(sizeof(ui64) == sizeof(PY_LONG_LONG));
        if (PyLong_Check(obj)) {
            // FIXME: overflow checks
            return PyLong_AsUnsignedLongMask(obj);
        } else if (PyInt_Check(obj)) {
            return PyInt_AsUnsignedLongLongMask(obj);
        }
        ythrow PyError(PyExc_TypeError) << "Expected an integer, got " << FmtObj(obj);
    }
}

void CreateShard(PyObject* description, const char* outDir) {
    try {
        if (!IsIndexable(description)) {
            ythrow PyError(PyExc_TypeError) << "Expected a list or a tuple as the input parameter, got " << FmtObj(description);
        }

        NBaseSearch::TInvertedIndexWriter writer(TFsPath(outDir) / "invertedindexstorage.", 16);
        Py_ssize_t len = GetSize(description);
        Y_VERIFY(len >= 0);
        for (auto i : xrange(len)) {
            PyObject* elem = GetElementAtIndex(description, i);
            Y_VERIFY(elem);
            if (!IsIndexableOfSize(elem, 2)) {
                ythrow PyError(PyExc_TypeError) << "Element at index " << i << " needs to be a list or a tuple of size 2, got "
                    << FmtObj(elem);
            }
            PyObject* hits = GetElementAtIndex(elem, 1);
            if (!IsIndexable(hits)) {
                ythrow PyError(PyExc_TypeError) << "Element at index [" << i <<"][1] needs to be a list or a tuple, got " << FmtObj(hits);
            }
            for (auto j : xrange(GetSize(hits))) {
                PyObject* hit = GetElementAtIndex(hits, j);
                if (!IsIndexableOfSize(hit, 2)) {
                    ythrow PyError(PyExc_TypeError) << "Each hit must be be a list or a tuple of size 2, got " << FmtObj(hit);
                }

                writer.WriteHit({GetAs<ui32>(GetElementAtIndex(hit, 0)), GetAs<ui32>(GetElementAtIndex(hit, 1))});
            }
            writer.WriteHash(GetAs<ui64>(GetElementAtIndex(elem, 0)));
        }
        writer.Finish();
    } catch (const PyError& ex) {
        PyErr_SetString(ex.Ex, ex.what());
    } catch (const std::exception& ex) {
        PyErr_Format(PyExc_RuntimeError, "Unknown exception in native code: %s", ex.what());
    }
}

