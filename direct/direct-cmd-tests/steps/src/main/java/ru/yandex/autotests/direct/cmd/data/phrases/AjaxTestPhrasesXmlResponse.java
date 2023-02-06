package ru.yandex.autotests.direct.cmd.data.phrases;

import ru.yandex.autotests.direct.cmd.data.XmlResponse;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="root")
public class AjaxTestPhrasesXmlResponse extends XmlResponse {
    List<String> phrase;

    @XmlElement
    public void setPhrase(List<String> phrase) {
        this.phrase = phrase;
    }

    public List<String> getPhrase() {
        return phrase;
    }

    public AjaxTestPhrasesXmlResponse withPhrase(List<String> phrase) {
        this.phrase = phrase;
        return this;
    }

}
