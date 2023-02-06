#ifndef MACS_TESTS_USER_JOURNAL_MOCK_H_
#define MACS_TESTS_USER_JOURNAL_MOCK_H_

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/user_journal.h>
#include <user_journal/journal.h>
#include <user_journal/parameters/message.h>

using namespace user_journal;
using namespace user_journal::parameters;

struct UserJournalMock : public macs::UserJournal {
    MOCK_METHOD(void, asyncGetShardName, (std::function<void (std::string)>), (const, override));

    UserJournalMock(Journal journal) : macs::UserJournal(journal) {}
};

struct RequestParametersMock : public RequestParameters {
    MOCK_METHOD(void, map, (const Mapper&), (const, override));
    MOCK_METHOD(const std::string&, uid, (), (const, override));
};

struct WriterMock : public Writer {
    MOCK_METHOD(void, write, (const std::string&, const Entry&), (const, override));
};

struct MapperMock : public Mapper {
    MOCK_METHOD(void, mapValue, (const Operation & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Target & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::string & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const Date & v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (bool v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (int v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (size_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (std::time_t v, const std::string & name) , (const, override));
    MOCK_METHOD(void, mapValue, (const std::vector<std::string> & v, const std::string & name) , (const, override));
};


#endif // MACS_TESTS_USER_JOURNAL_MOCK_H_
