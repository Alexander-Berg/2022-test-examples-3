package ru.yandex.autotests.direct.httpclient.util.jsonParser.requestParameters;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import ru.yandex.autotests.direct.httpclient.core.IFormParameters;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.JsonStringTransformableCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author alex-samo (alex-samo@yandex-team.ru)
 */
public abstract class AbstractRequestParameters implements IFormParameters {
    private boolean ignoreEmptyParameters = true;
    protected String objectId;

    protected String getFormFieldName(Field field) {
        String name = field.getAnnotation(JsonPath.class).requestPath();
        if(objectId == null) {
            return name;
        }
        return name + "-" + objectId;
    }

    protected void addCollectionsParameters(List<NameValuePair> result) {
        for(Field field : getCollections()) {
            Collection collectionValue;
            try {
                collectionValue = (Collection) PropertyUtils.getProperty(this, field.getName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if(collectionValue == null) {
                continue;
            }
            for (Object collectionItem : collectionValue) {
                ((AbstractRequestParameters) collectionItem).ignoreEmptyParameters(this.ignoreEmptyParameters);
                result.addAll(((AbstractRequestParameters) collectionItem).parameters());
            }
        }
    }

    protected List<Field> getAllFields(List<Field> fields, Class<?> type) {
        Collections.addAll(fields, type.getDeclaredFields());
        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }
        return fields;
    }

    protected List<Field> getCollections() {
        List<Field> result = new ArrayList<>();
        for (Field field : getAllFields(new ArrayList<Field>(), this.getClass())) {
            if(Collection.class.isAssignableFrom(field.getType())) {
                result.add(field);
            }
        }
        return result;
    }

    protected String getProperty(String name) {
        try {
            if (JsonStringTransformableCmdBean.class.isAssignableFrom(PropertyUtils.getProperty(this, name).getClass())) {
                return ((JsonStringTransformableCmdBean)PropertyUtils.getProperty(this, name)).toJson();
            }
            return PropertyUtils.getProperty(this, name).toString();
        } catch (NoSuchMethodException e) {
            throw new BackEndClientParametersException("Field [" + name + "] must have getter", e);
        } catch (Exception e) {
            return "";
        }
    }

    public void ignoreEmptyParameters(boolean ignoreEmptyParameters) {
        this.ignoreEmptyParameters = ignoreEmptyParameters;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public <T extends AbstractRequestParameters> T merge(T basicFormParameters) {
        try {
            PropertyUtils.copyProperties(this, basicFormParameters);
        } catch (Exception e) {
            throw new BackEndClientParametersException("Failed to initialize context", e);
        }
        return (T) this;
    }

    public List<NameValuePair> parameters() {
        List<NameValuePair> result = new ArrayList<>();
        for (Field field : getAllFields(new LinkedList<Field>(), this.getClass())) {
            if (field.isAnnotationPresent(JsonPath.class)) {
                String value = getProperty(field.getName());
                if(ignoreEmptyParameters && value.equals("")) {
                    continue;
                }
                result.add(new BasicNameValuePair(getFormFieldName(field), value));
            }
        }
        addCollectionsParameters(result);
        return result;
    }

    @Override
    public String toString() {
        return parameters().toString();
    }

}
