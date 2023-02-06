#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/common/env.h>

#include <market/idx/datacamp/proto/offer/OfferMeta.pb.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/idx/datacamp/proto/tests/test_field_type/proto/IncorrectType.pb.h>

#include <kernel/gazetteer/common/protohelpers.h>
#include <google/protobuf/compiler/importer.h>

#include <string>

std::string ErrorMessage(const TBasicString<char> &type_name, const TBasicString<char> &field_name) {
    return "Field \"" + field_name + "\" has wrong type \"" + type_name + "\".\n"
                                                                          "You have to use (Market.version_counter) option only "
                                                                          "with Market.DataCamp.VersionCounter type of field.\n"
                                                                          "Check https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/datacamp/proto/offer/OfferMeta.proto?rev=r8945602#L259\n";
}

bool CheckDescriptor(const NProtoBuf::Descriptor *message_descriptor) {
    for (int idx = 0; idx < message_descriptor->field_count(); ++idx) {
        const auto field = message_descriptor->field(idx);
        auto options = field->options();
        if (options.HasExtension(Market::version_counter)) {
            if (field->type() == google::protobuf::FieldDescriptor::TYPE_MESSAGE) {
                if (field->message_type()->full_name() != Market::DataCamp::VersionCounter::descriptor()->full_name()) {
                    Cerr << ErrorMessage(field->message_type()->full_name(), field->full_name());
                    return false;
                }
                // проверка вложенных сообщений
                if (!CheckDescriptor(field->message_type()))
                    return false;
            } else {
                Cerr << ErrorMessage(field->type_name(), field->full_name());
                return false;
            }
        }

    }
    return true;
}

/*
 Тест проверяет сообщение Market.DataCamp.Offer и его вложенные структуры.
 Поля, использующие опцию (Market.version_counter), должны быть
 типа Market.DataCamp.VersionCounter
*/
Y_UNIT_TEST_SUITE(TestMessageType) {
    Y_UNIT_TEST(TestVersionCounter) {
        Market::DataCamp::Offer message_to_check;
        UNIT_ASSERT(CheckDescriptor(message_to_check.descriptor()));
    }

    Y_UNIT_TEST(IncorrectType) {
        Market::DataCamp::IncorrectType message_to_check;
        UNIT_ASSERT(!CheckDescriptor(message_to_check.descriptor()));
    }
};
