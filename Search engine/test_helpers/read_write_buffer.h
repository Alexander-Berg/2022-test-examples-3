#pragma once

#include <util/generic/buffer.h>
#include <util/generic/maybe.h>
#include <util/memory/blob.h>
#include <util/stream/buffer.h>


namespace NPlutonium::NChunkler {

struct TReadWriteBuffer {
    TBlob Read();
    IOutputStream* StartWriting();

private:
    TBuffer Buffer_;
    TMaybe<TBufferOutput> OutputStream_;
};

}
