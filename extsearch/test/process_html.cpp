#include "process_html.h"

#include <kernel/hosts/owner/owner.h>
#include <kernel/indexer/baseproc/doc_attr_filler.h>
#include <kernel/indexer/face/blob/directzoneinserter.h>

#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/numerator/blob/numserializer.h>
#include <library/cpp/html/html5/parse.h>
#include <library/cpp/html/zoneconf/ht_conf.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/stream/file.h>

namespace NImageLib {
    void ProcessHtml(const TString& htmlPath, ECharset enc, TBuffer& numeratorEvents, TBuffer& zone, TBuffer& zoneImg, const TString& url) {
        Y_VERIFY(url.StartsWith("http"));

        // Parser
        const TMappedFileInput mappedFile(htmlPath);
        const TString html(mappedFile.Buf(), mappedFile.Avail());
        NHtml::THtmlChunksWriter chunks;
        NHtml5::ParseHtml(html, &chunks);

        // Numerator
        // Numerator config
        static constexpr size_t ConfigCount = 2;
        THtConfigurator Configurator[ConfigCount];
        Configurator[0].Configure((ArcadiaSourceRoot() + "/yweb/common/roboconf/htparser.ini").data());
        Configurator[1].Configure((ArcadiaSourceRoot() + "/extsearch/images/robot/parsers/html_parser/config/htparser.linktext.ini").data());

        NHtml::TStorage storage;
        storage.SetPeerMode(NHtml::TStorage::ExternalText);
        NHtml::TParserResult parsed(storage);

        const TBuffer htmlChunks = chunks.CreateResultBuffer();
        const NHtml::TChunksRef chunksRefs(htmlChunks);

        if (!NHtml::NumerateHtmlChunks(chunksRefs, &parsed)) {
            ythrow yexception() << "can't deserialize html chunks";
        }

        THolder<IParsedDocProperties> props(CreateParsedDocProperties());
        props->SetProperty(PP_CHARSET, NameByCharset(enc));
        props->SetProperty(PP_BASE, url.data());

        TBuffer buf;
        TBuffer zoneBuffers[ConfigCount];
        TNumerSerializer serializer(buf, props.Get(), Configurator, ConfigCount, zoneBuffers);
        Numerator numerator(serializer);
        numerator.Numerate(storage.Begin(), storage.End(), props.Get(), nullptr);
        if (!numerator.DocFormatOK()) {
            ythrow yexception() << "Numerator: " << numerator.GetParseError();
        }

        numeratorEvents = SerializeNumeratorEvents(chunksRefs, buf, props);
        zone = zoneBuffers[0];
        zoneImg = zoneBuffers[1];
    }

    TBuffer ProcessSegmentator(const TBuffer& numeratorEvents, const TBuffer& zone) {
        TNumeratorEvents events(numeratorEvents);
        NSegm::TSegmentatorHandler<> handler(true);
        handler.InitSegmentator("", nullptr);
        events.Numerate(handler, zone.Data(), zone.Size());

        NIndexerCore::TDirectZoneInserter inserter;
        StoreSegmentatorSpans(handler, inserter);
        inserter.PrepareZones();

        const TBuffer segmentZone = inserter.SerializeZones();
        return segmentZone;
    }

} // namespace NImageLib
