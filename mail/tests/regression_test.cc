#include <mimeparser/Mulca.h>
#include <boost/thread.hpp>
#include <boost/program_options.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/filesystem.hpp>
#include <boost/thread/condition_variable.hpp>
#include <iostream>
#include <fstream>
#include <memory>

namespace fs=boost::filesystem;
namespace po=boost::program_options;
using namespace boost;

namespace {
const unsigned int DEFAULT_THREAD_NUMBER=4;
boost::condition_variable cond;
boost::mutex mut;
unsigned int finishedTasksCount;
}

namespace {
namespace copypaste {
template<class T>
typename po::typed_value<T>* opt(T& t)
{
    return po::value<T>(&t)->default_value(t);
}
}

fs::path getDefaultTestFolderBase(const std::string& argv0)
{
    fs::path path(fs::canonical(argv0));
    return (path.parent_path().parent_path()/"data");
}

struct Options {
    Options(const std::string& argv0)
        : messagesPath((getDefaultTestFolderBase(argv0)/"messages").string())
        , metaPath((getDefaultTestFolderBase(argv0)/"meta").string())
        , threadNumber(DEFAULT_THREAD_NUMBER)
    {}
    std::string messagesPath;
    std::string metaPath;
    unsigned int threadNumber;
};

Options parseOptions(int argc, char* argv[])
{
    using namespace copypaste;
    Options result(argv[0]);
    po::variables_map vm;
    po::options_description description("Regression test options");
    description.add_options()
    ("help", "produce help message")
    ("messagesPath", opt(result.messagesPath), "Path of messages files")
    ("metaPath", opt(result.metaPath), "Path of meta files")
    ("threadNumber", opt(result.threadNumber), "Number of threads");
    po::command_line_parser parser(argc, argv);
    po::store(parser.options(description).run(), vm);
    po::notify(vm);
    if (vm.count("help")) {
        std::cerr << description << std::endl;
        throw std::runtime_error("RTFM");
    }
    return result;
}

class Task {
public:
    typedef void result_type;
public:
    Task(const fs::path& message, const fs::path& meta)
        : message_(message)
        , meta_(meta)
        , result_(false) {
    }
    void operator()() {
        using MimeParser::Mulca::parse_message;
        try {
            std::string newMeta=parse_message(readFile(message_.string()));
            std::string meta=readFile(meta_.string());
            if (meta!=newMeta) {
                std::string error("Parse difference: ");
                error+=message_.string();
                error+=" and ";
                error+=meta_.string();
                throw std::runtime_error(error);
            }
        } catch (const std::exception& e) {
            information_=e.what();
            throw;
        } catch (...) {
            information_="Unknown exception";
            throw;
        }
        {
            boost::lock_guard<boost::mutex> lock(mut);
            --finishedTasksCount;
        }
        cond.notify_one();
    }
    bool result() const {
        return result_;
    }
    const std::string& information() const {
        return information_;
    }
private:
    static std::string readFile(const std::string& filename) {
        typedef std::istreambuf_iterator<char> BufIterator;
        std::ifstream stream(filename.c_str());
        return std::string(BufIterator(stream), BufIterator());
    }
private:
    fs::path message_;
    fs::path meta_;
    bool result_;
    std::string information_;
};

std::unique_ptr<std::vector<Task> > enqueueTasks(const Options& options)
{
    typedef fs::directory_iterator Iterator;
    std::unique_ptr<std::vector<Task> > result(new std::vector<Task>());
    fs::path messagesFolder(options.messagesPath);
    Iterator end;
    for (Iterator it(messagesFolder) ; it != end ; ++it) {
        fs::path path(*it);
        std::string name=path.filename().string();
        fs::path meta(options.metaPath);
        meta/=name;
        Task task(*it, meta.replace_extension(".xml"));
        result->push_back(task);
    }
    return result;
}

}

void runTest(const Options& options)
{
    asio::io_service io_service;
    asio::io_service::work work(io_service);
    boost::thread_group threads;
    for (std::size_t i = 0; i < options.threadNumber; ++i) {
        threads.create_thread(boost::bind(&asio::io_service::run, &io_service));
    }
    std::unique_ptr<std::vector<Task> > taskVector=enqueueTasks(options);
    finishedTasksCount = static_cast<unsigned int>(taskVector->size());
    for (unsigned int i=0; i!=taskVector->size(); ++i) {
        io_service.post(boost::bind((*taskVector)[i]));
    }
    boost::unique_lock<boost::mutex> lock(mut);
    while (finishedTasksCount > 0) {
        cond.wait(lock);
    }
    io_service.stop();
    threads.join_all();
}

int main(int argc, char* argv[])
{
    Options options=parseOptions(argc, argv);
    runTest(options);
}
