#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/disk_attach_builder.h>

using namespace testing;

namespace sendbernar {

struct DiskAttachBuilderTest : public Test {
    DiskAttachBuilderTest() :
        Test(),
        builder(DiskConfig{"http://disk.host", "http://preview.host"}) {}

    DiskAttachBuilder builder;
};

TEST_F(DiskAttachBuilderTest, should_return_empty_string_on_no_attaches) {
    ASSERT_EQ(builder.build({}), "");
}

TEST_F(DiskAttachBuilderTest, should_make_attach_html_with_disk_url_and_name) {
    params::DiskAttach att {"/path_to_disk", "filename", boost::none,
                            boost::none, boost::none, boost::none};

    ASSERT_EQ(builder.build({att}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk\">"
              "filename"
              "</a><br>");
}

TEST_F(DiskAttachBuilderTest, should_add_encoded_preview_url_if_exists) {
    params::DiskAttach att {"/path_to_disk", "filename", boost::none,
                            boost::make_optional<std::string>("/path_to_preview"),
                            boost::none, boost::none};

    ASSERT_EQ(builder.build({att}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk\" "
              "data-preview=\"http%3A%2F%2Fpreview.host%2Fpath_to_preview\">"
              "filename"
              "</a><br>");
}

TEST_F(DiskAttachBuilderTest, should_add_size_if_exists_and_ignore_folder) {
    params::DiskAttach att {"/path_to_disk", "filename",
                            boost::none, boost::none,
                            boost::make_optional<std::size_t>(666),
                            boost::make_optional<std::string>("folder")};

    ASSERT_EQ(builder.build({att}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk\">"
              "filename (666)"
              "</a><br>");
}

TEST_F(DiskAttachBuilderTest, should_add_folder_if_exists) {
    params::DiskAttach att {"/path_to_disk", "filename",
                            boost::none, boost::none, boost::none,
                            boost::make_optional<std::string>("folder")};

    ASSERT_EQ(builder.build({att}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk\">"
              "filename (folder)"
              "</a><br>");
}

TEST_F(DiskAttachBuilderTest, should_return_all_attaches) {
    params::DiskAttach file {"/path_to_disk_first", "filename", boost::none,
                             boost::make_optional<std::string>("/path_to_preview"),
                             boost::make_optional<std::size_t>(666),
                             boost::none};
    params::DiskAttach dir {"/path_to_disk_second", "dirname",
                            boost::none, boost::none, boost::none,
                            boost::make_optional<std::string>("folder")};

    ASSERT_EQ(builder.build({file, dir}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk_first\" "
              "data-preview=\"http%3A%2F%2Fpreview.host%2Fpath_to_preview\">"
              "filename (666)"
              "</a><br>"
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk_second\">"
              "dirname (folder)"
              "</a><br>");
}

TEST_F(DiskAttachBuilderTest, should_make_attach_html_with_hash) {
    params::DiskAttach att {"/path_to_disk", "filename", 
                            boost::make_optional<std::string>("simple_hash"),
                            boost::none, boost::none, boost::none};

    ASSERT_EQ(builder.build({att}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk\" "
              "data-hash=\"simple_hash\">"
              "filename"
              "</a><br>");
}


TEST_F(DiskAttachBuilderTest, should_return_all_attaches_and_some_hash) {
    params::DiskAttach file {"/path_to_disk_first", "filename",
                             boost::make_optional<std::string>("simple_hash"),
                             boost::make_optional<std::string>("/path_to_preview"),
                             boost::make_optional<std::size_t>(666),
                             boost::none};
    params::DiskAttach dir {"/path_to_disk_second", "dirname",
                            boost::none, boost::none, boost::none,
                            boost::make_optional<std::string>("folder")};

    ASSERT_EQ(builder.build({file, dir}),
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk_first\" "
              "data-preview=\"http%3A%2F%2Fpreview.host%2Fpath_to_preview\" "
              "data-hash=\"simple_hash\">"
              "filename (666)"
              "</a><br>"
              "<a class=\"narod-attachment\" "
              "target=\"_blank\" "
              "rel=\"noopener noreferrer\" "
              "href=\"http://disk.host/path_to_disk_second\">"
              "dirname (folder)"
              "</a><br>");
}

}
