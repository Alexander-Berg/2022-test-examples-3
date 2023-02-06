#include <extsearch/audio/generative/cpp/backend/library/flacdecoder/flacdecoder.h>
#include <extsearch/audio/generative/cpp/backend/library/logger/logger.h>

#include <library/cpp/logger/stream.h>

#include <util/system/file.h>
#include <util/stream/file.h>

int main(int argc, char* argv[]) {
    if (argc != 3) {
        return 1;
    }
    using namespace NGenerative;
    try {
        TString inputFilename = argv[1];
        TString outputFilename = argv[2];

        TFile rawAudioFile(outputFilename, CreateAlways);

        TLog log(MakeHolder<TStreamLogBackend>(& Cout));
        TGenericLogger glog(&log);

        auto flacDecoder = IFlacDecoder::Create(&log, [&rawAudioFile](const TArrayRef <uint8_t> data) {
            rawAudioFile.Write(data.data(), data.size());
        });
        if (!flacDecoder) {
            glog.Error() << "Failed to create flac decoder";
            return 1;
        }
        TVector <uint8_t> data;
        const size_t chunkSize = 16384;
        data.resize(chunkSize);
        IFlacDecoder::EDecodeStatus result = IFlacDecoder::EDecodeStatus::InProgress;
        TFileInput in{TFile(inputFilename, RdOnly)};
        size_t rsz = 0;
        size_t inBuf = 0;
        while ((rsz = in.Read(&data[inBuf], chunkSize)) != 0 || result == IFlacDecoder::EDecodeStatus::InProgress) {
            data.resize(inBuf + rsz);
            glog.Debug() << "Read " << rsz;
            result = flacDecoder->Decode(data);
            if (result == IFlacDecoder::EDecodeStatus::Error) {
                glog.Error() << "Flac decoding failed";
                return 1;
            }
            inBuf = data.size();
            data.resize(inBuf + chunkSize);
        }
        rawAudioFile.Close();
    } catch (...) {
        Cerr << "Exception:" << CurrentExceptionMessage() << Endl;
        return 1;
    }
    return 0;
}
