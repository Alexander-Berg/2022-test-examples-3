#pragma once

#include <market/library/shiny/cli/command.h>

namespace NMarket::NShiny::NTestClient {
    struct TRootUserData {
        int RootOption = 0;
    };

    struct TFolder1UserData : public TRootUserData {
        int Folder1Option = 0;
    };

    struct TFolder2UserData : public TRootUserData {
        int Folder2Option = 0;
    };

    struct TCommand11UserData : public TFolder1UserData {
        int Command11Option = 0;
        int FreeArg = 1;
    };

    struct TCommand12UserData : public TFolder1UserData {
        int Command12Option = 0;
    };

    struct TCommand21UserData : public TFolder2UserData {
        int Command21Option = 0;
    };

    struct TCommand22UserData : public TFolder2UserData {
        int Command22Option = 0;
    };

    struct ICallBack {
        virtual ~ICallBack() = default;
        virtual void Run(TStringBuf name, int root, int folder, int command) const = 0;
    };

    class TCommand11 final : public TClientSubcommand<TFolder1UserData, TCommand11UserData> {
    public:
        using TBase = TClientSubcommand<TFolder1UserData, TCommand11UserData>;

        TCommand11(const ICallBack& callback);;
        void Config(TConfig& config) override;
        int Run(TConfig& config) override;

    private:
        const ICallBack& CallBack;
    };

    class TCommand12 final : public TClientSubcommand<TFolder1UserData, TCommand12UserData> {
    public:
        using TBase = TClientSubcommand<TFolder1UserData, TCommand12UserData>;

        TCommand12(const ICallBack& callback);;
        void Config(TConfig& config) override;
        int Run(TConfig& config) override;

    private:
        const ICallBack& CallBack;
    };

    class TFolder1 final : public TClientCommandSubtree<TRootUserData, TFolder1UserData> {
    public:
        using TBase = TClientCommandSubtree<TRootUserData, TFolder1UserData>;

        TFolder1(const ICallBack& callback);
        void Config(TConfig& config) override;
    };

    class TCommand21 final : public TClientSubcommand<TFolder2UserData, TCommand21UserData> {
    public:
        using TBase = TClientSubcommand<TFolder2UserData, TCommand21UserData>;

        TCommand21(const ICallBack& callback);;
        void Config(TConfig& config) override;
        int Run(TConfig& config) override;

    private:
        const ICallBack& CallBack;
    };

    class TCommand22 final : public TClientSubcommand<TFolder2UserData, TCommand22UserData> {
    public:
        using TBase = TClientSubcommand<TFolder2UserData, TCommand22UserData>;

        TCommand22(const ICallBack& callback);;
        void Config(TConfig& config) override;
        int Run(TConfig& config) override;

    private:
        const ICallBack& CallBack;
    };

    class TFolder2 final : public TClientCommandSubtree<TRootUserData, TFolder2UserData> {
    public:
        using TBase = TClientCommandSubtree<TRootUserData, TFolder2UserData>;

        TFolder2(const ICallBack& callback);
        void Config(TConfig& config) override;
    };

    class TRoot final : public TClientCommandRoot<TRootUserData> {
    public:
        using TBase = TClientCommandRoot<TRootUserData>;

        TRoot(const ICallBack& callback);
        void Config(TConfig& config) override;
    };
}
