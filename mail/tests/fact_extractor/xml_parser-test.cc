#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/fact_extractor/xml_parser.h>

namespace {

using namespace msg_body;
using namespace testing;

struct XmlParserTest : public Test {
    typedef xml::Tree::value_type Node;
    typedef xml::Tree Tree;
    static Node & addChild( Node & parent, const Node & child ) {
        parent.second.push_back(child);
        return parent;
    }
    static Node & addChild( Node & parent, const std::string & name, const std::string & value) {
        return addChild( parent, Node(name, Tree(value) ) );
    }
    static Node & addChild( Node & parent, const std::string & name) {
        return addChild( parent, Node(name, Tree() ) );
    }

    static Node createExampleTypeMsg() {
        Node typeMsg("type-msg", Tree());
        addChild(typeMsg, "id", "event");

        Node include("include", Tree());
        addChild(include, "type", "4");
        addChild(include, "type", "2");
        addChild(typeMsg, include);

        Node exclude("exclude", Tree());
        addChild(exclude, "type", "6");
        addChild(typeMsg, exclude);

        return typeMsg;
    }
};

TEST_F( XmlParserTest, Type_get_returnsTypeValue ) {
    Node node("type", Tree("10"));

    EXPECT_EQ( xml::Type(node).get(), 10 );
}

TEST_F( XmlParserTest, Types_get_returnsTypeValueForEachTag) {
    Node node("include", Tree());
    addChild(node, "type", "4");
    addChild(node, "type", "6");

    EXPECT_EQ( xml::Types(node).get().size(), 2ul );
}

TEST_F( XmlParserTest, TypeMsg_include_returnsIncludeTypeWrappers ) {
    Node includeNode("include", Tree());
    addChild(includeNode, "type", "4");
    addChild(includeNode, "type", "2");
    Node node("type-msg", Tree());
    addChild(node, includeNode);

    EXPECT_EQ( xml::TypeMsg(node).include().get().size(), 2ul );
}

TEST_F( XmlParserTest, TypeMsg_exclude_returnsExcludeTypeWrappers ) {
    Node excludeNode("exclude", Tree());
    addChild(excludeNode, "type", "4");
    addChild(excludeNode, "type", "2");
    Node node("type-msg", Tree());
    addChild(node, excludeNode);

    EXPECT_EQ( xml::TypeMsg(node).exclude().get().size(), 2ul );
}

TEST_F( XmlParserTest, TypeMsg_ids_returnsIdsWrapper ) {
    Node node("type-msg", Tree());
    addChild(node, "id", "event");
    addChild(node, "id", "ticket");

    EXPECT_EQ( xml::TypeMsg(node).ids().size(), 2ul );
}

}
