def test_templates_can_be_loaded_and_rendered(factory):
    result = factory["sample.j2"].render()
    assert result == "I fear ninjas."


def test_template_factory_can_handle_nested_templates(factory):
    result = factory["folder/nested_sample.j2"].render(stuff="STUFF")
    assert result == "I fear all kinds of invisible STUFF."


def test_template_factory_supports_globals(factory):
    factory.globals["stuff"] = "STUFF"
    result = factory["folder/nested_sample.j2"].render()
    assert result == "I fear all kinds of invisible STUFF."


def test_filters_can_be_registered_using_decorator(factory):

    @factory.filters.item("beep_it")
    def beep_it(text):
        return text.replace("fuck", "f*ck")

    result = factory["sample_with_filter.j2"].render()
    assert result == "Ninjas are f*cking scary!"
