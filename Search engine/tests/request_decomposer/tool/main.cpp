#include <search/reqparam/reqparam.h>
#include <search/panther/runtime/request/request_decomposer.h>
#include <search/panther/protos/frequent_terms.pb.h>

#include <kernel/qtree/richrequest/printrichnode.h>

#include <kernel/reqbundle/parse_for_search.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <util/folder/path.h>

using namespace NPanther;

void TestDecomposedRequest(const TString& inputFile, const NSuperLemmer::ESuperLemmerVersion superLemmerVersion, TRequestParams& rp,  bool useReqBundle, bool enableNgrams, bool enableNewL1) {
    rp.EnableReqBundleInPanther = useReqBundle;
    rp.UseReqBundleConstraintChecker = true;
    if (enableNgrams) {
        rp.LumberJackFromNgrams = 3;
        rp.LumberJackToNgrams = 3;
    }
    if (enableNewL1) {
        Y_ENSURE(enableNgrams);
        rp.NewL1 = true;
    }

    TFsPath frequentTermsPath = TFsPath(GetWorkPath()) / "frequent_terms.proto";
    THashSet<TString> frequentTerms;
    Y_ENSURE(frequentTermsPath.Exists());
    TFileInput fin(frequentTermsPath);
    NPanther::TFrequentTermsProto frequentTermsProto;
    Y_ENSURE(frequentTermsProto.ParseFromArcadiaStream(&fin));
    frequentTerms = { frequentTermsProto.GetTerms().begin(), frequentTermsProto.GetTerms().end() };

    IInputStream* inputStream = &Cin;
    THolder<TFileInput> fileInput;

    if (inputFile) {
        fileInput = MakeHolder<TFileInput>(inputFile);
        inputStream = fileInput.Get();
    }

    TString qtree;
    static const TString qtreePrefix = "Qtree:";
    static const TString reqBundlePrefix = "ReqBundle:";
    while (inputStream->ReadLine(qtree)) {
        // comments
        if  (qtree.StartsWith("###")) {
            continue;
        }
        Y_ENSURE(qtree.StartsWith(qtreePrefix));
        TRichTreePtr tree = DeserializeRichTree(DecodeRichTreeBase64(qtree.substr(qtreePrefix.size())));
        DebugPrintRichTree(*tree->Root, Cout);
        Cout << Endl;

        TString bundle;
        inputStream->ReadLine(bundle);
        Y_ENSURE(bundle.StartsWith(reqBundlePrefix));

        if (useReqBundle) {
            NReqBundle::TReqBundleSearchParser::TOptions parserOptions;
            NReqBundle::TReqBundleSearchParser parser(parserOptions);
            if (bundle.size() != reqBundlePrefix.size()) {
                parser.AddBase64(bundle.substr(reqBundlePrefix.size()));
                rp.RequestBundle = parser.GetPreparedForSearch();
            }
        }

        THashDictionary hashDictionary;
        TDecomposedRequest request = DecomposeRequest(rp, tree, superLemmerVersion, &frequentTerms, &hashDictionary, enableNewL1);

        TVector<TRequestKey> keys;
        TVector<float> weights;
        Y_ENSURE(TDecomposedRequestCodec::Decompress(TDecomposedRequestCodec::Compress(request),  &keys, &weights));
        Y_ENSURE(keys == request.Keys);
        Y_ENSURE(weights == request.WordWeights);

        Cout << " word weights:" << Endl;
        for (const float weight : request.WordWeights) {
            Cout << "    " << weight << Endl;
        }

        Sort(request.Keys.begin(), request.Keys.end(), [] (const TRequestKey& a, const TRequestKey& b) {
            return a.DebugText() < b.DebugText();
        });

        Cout << "  keys:" << Endl;
        for (const TRequestKey& key : request.Keys) {
            Cout << "    ";

            if (key.IsNgram() || rp.NewL1.Get()) {
                Cout << TRequestKey::NecessityPrefix(key.Necessity()) << TRequestKey::BigEndianHexNgramEncode(key.Text());

                ui64 hash = TLumberjack::KeyToHash<ui64>(key.Text());

                Cout << " (grams: " << static_cast<size_t>(key.Grams()) << ", words: ";
                if (hashDictionary.Dict.contains(hash)) {
                    for (const TVector<TString>& wordsVariant : hashDictionary.Dict.at(hash)) {
                        Cout << "{ ";
                        for (const TString& word : wordsVariant) {
                            Cout << word << " ";
                        }
                        Cout << "} ";
                    }
                } else {
                    Cout << "unknown";
                }
                Cout << ") ";
            } else {
                Cout << key.DebugText();
            }

            Cout << Endl;
        }

        Sort(request.AttrsForCandidates.begin(), request.AttrsForCandidates.end());

        Cout << "  attrs for candidates:" << Endl;
        for (const TString& attr : request.AttrsForCandidates) {
            Cout << "    " << attr << Endl;
        }

        Cout << "  attr restrictions:" << Endl;
        for (const NAttributes::TSingleAttrRestriction& restriction : request.AttrRestrictions.GetBase().GetTree()) {
            if (restriction.GetTreeOper() == NAttributes::TSingleAttrRestriction::Leaf) {
                Cout << "    {" << restriction.GetLeft() << " " << restriction.GetRight() << " " << static_cast<int>(restriction.GetCmpOper()) << "}" << Endl;
            }
        }
    }
}

int main(int argc, const char* argv[]) {
    NLastGetopt::TOpts opts;

    opts.SetTitle("request_decomposer - reads base64 encoded `Qtree:` and `ReqBundle:` (see request_decomposer.in) from stdin (or input file), decompose and writes it on stdout.");

    TString version;
    opts
        .AddLongOption("super-lemmer-version", "SuperLemmerVersion")
        .Optional()
        .RequiredArgument("SUPER_LEMMER_VERSION")
        .StoreResult(&version)
        .DefaultValue("none");

    TString inputFile;
    opts
        .AddLongOption('i', "input", "Input file path")
        .Optional()
        .RequiredArgument("PATH")
        .StoreResult(&inputFile);

    bool enableNgrams = false;
    opts
        .AddLongOption('n', "enable-ngrams", "Enable ngrams")
        .Optional()
        .NoArgument()
        .SetFlag(&enableNgrams);

    bool enableNewL1 = false;
    opts
        .AddLongOption('l', "enable-new-l1", "Enable new l1 in lumberjack")
        .Optional()
        .NoArgument()
        .SetFlag(&enableNewL1);

    opts.AddHelpOption('h');
    NLastGetopt::TOptsParseResult(&opts, argc, argv);

    TRequestParams rp;
    const NSuperLemmer::ESuperLemmerVersion superLemmerVersion = FromString<NSuperLemmer::ESuperLemmerVersion>(version);
    TestDecomposedRequest(inputFile, superLemmerVersion, rp, /*useReqBundle=*/false, enableNgrams, enableNewL1);
    TestDecomposedRequest(inputFile, superLemmerVersion, rp, /*useReqBundle=*/true, enableNgrams, enableNewL1);
    return 0;
}
