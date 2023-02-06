#include <market/library/shiny/cli/test_client/test_client.h>
namespace NMarket::NShiny::NTestClient {

    int TCommand11::Run(TConfig& config) {
        Y_UNUSED(config);
        CallBack.Run("command11", User.RootOption, User.Folder1Option, User.Command11Option);
        return EXIT_SUCCESS;
    }

    void TCommand11::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('c', "command-option", "command 11 option").StoreResult(&User.Command11Option).RequiredArgument().Optional();
        config.Opts->AddFreeArgBinding("free-arg", User.FreeArg, "free arg demo");
    }

    TCommand11::TCommand11(const ICallBack& callback)
        : TBase("c1", { "c11" }, "command 1 from folder 1")
        , CallBack(callback)
    {}

    int TCommand12::Run(TConfig& config) {
        Y_UNUSED(config);
        CallBack.Run("command12", User.RootOption, User.Folder1Option, User.Command12Option);
        return EXIT_SUCCESS;
    }

    void TCommand12::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('c', "command-option", "command 12 option").StoreResult(&User.Command12Option).RequiredArgument().Optional();
        config.SetFreeArgsNum(0);
    }

    TCommand12::TCommand12(const ICallBack& callback)
        : TBase("c2", { "c12" }, "command 2 from folder 1")
        , CallBack(callback)
    {}

    void TFolder1::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('f', "folder-option", "folder 1 option").StoreResult(&User.Folder1Option).RequiredArgument().Optional();
    }

    TFolder1::TFolder1(const ICallBack& callback)
        : TBase("f1", { "folder1" }, "folder 1")
    {
        AddCommand(MakeIntrusive<TCommand11>(callback));
        AddCommand(MakeIntrusive<TCommand12>(callback));
    }

    int TCommand21::Run(TConfig& config) {
        Y_UNUSED(config);
        CallBack.Run("command21", User.RootOption, User.Folder2Option, User.Command21Option);
        return EXIT_SUCCESS;
    }

    void TCommand21::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('c', "command-option", "command 21 option").StoreResult(&User.Command21Option).RequiredArgument().Optional();
        config.SetFreeArgsNum(0);
    }

    TCommand21::TCommand21(const ICallBack& callback)
        : TBase("c1", { "c21" }, "command 1 from folder 2")
        , CallBack(callback)
    {}

    int TCommand22::Run(TConfig& config) {
        Y_UNUSED(config);
        CallBack.Run("command22", User.RootOption, User.Folder2Option, User.Command22Option);
        return EXIT_SUCCESS;
    }

    void TCommand22::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('c', "command-option", "command 22 option").StoreResult(&User.Command22Option).RequiredArgument().Optional();
        config.SetFreeArgsNum(0);
    }

    TCommand22::TCommand22(const ICallBack& callback)
        : TBase("c2", { "c22" }, "command 2 from folder 2")
        , CallBack(callback)
    {}

    void TFolder2::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('f', "folder-option", "folder 2 option").StoreResult(&User.Folder2Option).RequiredArgument().Optional();
    }

    TFolder2::TFolder2(const ICallBack& callback)
        : TBase("f2", { "folder2" }, "folder 2")
    {
        AddCommand(MakeIntrusive<TCommand21>(callback));
        AddCommand(MakeIntrusive<TCommand22>(callback));
    }

    void TRoot::Config(TConfig& config) {
        TBase::Config(config);
        config.Opts->AddLongOption('r', "root-option", "root option").StoreResult(&User.RootOption).RequiredArgument().Optional();
    }

    TRoot::TRoot(const ICallBack& callback)
        : TBase("Test Client", "root command")
    {
        AddCommand(MakeIntrusive<TFolder1>(callback));
        AddCommand(MakeIntrusive<TFolder2>(callback));
    }

}
