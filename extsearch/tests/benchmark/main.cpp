#include <extsearch/geo/kernel/xml_writer/tests/lib/example.h>
#include <extsearch/geo/kernel/xml_writer/wrapper/libxml.h>
#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <library/cpp/testing/benchmark/bench.h>

Y_CPU_BENCHMARK(CreateLibXml, iface) {
    for (size_t i = 0; i < iface.Iterations(); ++i) {
        NXml::TDocument doc{"ymaps", NXml::TDocument::RootName};
        FillBsuDocument(NXmlWr::TLibXmlNode{doc.Root()});
        TStringStream sstream;
        doc.Root().Save(sstream, "UTF-8", false /* no pretty formatting */);
        TString res = sstream.Str();
        Y_DO_NOT_OPTIMIZE_AWAY(res);
    }
}

Y_CPU_BENCHMARK(CreateMy, iface) {
    for (size_t i = 0; i < iface.Iterations(); ++i) {
        NXmlWr::TDocument doc{"ymaps"};
        FillBsuDocument(doc.Root());
        TString res = doc.Root().AsString();
        Y_DO_NOT_OPTIMIZE_AWAY(res);
    }
}
