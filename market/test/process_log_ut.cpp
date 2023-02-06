#include <market/library/process_log/process_log.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NProcessLog;


TEST(ProcessLog, FlushWithoutInit)
{
    // Check that we don't fail on Flush() even if the object has not been
    // properly initialized before

    struct TestFlush : TProcessLog {
        void Flush() {
            TProcessLog::Flush();
        }
    };

    TestFlush test;
    EXPECT_NO_THROW(test.Flush());
}


TEST(ProcessLog, WriteWithoutInit1)
{
    // Check that operator<< is safe even if the object has not been
    // properly initialized before

    TLogMessage msg("110", "Test message");
    EXPECT_NO_THROW(PLOG << msg);
}


TEST(ProcessLog, WriteWithoutInit2)
{
    // Check that Write() is safe even if the object has not been
    // properly initialized before

    TLogMessage msg("110", "Test message");
    EXPECT_NO_THROW(PLOG.WriteMessage(msg));
}


TEST(ProcessLog, DerivedLogger)
{
    // Check that we are able to create derived logger.
    // Expect that:
    //  1) TGlobalProcessLogRAII will call derived InitImpl(), FlushImpl() and DestroyImpl().
    //  2) Custom PrepareProtoLogMessage() function will be called during usual << operation.

    // Also test crash handler.

    // Simple and silly way to check whether Init() and Flush() has been called.
    static bool InitCalled = false, FlushCalled = false, OnCrashCalled = false, DestroyCalled = false;

    class TCustomLog : public TProcessLog {
    public:
        void SetGeneration(const TString& generation) {
            IndexerGeneration = generation;
        }

        static TCustomLog& Instance() {
            return *Singleton<TCustomLog>();
        }

        void InitImpl() override {
            InitCalled = true;
        }

        void FlushImpl() override {
            FlushCalled = true;
        }

        void DestroyImpl() override {
            DestroyCalled = true;
        }

    protected:
        void PreprocessMessageImpl(const TLogMessage& msg) const override {
            msg.YtLogMsg.set_indexer_generation(IndexerGeneration);
            msg.FillProto();

            throw NYT::TYtError(10, "Test error");
        }

        void ActOnFailureImpl(const TString&) override {
            OnCrashCalled = true;
        }

    private:
        TString IndexerGeneration;
    };

    // Test body.

    {
        TString CrashMessage = "";

        TLogMessage msg("110", "Test");

        TGlobalProcessLogRAII<TCustomLog> RAII(true,
                TProcessLogOptions()
                    .CrashOnFailure(false)
        );
        ASSERT_TRUE(InitCalled);

        TCustomLog::Instance().SetGeneration("123456");
        EXPECT_NO_THROW(TCustomLog::Instance() << msg);

        ASSERT_EQ("123456", msg.YtLogMsg.indexer_generation());
        ASSERT_EQ("Test", msg.YtLogMsg.text());
        ASSERT_TRUE(OnCrashCalled);
    }

    ASSERT_TRUE(FlushCalled);
    ASSERT_TRUE(DestroyCalled);
}
