#include <robot/jupiter/protos/compatibility/web_factors.pb.h>
#include <market/idx/generation/indexerf/src/proto/IndexErf.pb.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/hash.h>

using TPbType = NProtoBuf::FieldDescriptor::Type;

/*
MARKETINDEXER-40252 -- индексатор падал из-за несовпадения схем.

Теперь таблица строится на основе THostErfInfoProto с добавлением поля Host типа string.
https://arcanum.yandex-team.ru/arc/trunk/arcadia/robot/jupiter/tools/export/lib/herf.cpp?rev=r8324452#L78
*/

// не универсально но достаточно, лучше ложное падение теста
bool IsSafeCast(TPbType from, TPbType to) {
    if (from == to) {
        return true;
    }
    if (from == NProtoBuf::FieldDescriptor::TYPE_UINT32 &&
        to == NProtoBuf::FieldDescriptor::TYPE_UINT64) {
        return true;
    }
    if (from == NProtoBuf::FieldDescriptor::TYPE_FLOAT &&
        to == NProtoBuf::FieldDescriptor::TYPE_DOUBLE) {
        return true;
    }
    return false;
}

Y_UNIT_TEST_SUITE(Compatibilty) {

    Y_UNIT_TEST(TestHerfSchema) {
        THashMap<TString, TPbType> original;

        {
            auto originalDescriptor = NJupiter::THostErfInfoProto::descriptor();
            for (int i = 0; i < originalDescriptor->field_count(); ++i) {
                TString name = originalDescriptor->field(i)->name();
                auto type = originalDescriptor->field(i)->type();
                original[name] = type;
            }
        }

        {
            auto descriptor = NMarket::IndexErf::THerfFeatures::descriptor();
            for (int i = 0; i < descriptor->field_count(); ++i) {
                TString name = descriptor->field(i)->name();
                auto type = descriptor->field(i)->type();

                if (name == "Host") {
                    UNIT_ASSERT_EQUAL(type, NProtoBuf::FieldDescriptor::TYPE_STRING);
                } else if (original.contains(name)) {
                    UNIT_ASSERT(IsSafeCast(original[name], type));
                } else {
                    Cerr << "No such field in original table: " << name << Endl;
                }
            }
        }
    }

    Y_UNIT_TEST(TestErfSchema) {
        THashMap<TString, TPbType> original;

        {   
            auto originalDescriptor = NJupiter::SDocErf2InfoProto::descriptor();
            for (int i = 0; i < originalDescriptor->field_count(); ++i) {
                TString name = originalDescriptor->field(i)->name();
                auto type = originalDescriptor->field(i)->type();
                original[name] = type;
            }
        }

        {
            auto descriptor = NMarket::IndexErf::TErfFeatures::descriptor();
            for (int i = 0; i < descriptor->field_count(); ++i) {
                TString name = descriptor->field(i)->name();
                auto type = descriptor->field(i)->type();

                if (name == "Host") {
                    UNIT_ASSERT_EQUAL(type, NProtoBuf::FieldDescriptor::TYPE_STRING);
                } else if (original.contains(name)) {
                    UNIT_ASSERT(IsSafeCast(original[name], type));
                } else { 
                    Cerr << "No such field in original table: " << name << Endl;
                }
            }
        }
    }

}
