#ifndef __THROW_WMI_HELPER_H
#define __THROW_WMI_HELPER_H

#define EXPECT_THROW_WMI(statement, errcode, message)   \
    try {                                               \
        statement;                                      \
        ADD_FAILURE();                                  \
    } catch(wmi_exception& e) {                         \
        EXPECT_STREQ(message, e.what());                \
        EXPECT_EQ(errcode, e.code());                   \
    }

#define ASSERT_THROW_WMI(statement, errcode, message)   \
    try {                                               \
        statement;                                      \
        FAIL();                                         \
    } catch(wmi_exception& e) {                         \
        EXPECT_STREQ(message, e.what());                \
        ASSERT_EQ(errcode, e.code());                   \
    }

#define EXPECT_THROW_SYS(statement, errcode, message)   \
    try {                                               \
        statement;                                      \
        ADD_FAILURE();                                  \
    } catch(const macs::system_error& e) {              \
        EXPECT_STREQ(message, e.what());                \
        EXPECT_EQ(errcode, e.code());                   \
    }

#define ASSERT_THROW_SYS(statement, errcode, message)   \
    try {                                               \
        statement;                                      \
        FAIL();                                         \
    } catch(const macs::system_error& e) {              \
        EXPECT_STREQ(message, e.what());                \
        ASSERT_EQ(errcode, e.code());                   \
    }

#endif
