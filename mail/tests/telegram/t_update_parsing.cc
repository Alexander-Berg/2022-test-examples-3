#include <catch.hpp>

#include <messenger/telegram/module.h>
#include <ymod_httpclient/cluster_client.h>
#include <ymod_webserver/server.h>
#include <yplatform/application/repository.h>

namespace botserver::messenger {

struct fake_deps : yplatform::module
{
    template <typename Handler>
    void bind(string, vector<string>, Handler)
    {
    }

    void async_run(auto /*ctx*/, auto /*req*/, auto handler)
    {
        handler(error_code(), yhttp::response{ .status = 200, .body = "{\"ok\": true }" });
    }
};

using telegram_module = telegram::module_impl<fake_deps, fake_deps>;

struct t_telegram_update_parsing
{
    boost::asio::io_service io;
    yplatform::ptree conf;
    shared_ptr<fake_deps> deps_module = make_shared<fake_deps>();
    shared_ptr<telegram_module> module;

    botpeer peer;
    gate_message_ptr message;
    task_context_ptr ctx = boost::make_shared<task_context>();

    telegram::settings settings;

    t_telegram_update_parsing()
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<fake_deps>("web_server", deps_module);
        repo->add_service<fake_deps>("telegram_client", deps_module);

        settings.link_template = "https://t.me/%1%";
        module = make_shared<telegram_module>(settings);
        module->settings.allowed_attachment_types = {
            "document", "photo", "voice", "video_note", "sticker"
        };
        module->settings.russian_locale_languages = { "ru" };
        module->set_message_handler([this](auto /*ctx*/, auto peer, auto message) {
            this->peer = peer;
            this->message = message;
            return make_ready_future();
        });
    }

    json_value make_update(std::string text)
    {
        json_value res;
        if (auto err = res.parse(text))
        {
            throw runtime_error(*err);
        }
        return res;
    }

    future<void> make_ready_future()
    {
        promise<void> res;
        res.set();
        return res;
    }
};

TEST_CASE_METHOD(t_telegram_update_parsing, "disable_processing")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "text": "Hello, world!"
        }
    })JSON");

    module->disable_processing(ctx, hours(1));
    module->process_update(ctx, update).get();
    REQUIRE(!message);
    REQUIRE(peer.chat_id.empty());
}

TEST_CASE_METHOD(t_telegram_update_parsing, "simple_text_message")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "text": "Hello, world!"
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(peer.chat_id == "test_chat_id");
    REQUIRE(message);
    REQUIRE(message->received_date == 100500);
    REQUIRE(message->text == "Hello, world!");
}

TEST_CASE_METHOD(t_telegram_update_parsing, "forwarded_from_user")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "forward_from": {
                "first_name": "Anton",
                "last_name": "Nazin",
                "username": "anton_nazin"
            }
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->forwarded_from->name == "Anton Nazin");
    REQUIRE(message->forwarded_from->login == "anton_nazin");
    REQUIRE(message->forwarded_from->profile_link == "https://t.me/anton_nazin");
}

TEST_CASE_METHOD(t_telegram_update_parsing, "forwarded_from_chat")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "forward_from_chat": {
                "title": "PlayStation",
                "username": "psprices_ru_ps4"
            }
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->forwarded_from->name == "PlayStation");
    REQUIRE(message->forwarded_from->login == "psprices_ru_ps4");
    REQUIRE(message->forwarded_from->profile_link == "https://t.me/psprices_ru_ps4");
}

TEST_CASE_METHOD(t_telegram_update_parsing, "document_with_caption")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "document": {
                "file_id": "test_file_id"
            },
            "caption": "Message text"
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(peer.chat_id == "test_chat_id");
    REQUIRE(message);
    REQUIRE(message->received_date == 100500);
    REQUIRE(message->text == "Message text");
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id");
}

TEST_CASE_METHOD(t_telegram_update_parsing, "biggest_photo")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "photo": [
                {
                    "file_id": "test_file_id_low_res",
                    "width": 240
                },
                {
                    "file_id": "test_file_id_high_res",
                    "width": 1080
                },
                {
                    "file_id": "test_file_id_mid_res",
                    "width": 800
                }
            ]
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(peer.chat_id == "test_chat_id");
    REQUIRE(message);
    REQUIRE(message->received_date == 100500);
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id_high_res");
}

TEST_CASE_METHOD(t_telegram_update_parsing, "photo_meta")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "photo": [
                {
                    "file_id": "test_file_id",
                    "width": 240,
                    "file_size": 44226
                }
            ]
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id");
    REQUIRE(message->attachments.front().file_name == "photo.jpg");
    REQUIRE(message->attachments.front().mime_type == "image/jpeg");
    REQUIRE(message->attachments.front().size == 44226);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "voice_meta")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "voice": {
                "file_id": "test_file_id",
                "mime_type": "audio/ogg",
                "file_size": 11952
            }
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id");
    REQUIRE(message->attachments.front().file_name == "voice.ogg");
    REQUIRE(message->attachments.front().mime_type == "audio/ogg");
    REQUIRE(message->attachments.front().size == 11952);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "video_note")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "video_note": {
                "file_id": "test_file_id",
                "file_size": 139667
            }
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id");
    REQUIRE(message->attachments.front().file_name == "video_note");
    REQUIRE(message->attachments.front().mime_type == "");
    REQUIRE(message->attachments.front().size == 139667);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "sticker")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "sticker": {
                "file_id": "test_file_id",
                "file_size": 139667
            }
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message);
    REQUIRE(message->attachments.size() == 1);
    REQUIRE(message->attachments.front().id == "test_file_id");
    REQUIRE(message->attachments.front().file_name == "sticker");
    REQUIRE(message->attachments.front().mime_type == "");
    REQUIRE(message->attachments.front().size == 139667);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "default_language")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "date": 100500,
            "text": "Hello, world!"
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message->lang == i18n::language::ru);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "en_language_parsing")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "from": {
                "language_code": "en"
            },
            "date": 100500,
            "text": "Hello, world!"
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message->lang == i18n::language::en);
}

TEST_CASE_METHOD(t_telegram_update_parsing, "ru_language_parsing")
{
    auto update = make_update(R"JSON({
        "message": {
            "chat": {
                "id": "test_chat_id"
            },
            "from": {
                "language_code": "ru"
            },
            "date": 100500,
            "text": "Hello, world!"
        }
    })JSON");

    module->process_update(ctx, update).get();
    REQUIRE(message->lang == i18n::language::ru);
}

}
