package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.*;

import java.util.ArrayList;

public class CategoryParser extends StackableElementOrientedSAXHandler<Category> {
    
    Category category;
    private ModelParser modelParser;
    private ModelIdParser modelIdParser;
    private ModelsCountParser modelsCountParser;

    public void setModelParser(ModelParser modelParser) {
        this.modelParser = modelParser;
    }

    public void setModelIdParser(ModelIdParser modelIdParser) {
        this.modelIdParser = modelIdParser;
    }

    public void setModelsCountParser(ModelsCountParser modelsCountParser) {
        this.modelsCountParser = modelsCountParser;
    }

    public CategoryParser() {
        addElementListener("/response/category", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                category = new Category();
                category.models = new ArrayList<Model>();
                category.modelIds = new ArrayList<Integer>();
                category.id = Integer.parseInt(attributes.getValue("id"));
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
        addElementValueListener("/response/category/name", new ElementValueListener() {
            @Override
            public void onValue(ElementOrientedSAXHandler handler, String value) throws SAXParseException {
                category.name = value;
            }
        });
        addElementValueListener("/response/category/type", new ElementValueListener() {
            @Override
            public void onValue(ElementOrientedSAXHandler handler, String value) throws SAXParseException {
                category.type = value;
            }
        });
        addInnerParser("/response/category/models", new InnerParser<Integer>() {
            @Override
            public StackableElementOrientedSAXHandler<Integer> getParser() {
                return modelsCountParser;
            }

            @Override
            public void parsed(Integer parsed) {
                category.modelsCount = modelsCountParser.getParsed();
            }
        });
        addInnerParser("/response/category/models/model", new InnerParser<Model>() {
            @Override
            public StackableElementOrientedSAXHandler<Model> getParser() {
                return modelParser;
            }

            @Override
            public void parsed(Model parsed) {
                category.models.add(parsed);
            }
        });
        addInnerParser("/response/category/models/model", new InnerParser<Integer>() {
            @Override
            public StackableElementOrientedSAXHandler<Integer> getParser() {
                return modelIdParser;
            }

            @Override
            public void parsed(Integer parsed) {
                category.modelIds.add(parsed);
            }
        });
    }

    @Override
    public Category getParsed() {
        return category;
    }
    
}
