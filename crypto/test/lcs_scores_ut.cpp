#include <library/cpp/testing/unittest/registar.h>
#include <util/charset/wide.h>
#include <crypta/lib/native/identifiers/lib/id_types/all.h>
#include <crypta/graph/engine/proto/graph.pb.h>
#include <crypta/graph/engine/score/native/lib/lcs_scores.h>

// ============ Email Tests
Y_UNIT_TEST_SUITE(TEmailsSimpleLcsScoreTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsSimpleLcsScoringStrategy emailSimpleScore;
        double score = emailSimpleScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestTheSameHost) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());

        NCrypta::NGraphEngine::TEmailsSimpleLcsScoringStrategy emailSimpleScore;
        double score = emailSimpleScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 2.0);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsSimpleLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5.0);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsSimpleLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }
}

Y_UNIT_TEST_SUITE(TEmailsAverageBestLcsTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsAverageBestLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestTheSameHost) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());

        NCrypta::NGraphEngine::TEmailsAverageBestLcsScoringStrategy emailSimpleScore;
        double score = emailSimpleScore.ComputeLcsScore(graph);

        UNIT_ASSERT_DOUBLES_EQUAL(score, 2.666666666, 1e-9);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsAverageBestLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5.);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsAverageBestLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 3.75);
    }
}

Y_UNIT_TEST_SUITE(TEmailsMedianPairwiseLcsTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsMedianPairwiseLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestTheSameHost) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());

        NCrypta::NGraphEngine::TEmailsMedianPairwiseLcsScoringStrategy emailSimpleScore;
        double score = emailSimpleScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 2.);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsMedianPairwiseLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5.);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris@gmail.com").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("sirius@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("ris_1945@ya.ru").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TEmail("1945_1945@mail.ru").ToProto());

        NCrypta::NGraphEngine::TEmailsMedianPairwiseLcsScoringStrategy emailScore;
        double score = emailScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 2.);
    }
}

// ============ Login Tests
Y_UNIT_TEST_SUITE(TLoginsSimpleLcsScoreTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());

        NCrypta::NGraphEngine::TLoginsSimpleLcsScoringStrategy LoginSimpleScore;
        double score = LoginSimpleScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsSimpleLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5.0);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsSimpleLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }
}

Y_UNIT_TEST_SUITE(TLoginsAverageBestLcsTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());

        NCrypta::NGraphEngine::TLoginsAverageBestLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsAverageBestLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsAverageBestLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 3.75);
    }
}

Y_UNIT_TEST_SUITE(TLoginsMedianPairwiseLcsTestSuite) {
    Y_UNIT_TEST(TestSmallSize) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());

        NCrypta::NGraphEngine::TLoginsMedianPairwiseLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 0.0);
    }

    Y_UNIT_TEST(TestEndLcs) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsMedianPairwiseLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 5.);
    }

    Y_UNIT_TEST(TestSeveral) {
        NCrypta::NGraphEngine::TGraph graph;
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("sirius").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("ris-1945").ToProto());
        graph.AddVertices()->MergeFrom(NIdentifiers::TLogin("a1945-1945").ToProto());

        NCrypta::NGraphEngine::TLoginsMedianPairwiseLcsScoringStrategy LoginScore;
        double score = LoginScore.ComputeLcsScore(graph);

        UNIT_ASSERT_EQUAL(score, 2.0);
    }
}
