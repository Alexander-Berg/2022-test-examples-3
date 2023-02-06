#include <extsearch/images/robot/parsers/microdata/schemaorg/parser/parse.h>

#include <yweb/protos/outlinks/links.pb.h>
#include <yweb/robot/preparat/io/io.h>
#include <yweb/structhtml/semweb/tree/output_json.h>

#include <kernel/segmentator/structs/segment_span.h>

#include <library/cpp/charset/doccodes.h>
#include <library/cpp/html/face/propface.h>
#include <library/cpp/html/html5/parse.h>
#include <library/cpp/html/zoneconf/ht_conf.h>
#include <library/cpp/langs/langs.h> 
#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/numerator/blob/numserializer.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>

#include <util/generic/buffer.h>
#include <util/generic/string.h>
#include <util/stream/file.h>

namespace {
    TBuffer ComposeNumeratorEvents(const TStringBuf& url, const TString& htmlPath, const ECharset enc) {
        // Parser
        const TMappedFileInput mappedFile(htmlPath);
        const TString html(mappedFile.Buf(), mappedFile.Avail());
        NHtml::THtmlChunksWriter chunks;
        NHtml5::ParseHtml(html, &chunks);

        //Numerator
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

        return SerializeNumeratorEvents(chunksRefs, buf, props);
    }

    template <typename T>
    bool IsEqualProtos(const T& proto1, const T& proto2) {
        google::protobuf::util::MessageDifferencer differencer;
        return differencer.Equals(proto1, proto2);
    }

    template <typename TMessage,
              typename = std::enable_if_t<std::is_base_of<google::protobuf::Message, TMessage>::value>>
    void CheckResult(const TVector<TSimpleSharedPtr<TMessage>>& protos, const TString& path) {
        TFileInput input(path);
        TString line;
        size_t productsAmount = FromString<size_t>(input.ReadLine());
        UNIT_ASSERT(productsAmount == protos.size());
        for (size_t lineNum = 0; input.ReadLine(line); ++lineNum) {
            const TMessage& canonized = NProtobufJson::Json2Proto<TMessage>(line);
            UNIT_ASSERT(IsEqualProtos(*protos[lineNum], canonized));
        }
    }

   template <typename TMessage,
              typename = std::enable_if_t<std::is_base_of<google::protobuf::Message, TMessage>::value>>
    void CheckLinksResult(const TMessage& proto, const TString& path) {
        TFileInput input(path);
        const TMessage& canonized = NProtobufJson::Json2Proto<TMessage>(input);
        UNIT_ASSERT(IsEqualProtos(proto, canonized));
    }

    template <typename TMessage,
              typename = std::enable_if_t<std::is_base_of<google::protobuf::Message, TMessage>::value>>
    void PrintVectorWithProto2Json(const TVector<TSimpleSharedPtr<TMessage>>& protos) {
        Cout << protos.size() << Endl;
        for (const auto& proto : protos) {
            NProtobufJson::Proto2Json(*proto, Cout);
            Cout << Endl;
        }
    }
}

