#include <mail/ymod_python/example/yplatform_mod/include/example.h>

namespace NExample {

class TTest : public yplatform::module
{
public:
    void init() { }

    void start(){
        boost::asio::io_service& io_service = *Reactor.io();
        // string -> json
        boost::asio::post(io_service, [&] {
            boost::this_thread::sleep_for(boost::chrono::milliseconds(1000));
            std::string json_string{"{\"value\":123}"};
            // вызов функции из Example модуля, которая потом вызовет
            // питоновскую функцию
            Example->StringToJsonValue(json_string, [&](const NJson::TJsonValue& json){
                // текущий колбэк исполняется в питоновском потоке под GIL
                // поэтому, чтобы не держать долго GIL, завершим колбэк асинхронно
                // в своём реакторе
                boost::asio::post(io_service, [json] { // json передаём копированием
                                                       // почему бы не протащить из Example->StringToJsonValue
                                                       // json ввиде shared_ptr?
                                                       // проблема в том, что буст использует custom_deleter
                                                       // который в свою очередь вызывает decref у PyObject
                                                       // и тут кроются грабли.
                                                       // подробнее смотри mail/ymod_python/README.md
                    // тут выводим то, что вернул питон
                    std::cout << "from c++: string field = " << json["string"].GetString() << std::endl;
                });
            });
        });

        // json -> string
        boost::asio::post(io_service, [&] {
            boost::this_thread::sleep_for(boost::chrono::milliseconds(2000));
            auto json = std::make_shared<NJson::TJsonValue>(NJson::JSON_MAP);
            (*json)["value1"] = 123;
            (*json)["value2"] = "str";
            Example->JsonValueToString(json, [&](const std::string& str){
                boost::asio::post(io_service, [this, str] {
                    std::cout << "from c++: json to string = " << str << std::endl;
                    Example->TestPyDefFromCpp();
                });
            });
        });
    }

    void stop() { }

    void fini() { }

    TTest(yplatform::reactor& reactor, const yplatform::ptree&)
        : Reactor(reactor)
    {
        Example = yplatform::find<NExample::TExample, std::shared_ptr>("example");
    }

    std::shared_ptr<NExample::TExample> Example;
    yplatform::reactor& Reactor;
};

} // namespace NExample

#include <yplatform/module_registration.h>
REGISTER_MODULE(NExample::TTest)
