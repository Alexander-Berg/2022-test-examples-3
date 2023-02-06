defmodule SolomonHelperTest do
  use ExUnit.Case
  doctest SolomonHelper

  test "greets the world" do
    assert SolomonHelper.hello() == :world
  end
end
