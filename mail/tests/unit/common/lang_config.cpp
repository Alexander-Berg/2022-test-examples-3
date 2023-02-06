#include "lang_config.h"

#include <common/lang_config.h>
#include <common/imap_context.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/xml_parser.hpp>

const std::string DEFAULT_LANGUAGE_CONFIG =
    "<language_options force_rename=\"0\" force_localize=\"0\">"
    "    <ru>"
    "        <inbox  xlist=\"\\Inbox\"  name=\"INBOX\" />"
    "        <draft  xlist=\"\\Drafts\" name=\"Черновики\" />"
    "        <spam   xlist=\"\\Spam\"   name=\"Спам\" />"
    "        <sent   xlist=\"\\Sent\"   name=\"Отправленные\" />"
    "        <trash  xlist=\"\\Trash\"  name=\"Удаленные\" />"
    "        <outbox name=\"Исходящие\" />"
    "    </ru>"
    "</language_options>";

const static bool RENAME_ENABLED = true;
const static bool RENAME_DISABLED = false;
const static bool LOCALIZE_ENABLED = true;
const static bool LOCALIZE_DISABLED = false;

boost::property_tree::ptree makePtree(const std::string& xml)
{
    std::stringstream xmlstream;
    xmlstream << xml;
    boost::property_tree::ptree result;
    boost::property_tree::read_xml(xmlstream, result);

    return result;
}

yimap::LanguageConfig makeLanguageConfig(
    const std::string& serverConfig,
    bool renameEnabled,
    bool localizeImap,
    const std::string& userLanguage)
{
    auto serverPtree = makePtree(serverConfig);
    yimap::UserSettings settings;
    settings.imapEnabled = true;
    settings.renameEnabled = renameEnabled;
    settings.localizeImap = localizeImap;
    return yimap::LanguageConfig(serverPtree.get_child("language_options"), settings, userLanguage);
}

//-----------------------------------------------------------------------------
// Russian users

TEST(Localization, RU_USER_LOCALIZE)
{
    auto ruUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_ENABLED, LOCALIZE_ENABLED, "ru");

    EXPECT_EQ(ruUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("draft"), "Черновики");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("sent"), "Отправленные");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("spam"), "Спам");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("trash"), "Удаленные");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("outbox"), "Исходящие");

    EXPECT_EQ(ruUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("outbox"), "");
}

TEST(Localization, RU_USER_RENAME)
{
    auto ruUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_ENABLED, LOCALIZE_DISABLED, "ru");

    EXPECT_EQ(ruUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("draft"), "Drafts");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("sent"), "Sent");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("spam"), "Spam");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("trash"), "Trash");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("outbox"), "Outbox");

    EXPECT_EQ(ruUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("outbox"), "");
}

// In case of no localize and no rename, use "" which means name from DB as is
TEST(Localization, RU_USER_NORENAME)
{
    auto ruUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_DISABLED, LOCALIZE_DISABLED, "ru");

    EXPECT_EQ(ruUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("draft"), "");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("sent"), "");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("spam"), "");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("trash"), "");
    EXPECT_EQ(ruUserLoc.nameFromSymbol("outbox"), "");

    EXPECT_EQ(ruUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(ruUserLoc.xlistFromSymbol("outbox"), "");
}

//-----------------------------------------------------------------------------
// English users

TEST(Localization, EN_USER_LOCALIZE)
{
    auto enUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_ENABLED, LOCALIZE_ENABLED, "en");

    EXPECT_EQ(enUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(enUserLoc.nameFromSymbol("draft"), "Drafts");
    EXPECT_EQ(enUserLoc.nameFromSymbol("sent"), "Sent");
    EXPECT_EQ(enUserLoc.nameFromSymbol("spam"), "Spam");
    EXPECT_EQ(enUserLoc.nameFromSymbol("trash"), "Trash");
    EXPECT_EQ(enUserLoc.nameFromSymbol("outbox"), "Outbox");

    EXPECT_EQ(enUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(enUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(enUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(enUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(enUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(enUserLoc.xlistFromSymbol("outbox"), "");
}

//-----------------------------------------------------------------------------
// Turkish users

TEST(Localization, TR_USER_LOCALIZE)
{
    auto trUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_ENABLED, LOCALIZE_ENABLED, "tr");

    EXPECT_EQ(trUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(trUserLoc.nameFromSymbol("draft"), "Drafts");
    EXPECT_EQ(trUserLoc.nameFromSymbol("sent"), "Sent");
    EXPECT_EQ(trUserLoc.nameFromSymbol("spam"), "Spam");
    EXPECT_EQ(trUserLoc.nameFromSymbol("trash"), "Trash");
    EXPECT_EQ(trUserLoc.nameFromSymbol("outbox"), "Outbox");

    EXPECT_EQ(trUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("outbox"), "");
}

// In case of no localize, but rename on we should also use english names
TEST(Localization, TR_USER_RENAME)
{
    auto trUserLoc =
        makeLanguageConfig(DEFAULT_LANGUAGE_CONFIG, RENAME_ENABLED, LOCALIZE_DISABLED, "tr");

    EXPECT_EQ(trUserLoc.nameFromSymbol("inbox"), "INBOX");
    EXPECT_EQ(trUserLoc.nameFromSymbol("draft"), "Drafts");
    EXPECT_EQ(trUserLoc.nameFromSymbol("sent"), "Sent");
    EXPECT_EQ(trUserLoc.nameFromSymbol("spam"), "Spam");
    EXPECT_EQ(trUserLoc.nameFromSymbol("trash"), "Trash");
    EXPECT_EQ(trUserLoc.nameFromSymbol("outbox"), "Outbox");

    EXPECT_EQ(trUserLoc.xlistFromSymbol("inbox"), "\\Inbox");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("draft"), "\\Drafts");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("sent"), "\\Sent");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("spam"), "\\Spam");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("trash"), "\\Trash");
    EXPECT_EQ(trUserLoc.xlistFromSymbol("outbox"), "");
}
