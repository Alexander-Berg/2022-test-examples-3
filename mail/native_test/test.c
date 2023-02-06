#include <util/system/compiler.h>
#include <util/system/types.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int counter = 0;

__attribute__((__visibility__("default")))
int jniwrapper_test_empty_ctor(const char* config, void** out) {
    Y_UNUSED(config);
    *out = 0;
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_copy_config_ctor(const char* config, void** out) {
    *out = strdup(config);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_counter_ctor(const char* config, void** out) {
    Y_UNUSED(config);
    *out = calloc(1, sizeof(int));
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_bad_ctor(const char* config, void** out) {
    Y_UNUSED(config);
    *out = 0;
    return -1;
}

__attribute__((__visibility__("default")))
void jniwrapper_test_dtor(void* instance) {
    free(instance);
}

__attribute__((__visibility__("default")))
void jniwrapper_test_increment(void* instance) {
    ++(*(int*) instance);
}

__attribute__((__visibility__("default")))
void jniwrapper_test_increment_global_counter(void* data) {
    ++counter;
    free(data);
}

static unsigned calc_sum(const void* data, size_t size) {
    unsigned sum = 0;
    const unsigned char* input = data;
    while (size--) {
        sum += *input++;
    }
    return sum;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_sum(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    unsigned sum;
    if (instance) {
        sum = calc_sum(instance, strlen((const char*) instance));
    } else {
        sum = 0;
    }
    sum += calc_sum(data, size);
    char* buf = malloc(11);
    *out = buf;
    sprintf(buf, "%u", sum);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_counter_to_string(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    char* buf = malloc(11);
    *out = buf;
    sprintf(buf, "%d", *(int*) instance);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_global_counter_to_string(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    char* buf = malloc(11);
    *out = buf;
    sprintf(buf, "%d", counter);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_copy(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    char* buf = malloc(size + 1);
    *out = buf;
    buf[size] = 0;
    memcpy(buf, data, size);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_copy_uri(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    Y_UNUSED(out);
    if (!uri) {
        uri = "NULL";
    }
    *out = strdup(uri);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_concat_uri_meta(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(data);
    Y_UNUSED(size);
    Y_UNUSED(out);
    const char prefix[] = "\xF0\x9F\xA4\x91 сегодня \xF0\x9F\xA4\x90: ";
    size_t prefixLen = (sizeof prefix) - 1;
    if (!metainfo) {
        metainfo = "NULL";
    }
    size_t uriLen = strlen(uri);
    size_t metainfoLen = strlen(metainfo);
    char* buf = malloc(uriLen + metainfoLen + prefixLen + 1);
    *out = buf;
    memcpy(buf, prefix, prefixLen);
    memcpy(buf + prefixLen, uri, uriLen);
    memcpy(buf + prefixLen + uriLen, metainfo, metainfoLen);
    buf[prefixLen + uriLen + metainfoLen] = 0;
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_concat_uri_meta16(
    void* instance,
    const ui16* uri,
    size_t uriLen,
    const ui16* metainfo,
    size_t metainfoLen,
    const void* data,
    size_t size,
    void** out,
    size_t* outLen)
{
    Y_UNUSED(instance);
    Y_UNUSED(metainfo);
    Y_UNUSED(metainfoLen);
    Y_UNUSED(data);
    Y_UNUSED(size);
    Y_UNUSED(out);
    const unsigned short prefix[] = {55358, 56592, 32, u'у', u'ж', u'е', 32, 55358, 56593, u':', 32};
    size_t prefixLen = sizeof prefix / sizeof prefix[0];
    *outLen = uriLen + metainfoLen + prefixLen;
    ui16* buf = malloc(*outLen * sizeof(ui16));
    *out = buf;
    memcpy(buf, prefix, sizeof prefix);
    memcpy(buf + prefixLen, uri, uriLen * sizeof(ui16));
    memcpy(buf + prefixLen + uriLen, metainfo, metainfoLen * sizeof(ui16));
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_copy_meta(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(data);
    Y_UNUSED(size);
    Y_UNUSED(out);
    if (!metainfo) {
        metainfo = "NULL";
    }
    *out = strdup(metainfo);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_copy_instance(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    Y_UNUSED(out);
    *out = strdup((const char*) instance);
    return 0;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_fail(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    *out = strdup("I don't want to parse this");
    return -1;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_oom(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    *out = 0;
    return -1;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_bad(
    void* instance,
    const char* uri,
    const char* metainfo,
    const void* data,
    size_t size,
    void** out)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(metainfo);
    Y_UNUSED(data);
    Y_UNUSED(size);
    *out = strdup("Bad input");
    return -2;
}

__attribute__((__visibility__("default")))
int jniwrapper_test_bad16(
    void* instance,
    const ui16* uri,
    size_t uriLen,
    const ui16* metainfo,
    size_t metainfoLen,
    const void* data,
    size_t size,
    void** out,
    size_t* outLen)
{
    Y_UNUSED(instance);
    Y_UNUSED(uri);
    Y_UNUSED(uriLen);
    Y_UNUSED(metainfo);
    Y_UNUSED(metainfoLen);
    Y_UNUSED(data);
    Y_UNUSED(size);
    const unsigned short* buf = u"Bad16 input";
    *outLen = 11;
    *out = malloc(22);
    memcpy(*out, buf, 22);
    return -2;
}

