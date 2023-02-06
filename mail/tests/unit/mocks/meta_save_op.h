#pragma once

#include <mail/notsolitesrv/src/meta_save_op/imeta_save_op.h>

#include <gmock/gmock.h>

class TMetaSaveOpMock : public NNotSoLiteSrv::NMetaSaveOp::IMetaSaveOp {
public:
    MOCK_METHOD(
        void,
        SetOpParams,
        (NNotSoLiteSrv::NMetaSaveOp::TRequest,
         NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpCallback),
        (override));

    MOCK_METHOD(
        void,
        Call,
        (NNotSoLiteSrv::NMetaSaveOp::IMetaSaveOp::TYieldCtx,
         NNotSoLiteSrv::TErrorCode,
         NNotSoLiteSrv::NMetaSaveOp::IMetaSaveOp::TResult),
        (const));

    void operator()(
        NNotSoLiteSrv::NMetaSaveOp::IMetaSaveOp::TYieldCtx yieldCtx,
        NNotSoLiteSrv::TErrorCode errorCode = {},
        NNotSoLiteSrv::NMetaSaveOp::IMetaSaveOp::TResult result = {}) override
    {
        Call(yieldCtx, errorCode, result);
    }

    void SetCallback(const NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpCallback& callback) {
        Callback = callback;
    }

    NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpCallback& GetCallback() {
        return Callback;
    }

private:
    NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpCallback Callback;
};
