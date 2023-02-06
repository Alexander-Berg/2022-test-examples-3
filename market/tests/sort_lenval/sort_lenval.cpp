#include <market/library/snappy-protostream/lenval_stream.h>


#include <util/generic/map.h>
#include <util/generic/string.h>
#include <util/stream/input.h>
#include <util/stream/output.h>


int main()
{
    NMarket::NLenval::TReader reader(Cin);
    TMultiMap<TString, TString> lenval;

    TString key;
    TString value;
    while (reader.Load(key))
    {
        reader.Load(value);
        lenval.insert(std::make_pair(key, value));
    }

    NMarket::NLenval::TWriter writer(Cout);
    for (auto it = lenval.begin(); it != lenval.end(); ++it)
    {
        writer.Write(it->first);
        writer.Write(it->second);
    }

    return 0;
}
