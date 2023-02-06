#include <extsearch/audio/generative/cpp/backend/library/encoder/mp3encoder.h>
#include <extsearch/audio/generative/cpp/backend/library/decoder/mp3decoder.h>

#include <library/cpp/logger/stream.h>

#include <util/system/filemap.h>

int main() {
    try {
        using namespace NGenerative;
        TAudioConfig ac;
        auto logBase = MakeHolder<TLog>(MakeHolder<TStreamLogBackend>(&Cout));
        TGenericLogger log(logBase.Get());
        TMp3Decoder dec(logBase.Get(), ac);
        TMp3Encoder enc(logBase.Get(), ac, false);
        Y_ENSURE(enc.Init());
        TString filename = "test.mp3";
        TFileMap fm(filename, TFileMap::oRdOnly);
        fm.Map(0, fm.Length());
        auto ptr = static_cast<const uint8_t*>(fm.Ptr());
        TInBuffer in(ptr, fm.Length());
        auto frames = dec.ParseFrames(in);
        const size_t toDecodeFrames = ceil(10.0 / ac.MP3FrameTimeSec);
        const size_t toDecodePadding = 4;
        const size_t decDelay = 528;
        //const size_t encDelay = 576;
        //const size_t encPadSize = ac.PCMFramesPerMP3Frame - decDelay;
        TAudioBufferS16 buf((toDecodeFrames + toDecodePadding + 1) * ac.PCMFramesPerMP3Frame);
        //buf.SetOffset(encPadSize);
        size_t decSize = frames[toDecodeFrames + toDecodePadding].Offset - frames[0].Offset;
        TInBuffer inFrames(ptr + frames[0].Offset, decSize);
        Y_ENSURE(dec.Decode(inFrames, buf));
        log.Debug() << "Remaining bytes = " << buf.RemainingFrames();
        buf.Rewind();
        buf.SetOffset(decDelay);
        TOutBuffer out(decSize);
        buf.SetPrefill(0);
        buf.SetPadding(1);
        enc.SetExpectedFrames(toDecodeFrames);
        enc.Encode(buf, out, false);
        log.Debug() << "out.Offset = " << out.GetCurrentOffset();
        TFile f("out.mp3", CreateAlways);
        f.Write(out.GetBeginPtr(), out.GetCurrentOffset());
        f.Write(ptr + frames[toDecodeFrames].Offset, frames.back().Offset + frames.back().Size - frames[toDecodeFrames].Offset);
    }catch(...) {
        Cerr << "Exception:" << CurrentExceptionMessage() << Endl;
    }
}