Y_UNIT_TEST_SUITE(TParseSchemaOrgTest) {
    Y_UNIT_TEST(Lamoda1) {
        TString url = "https://www.lamoda.ru/p/be032ewrux60/clothes-bestia-dzhemper/?rec_name=rnd_recommendations&rec_place=home";
        TString htmlPath = "htmls/lamoda1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/lamoda1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Lamoda2) {
        TString url = "https://www.lamoda.ru/p/je008ewxpi31/clothes-jennyfer-dzhinsy/";
        TString htmlPath = "htmls/lamoda2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/lamoda2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Lamoda3) {
        TString url = "https://www.lamoda.ru/p/ta037gwhred4/scarfs-taifun-palantin/";
        TString htmlPath = "htmls/lamoda3.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/lamoda3.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Lamoda4) {
        TString url = "https://www.lamoda.ru/p/kr007emeusi1/clothes-krakatau-kurtka/";
        TString htmlPath = "htmls/lamoda4.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/lamoda4.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Lamoda1_Links) {
        TString url = "https://www.lamoda.ru/p/be032ewrux60/clothes-bestia-dzhemper/?rec_name=rnd_recommendations&rec_place=home";
        TString htmlPath = "htmls/lamoda1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        const auto links = NImages::NSchemaOrg::MakeLinks(products[0]);
        NProtobufJson::Proto2Json(links, Cout);
        CheckLinksResult(links, "htmls/lamoda1_links.json");
    }

    Y_UNIT_TEST(Lamoda2_Links) {
        TString url = "https://www.lamoda.ru/p/je008ewxpi31/clothes-jennyfer-dzhinsy/";
        TString htmlPath = "htmls/lamoda2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        const auto links = NImages::NSchemaOrg::MakeLinks(products[0]);
        NProtobufJson::Proto2Json(links, Cout);
        CheckLinksResult(links, "htmls/lamoda2_links.json");
    }

    Y_UNIT_TEST(Beru1) {
        TString url = "https://beru.ru/product/vesy-elektronnye-marta-mt-1678-vesennie-tsvety/661046022?show-uid=15925925068689174515906009&offerid=aYv_oLxqzy457RoPu6VQaw";
        TString htmlPath = "htmls/beru1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/beru1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Beru2) {
        TString url = "https://beru.ru/product/smartfon-apple-iphone-11-64gb-fioletovyi-mwlx2ru-a/100773347839?show-uid=15941380550054722247506001&offerid=y60fA--Cjs6XX6ndN-9BVw";
        TString htmlPath = "htmls/beru2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/beru2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Goods1) {
        TString url = "https://goods.ru/catalog/details/mnogofunkcionalnyy-gel-dlya-lica-i-tela-royal-skin-s-95-soderzhaniem-aloe-300-ml-100013203146/";
        TString htmlPath = "htmls/goods1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/goods1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Goods2) {
        TString url = "https://goods.ru/catalog/kofemashiny/";
        TString htmlPath = "htmls/goods2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/goods2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Goods3) {
        TString url = "https://goods.ru/catalog/details/kofemashina-avtomaticheskaya-philips-ep1220-00-100024556822/";
        TString htmlPath = "htmls/goods3.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/goods3.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Goods4) {
        TString url = "https://goods.ru/catalog/details/smartfon-apple-iphone-se-64gb-product-red-mx9u2ru-a-100026543224/";
        TString htmlPath = "htmls/goods4.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/goods4.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Mvideo1) {
        TString url = "https://www.mvideo.ru/products/skovoroda-tefal-illico-26sm-g7010514-50050634";
        TString htmlPath = "htmls/mvideo1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/mvideo1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Mvideo2) {
        TString url = "https://www.mvideo.ru/products/smartfon-apple-iphone-se-2020-64gb-white-mx9t2ru-a-30049496";
        TString htmlPath = "htmls/mvideo2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/mvideo2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Avito1) {
        TString url = "https://www.avito.ru/moskva/tovary_dlya_detey_i_igrushki/konstruktor_lego_tehnik_rally_car_sobrannaya_mashina_1949896466";
        TString htmlPath = "htmls/avito1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/avito1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Avito2) {
        TString url = "https://www.avito.ru/moskva/tovary_dlya_detey_i_igrushki/kukly_monster_high_1217759931";
        TString htmlPath = "htmls/avito2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/avito2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Kupivip1) {
        TString url = "https://www.kupivip.ru/product/w19091157042/helmidge-palto";
        TString htmlPath = "htmls/kupivip1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/kupivip1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Kupivip2) {
        TString url = "https://www.kupivip.ru/product/w18080284041/seacare-omolazhivayuschii_nochnoi_krem_dlya_litsa_s_vitaminami_a_e_koenzimom_q10_i_mineralami_mertvogo_morya_50_m_seacare";
        TString htmlPath = "htmls/kupivip2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/kupivip2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Eldorado1) {
        TString url = "https://www.eldorado.ru/cat/detail/smartfon-huawei-p40-lite-e-midnight-black-art-l29/";
        TString htmlPath = "htmls/eldorado1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/eldorado1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Eldorado2) {
        TString url = "https://www.eldorado.ru/cat/detail/pylesos-philips-fc9569-01-powerpro-active/";
        TString htmlPath = "htmls/eldorado2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/eldorado2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Citilink1) {
        TString url = "https://www.citilink.ru/catalog/mobile/cell_phones/1178784/";
        TString htmlPath = "htmls/citilink1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/citilink1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Citilink2) {
        TString url = "https://www.citilink.ru/catalog/large_and_small_appliances/large_appliances/refrigerators/326917/";
        TString htmlPath = "htmls/citilink2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/citilink2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(Tiu1) {
        TString url = "https://moskva.tiu.ru/Posuda-i-utvar";
        TString htmlPath = "htmls/tiu1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/tiu1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(ProblemPrefix1) {
        TString url = "https://rose-market.ru/buket-21-roza-dzhumiliya/";
        TString htmlPath = "htmls/problem_prefix1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/problem_prefix1.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(ProblemPrefix2) {
        TString url = "https://moskva.tiu.ru/Posuda-i-utvar";
        TString htmlPath = "htmls/problem_prefix2.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/problem_prefix2.json";
        CheckResult(products, canonizedResult);
    }

    Y_UNIT_TEST(PriceWithSpaces1) {
        TString url = "https://ashrussia.ru/zhenskie_chernye_krossovki_ash_addict_i9727700000";
        TString htmlPath = "htmls/price_with_spaces1.html";
        auto buffer = ComposeNumeratorEvents(url, htmlPath, CODES_UTF8);
        TNumeratorEvents events(buffer);
        const auto products = NImages::NSchemaOrg::ParseProduct(url, events);
        PrintVectorWithProto2Json(products);
        const TString canonizedResult = "htmls/price_with_spaces1.json";
        CheckResult(products, canonizedResult);
    }
}

