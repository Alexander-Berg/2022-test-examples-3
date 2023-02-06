#include <library/cpp/offroad/tuple/adaptive_tuple_reader.h>
#include <kernel/doom/hits/panther_hit.h>
#include <kernel/doom/offroad/panther_hit_adaptors.h>
#include <kernel/doom/standard_models_storage/standard_models_storage.h>

#include <util/generic/array_ref.h>
#include <util/generic/scope.h>

#include <Python.h>

#include "common.h"

PyObject* DeserializeHits(const char* serialized, size_t len) {
    PyObjectHolder outList(PyList_New(0));
    if (!outList) {
        return nullptr;
    }

    using TReader = NOffroad::TAdaptiveTupleReader<NDoom::TPantherHit, NDoom::TPantherHitVectorizer, NDoom::TPantherHitSubtractor>;
    TReader::TTable table(NDoom::TStandardIoModelsStorage::Model<TReader::TModel>(NDoom::EStandardIoModel::InvertedIndexPantherHitModelV1));
    TReader reader(&table, MakeArrayRef(serialized, len));

    NDoom::TPantherHit hit;
    while (reader.ReadHit(&hit)) {
        static_assert(sizeof(unsigned) == sizeof(ui32));
        PyObjectHolder tuple(Py_BuildValue("II", hit.DocId(), hit.Relevance()));
        if (!tuple) {
            return nullptr;
        }
        if (PyList_Append(outList.get(), tuple.get()) != 0) {
            return nullptr;
        }
    }

    return outList.release();
}

