#include <library/cpp/testing/unittest/registar.h>
#include <util/charset/wide.h>
#include <crypta/lib/native/identifiers/lib/generic.h>
#include <crypta/lib/native/identifiers/lib/id_types/all.h>
#include <crypta/graph/engine/proto/graph.pb.h>
#include <crypta/graph/engine/score/native/lib/utils/getters.h>

Y_UNIT_TEST_SUITE(TFillEmailsTestSuite) {
    Y_UNIT_TEST(TestEmpty) {
        NCrypta::NGraphEngine::TGraph graph;

        TVector<TString> emails;
        NCrypta::NGraphEngine::FillEmails(graph, &emails);

        UNIT_ASSERT_EQUAL(emails.size(), 0);
    }

    Y_UNIT_TEST(TestOne) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());

        TVector<TString> emails;
        NCrypta::NGraphEngine::FillEmails(graph, &emails);

        UNIT_ASSERT_EQUAL(emails.size(), 1);

        UNIT_ASSERT_EQUAL(emails[0], "sirius");
    }

    Y_UNIT_TEST(TestDifferentTypes) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@mail.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sir").ToProto());

        TVector<TString> emails;
        NCrypta::NGraphEngine::FillEmails(graph, &emails);
        Sort(emails.begin(), emails.end());

        UNIT_ASSERT_EQUAL(emails.size(), 2);

        UNIT_ASSERT_EQUAL(emails[0], "ris_1945");
        UNIT_ASSERT_EQUAL(emails[1], "sirius");
    }
}

Y_UNIT_TEST_SUITE(TFillLoginsTestSuite) {
    Y_UNIT_TEST(TestEmpty) {
        NCrypta::NGraphEngine::TGraph graph;

        TVector<TString> logins;
        NCrypta::NGraphEngine::FillEmails(graph, &logins);

        UNIT_ASSERT_EQUAL(logins.size(), 0);
    }

    Y_UNIT_TEST(TestOne) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());

        TVector<TString> logins;
        NCrypta::NGraphEngine::FillLogins(graph, &logins);

        UNIT_ASSERT_EQUAL(logins.size(), 1);

        UNIT_ASSERT_EQUAL(logins[0], "sirius");
    }

    Y_UNIT_TEST(TestDifferentTypes) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sir").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());

        TVector<TString> logins;
        NCrypta::NGraphEngine::FillLogins(graph, &logins);
        Sort(logins.begin(), logins.end());

        UNIT_ASSERT_EQUAL(logins.size(), 2);

        UNIT_ASSERT_EQUAL(logins[0], "ris-1945");
        UNIT_ASSERT_EQUAL(logins[1], "sir");
    }
}
