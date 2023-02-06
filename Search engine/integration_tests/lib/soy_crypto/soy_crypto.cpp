#include <memory>
#include <openssl/bn.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bio.h>
#include <openssl/x509.h>

#include <cassert>
#define ASSERT assert

#include <cstring>
#include <string>
#include <iostream>
#include <library/cpp/string_utils/base64/base64.h>

#include <Python.h>
using namespace std;

TString soy_encrypt_part(RSA * rsa, const TString & msg) {
    unsigned char* to = (unsigned char*)malloc(RSA_size(rsa));
    char b64[1000];

    RSA_public_encrypt(msg.Size(), (const unsigned char*)msg.c_str(), to, rsa, RSA_PKCS1_OAEP_PADDING);

    Base64Encode(&b64[0], to, 256);
    TString result{&b64[0]};

    free(to);

    return result;
}

static PyObject * soy_encrypt(PyObject *self, PyObject *args) {
    char *pk;
    char *msg;

    if (PyTuple_Size(args) != 2) {
        PyErr_SetString(self, "soy_encrypt args error: soy_encrypt(public_key, messgae)");
    }

    if (!PyArg_ParseTuple(args, "ss", &pk, &msg)) {
        return nullptr;
    }

    BIO* bio = BIO_new_mem_buf(pk, strlen(pk));
    ASSERT(bio != nullptr);

    EVP_PKEY* pkey = PEM_read_bio_PUBKEY(bio, nullptr, nullptr, nullptr);
    ASSERT(pkey != nullptr);

    RSA* rsa = EVP_PKEY_get1_RSA(pkey);
    ASSERT(rsa != nullptr);

    size_t msg_len = strlen(msg);

    size_t pos = 0;
    size_t part_len = 100;
    TString result;

    while (pos < msg_len) {
        if (pos + part_len <= msg_len) {
            result += "<--" + soy_encrypt_part(rsa, TString{msg, pos, part_len}) + "-->";
        } else {
            result += "<--" + soy_encrypt_part(rsa, TString{msg, pos, msg_len - pos}) + "-->";
        }
        pos += part_len;
    }

    RSA_free(rsa);
    EVP_PKEY_free(pkey);
    BIO_free(bio);

    return Py_BuildValue("s", result.c_str());
}

// Список функций модуля
static PyMethodDef SoyCryptoMethods[] = {
    {"soy_encrypt", (PyCFunction)soy_encrypt, METH_VARARGS, "soy_encrypt"},
    {nullptr, nullptr, 0, nullptr}};

static struct PyModuleDef SoyCryptoDef = {
        PyModuleDef_HEAD_INIT,
        "soy_crypto",
        NULL,             // m_doc
        -1,               // m_size
        SoyCryptoMethods, // m_methods
        NULL,             // m_reload
        NULL,             // m_traverse
        NULL,             // m_clear
        NULL              // m_free
};

// Инициализация модуля
PyMODINIT_FUNC PyInit_soy_crypto(void) {
    return PyModule_Create(&SoyCryptoDef);
}
