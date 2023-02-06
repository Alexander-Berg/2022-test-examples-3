#include "read_write_buffer.h"


namespace NPlutonium::NChunkler {

TBlob TReadWriteBuffer::Read() {
    OutputStream_.Clear();
    return TBlob::FromBufferSingleThreaded(Buffer_);
}

IOutputStream* TReadWriteBuffer::StartWriting() {
    OutputStream_.Clear();
    Buffer_.Clear();
    OutputStream_.ConstructInPlace(Buffer_);
    return OutputStream_.Get();
}

}
