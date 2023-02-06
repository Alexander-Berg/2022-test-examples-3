#include <extsearch/images/robot/guppy/yt/output.h>
#include <extsearch/images/robot/guppy/yt/hub.h>

#include <extsearch/images/robot/library/opt/params.h>
#include <extsearch/images/robot/library/logger/logging.h>
#include <extsearch/images/robot/library/pipeline/stubdatasink.h>

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/io.h>
#include <mapreduce/yt/io/node_table_writer.h>
#include <mapreduce/yt/io/proxy_output.h>

#include <util/generic/xrange.h>

namespace NImages {
    namespace NGuppy {

        class TYTFactory {
        private:
            const NYT::IClientBasePtr Client;
            const TString InputPath;
            const TString OutputPath;

        public:
            TYTFactory(const TString& server, const TString& inputPath, const TString& outputPath)
                : Client(server && inputPath && outputPath ? NYT::CreateClient(server) : NYT::IClientBasePtr())
                , InputPath(inputPath)
                , OutputPath(outputPath)
            {
            }

            void Read(INodeOutput::TPtr target) const {
                if (Client) {
                    const size_t READERS_QUEUE_SIZE = 1;
                    const size_t READERS_COUNT = 5;
                    const auto pathTarget = MakeAsynchronous(
                            ITablePathOutput::TPtr(new TTableReader(Client, target)),
                            READERS_QUEUE_SIZE,
                            READERS_COUNT,
                            "ReadTable");

                    const auto rows = Client->Get(InputPath + "/@row_count").AsInt64();
                    const auto portion = (rows + READERS_COUNT - 1) / READERS_COUNT;
                    for (auto start : xrange<ui64>(0, rows, portion)) {
                        NLog::Message("Read rows %u..%u", start, start + portion);
                        const auto tablePath = NYT::TRichYPath(InputPath).AddRange(NYT::TReadRange::FromRowIndices(start, start + portion));
                        pathTarget->Put(tablePath);
                    }
                    pathTarget->Flush();
                } else {
                    const auto reader = NYT::CreateTableReader<NYT::TNode>(&Cin);
                    Read(*reader, *target);
                    target->Flush();
                }
            }

            NYT::TTableWriterPtr<NYT::TNode> CreateWriter() const {
                if (Client) {
                    return Client->CreateTableWriter<NYT::TNode>(OutputPath);
                } else {
                    return new NYT::TTableWriter<NYT::TNode>(new NYT::TNodeTableWriter(THolder(new TProxyOutput(Cout))));
                }
            }

        private:
            using ITablePathOutput = IDataSink<NYT::TRichYPath>;

            class TTableReader : public ITablePathOutput {
            private:
                const NYT::IClientBasePtr Client;
                const INodeOutput::TPtr Output;

            public:
                TTableReader(NYT::IClientBasePtr client, INodeOutput::TPtr output)
                    : Client(client)
                    , Output(output)
                {
                }

                void Put(const NYT::TRichYPath& path) override {
                    const auto reader = Client->CreateTableReader<NYT::TNode>(path);
                    Read(*reader, *Output);
                }

                void Flush() override {
                    Output->Flush();
                }
            };

            template<class TReader>
            static void Read(TReader& reader, INodeOutput& target) {
                for (; reader.IsValid(); reader.Next()) {
                    target.Put(reader.MoveRow());
                }
            }

            class TProxyOutput : public NYT::TProxyOutput {
            private:
                IOutputStream& Out;

            public:
                explicit TProxyOutput(IOutputStream& out)
                    : Out(out)
                {
                }

                size_t GetStreamCount() const override {
                    return 1;
                }

                IOutputStream* GetStream(size_t index) const override {
                    Y_VERIFY(index == 0);
                    return &Out;
                }

                void OnRowFinished(size_t /*idx*/) override {
                }
            };
        };
    }  // NGuppy
}  // NImages

struct TCommandLineArgs {
    TString Server;
    TString InputTable;
    TString OutputTable;
    TString ConfigFile;
    size_t ThreadsCount = 1;
    size_t BufferSize = 66000;
    TString InputColumn;
    TString OutputColumn;

    static TCommandLineArgs ParseArgs(int argc, const char* argv[]) {
        TCommandLineArgs args;

        NImages::TCmdParams cmd;
        cmd.AddOptional("server", "YT cluster name", "<string>", &args.Server);
        cmd.AddOptional("input-table", "Input table path", "<table path>", &args.InputTable);
        cmd.AddOptional("output-table", "Output table path", "<table path>", &args.OutputTable);
        cmd.AddRequired("config", "Config filename", "<path>", &args.ConfigFile);
        cmd.AddOptional("threads", "Input processing threads count", "<integer>", ToString(args.ThreadsCount), &args.ThreadsCount);
        cmd.AddOptional("buffer", "Buffer size for processing records", "<integer>", ToString(args.BufferSize), &args.BufferSize);
        cmd.AddRequired("input-column", "Name of input column to extract image blob", "<string>", &args.InputColumn);
        cmd.AddRequired("output-column", "Name of output column to extract image blob", "<string>", &args.OutputColumn);
        cmd.Parse(argc, argv);

        return args;
    }
};


int TestMapper(int argc, const char* argv[]) {
    using namespace NImages::NGuppy;
    auto args = TCommandLineArgs::ParseArgs(argc, argv);

    const auto config = CreateConfigFromFile(args.ConfigFile);
    const TYTFactory factory(args.Server, args.InputTable, args.OutputTable);
    const auto outputWriter = factory.CreateWriter();

    const auto source = MakeAsynchronous(
        INodeOutput::TPtr(
            new TRecordsHub(
                *config,
                args.InputColumn,
                args.OutputColumn,
                args.BufferSize,
                new TNodeOutput(outputWriter.Get()))),
        args.BufferSize,
        args.ThreadsCount,
        "ReadImage");
    factory.Read(source);
    return 0;
}
