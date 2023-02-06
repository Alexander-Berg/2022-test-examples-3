#include <xeno/xeno_impl.h>
#include <ymod_mdb_sharder/users_distributor.h>
#include <yplatform/module_registration.h>

namespace xeno {

namespace ph = std::placeholders;

class xeno_impl_testing : public xeno_impl
{
public:
    explicit xeno_impl_testing(yplatform::reactor& reactor) : xeno_impl(reactor)
    {
    }

    void init(const yplatform::ptree& conf)
    {
        xeno_impl::init(conf);
        yplatform::find<ymod_mdb_sharder::users_distributor>("users_distributor")
            ->subscribe(
                std::bind(
                    &xeno_impl_testing::on_acquire_accounts, shared_from(this), ph::_1, ph::_2),
                std::bind(&xeno_impl_testing::on_release_accounts, shared_from(this), ph::_2));
    }

protected:
    void on_acquire_accounts(const shard_id& shard_id, const uid_vector& uids)
    {
        uid_vector result;
        for (auto& uid : uids)
        {
            if (!settings_->forbidden_uids.count(uid))
            {
                result.push_back(uid);
            }
        }
        xeno_impl::on_acquire_accounts(shard_id, result);
    }

    void on_release_accounts(const uid_vector& uids)
    {
        xeno_impl::on_release_accounts(uids);
    }

    void load_user(uid_t uid, const shard_id& shard_id) override
    {
        if (settings_->forbidden_uids.count(uid))
        {
            return;
        }
        xeno_impl::load_user(uid, shard_id);
    }

    void update_user_karma(
        const shard_id& shard_id,
        uid_t uid,
        const karma_t& karma,
        const without_data_cb& cb) override
    {
        if (settings_->forbidden_uids.count(uid))
        {
            return cb(code::ok);
        }
        xeno_impl::update_user_karma(shard_id, uid, karma, cb);
    }
};

}

DEFINE_SERVICE_OBJECT(xeno::xeno_impl_testing);
