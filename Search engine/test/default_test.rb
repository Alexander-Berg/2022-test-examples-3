require 'minitest/spec'

describe_recipe 'disco::test' do
  include MiniTest::Chef::Assertions
  include MiniTest::Chef::Context
  include MiniTest::Chef::Resources

  it 'made web-application announce' do
    node[:disco].wont_be_nil
    node[:disco][:web_application].wont_be_nil
    node[:disco][:web_application].must_be_kind_of Array.class
  end

  it 'merged web-application announces' do
    node[:disco][:web_application].size.must_equal 3
    node[:disco][:web_application].each do |announce|
      announce[:domain].wont_be_nil
      announce[:port].wont_be_nil
    end
  end

end
