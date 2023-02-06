#pragma once

#include <Python.h>

void CreateShard(PyObject* description, const char* outDir);

PyObject* DeserializeHits(const char* serialized, size_t len);
