package ru.yandex.autotests.direct.httpclient.util.jsonParser;

import com.google.gson.Gson;
import com.googlecode.jxquery.BlankClass;
import com.googlecode.jxquery.creator.FieldCreator;
import com.googlecode.jxquery.utils.ArrayHelper;
import com.googlecode.jxquery.utils.ClassHelper;
import com.googlecode.jxquery.utils.Constants;
import com.googlecode.jxquery.utils.LogWrapper;
import com.googlecode.jxquery.utils.ReflectionHelper;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.yandex.autotests.direct.httpclient.data.PlainResponseMapBean;
import ru.yandex.autotests.direct.httpclient.data.ResponseMapBean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.02.15
 */
public class JsonPathXMLPopulater {

    private static LogWrapper log = new LogWrapper(
            LogFactory.getLog(JsonPathXMLPopulater.class));

    protected static <Vo extends Object> Vo eval(String parentNodePath, String xml, Vo vo, boolean fromJSON,
                                                 BeanType beanType) throws ParserConfigurationException,
            SAXException, IOException, XPathExpressionException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc = db.parse(new InputSource(new ByteArrayInputStream(xml
                .getBytes("utf-8"))));
        return eval(parentNodePath, doc, vo, fromJSON, beanType);
    }

    private static Object createValue(JsonPath xqf, Object org) {

        return createValue(xqf.creator(), org);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createValue(Class<?> creatorClass, Object org) {

        T value = null;
        try {
            if (!creatorClass.equals(BlankClass.class)) {
                FieldCreator<T> creator = (FieldCreator<T>) creatorClass
                        .newInstance();
                value = creator.create(org);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    protected static <Vo extends Object, Attr extends Object> Vo eval(String parentNodePath, Node xml, Vo vo, boolean fromJSON,
                                                                      BeanType beanType) throws XPathExpressionException {
        log.debug("evaluate object:" + vo);

        Field[] fields = ReflectionHelper.getAllFields(vo.getClass(),
                JsonPath.class);
        for (Field f : fields) {
            JsonPath xQuery = f.getAnnotation(JsonPath.class);
            PathQuerierResult pathQuerierResult = JsonPathXQuerier.get(parentNodePath, xml, f, xQuery, fromJSON, beanType);
            if (null != pathQuerierResult && pathQuerierResult.getResult() != null) {
                Object result = pathQuerierResult.getResult();
                try {
                    Object value = createValue(xQuery, result);
                    if (null != value) {
                        BeanUtils.setProperty(vo, f.getName(), value);
                    } else {
                        if (result instanceof Node) {
                            if (ClassHelper.isLangType(f.getType())) {
                                BeanUtils.setProperty(vo, f.getName(),
                                        ((Node) result).getTextContent());
                            } else if (JSONObject.class.equals(f.getType())) {
                                if (StringUtils
                                        .isNotBlank(((Element) result)
                                                .getAttribute(Constants.XML_EMBEDDED_JSON))) {

                                    JSONObject jObj = JSONObject
                                            .fromObject(((Element) result)
                                                    .getAttribute(Constants.XML_EMBEDDED_JSON));
                                    BeanUtils
                                            .setProperty(vo, f.getName(), jObj);
                                }

                            } else if (ResponseMapBean.class.isAssignableFrom(f.getType())) {
                                String res = ((Element) result)
                                        .getAttribute(Constants.XML_EMBEDDED_JSON).replaceAll("\'a([0-9a-zA-Z._-]+)\':", "\"$1\":");
                                BeanUtils.setProperty(vo, f.getName(), new Gson().fromJson(res, f.getType()));
                            } else if (PlainResponseMapBean.class.isAssignableFrom(f.getType())) {
                                String res = "{\"" + f.getName() + "\":" + ((Element) result)
                                        .getAttribute(Constants.XML_EMBEDDED_JSON).replaceAll("\'a([0-9a-zA-Z._-]+)\':", "\"$1\":") + "}";
                                BeanUtils.setProperty(vo, f.getName(), new Gson().fromJson(res, f.getType()));
                            } else {
                                Object attr = f.getType().newInstance();
                                eval(pathQuerierResult.getParentNodePath(), (Node) result, attr, fromJSON, beanType);
                                BeanUtils.setProperty(vo, f.getName(), attr);
                            }
                        } else if (result instanceof NodeList) {
                            NodeList nl = (NodeList) result;

                            List<Attr> attr = new ArrayList<Attr>();
                            Class<Attr> attrType = (Class<Attr>) ReflectionHelper
                                    .getFieldComponentType(f);

                            for (int i = 0; i < nl.getLength(); i++) {
                                Node n = nl.item(i);
                                if (ClassHelper.isLangType(attrType)) {
                                    attr.add((Attr) n.getTextContent());
                                } else if (JSONObject.class.equals(attrType)) {
                                    if (StringUtils
                                            .isNotBlank(((Element) n)
                                                    .getAttribute(Constants.XML_EMBEDDED_JSON))) {
                                        attr.add((Attr) JSONObject.fromObject(((Element) n)
                                                .getAttribute(Constants.XML_EMBEDDED_JSON)));
                                    }
                                } else if (ResponseMapBean.class.isAssignableFrom(attrType)) {
                                    String res = ((Element) n)
                                            .getAttribute(Constants.XML_EMBEDDED_JSON).replaceAll("\'(a[0-9a-zA-Z._-]+)\':", "\"$1\":");
                                    attr.add((Attr) new Gson().fromJson(res, attrType));
                                } else {
                                    attr.add((Attr) eval(pathQuerierResult.getParentNodePath(), n,
                                            attrType.newInstance(), fromJSON, beanType));
                                }
                            }

                            if (f.getType().isArray()) {
                                if (ClassHelper.isLangType(attrType)) {
                                    BeanUtils.setProperty(vo, f.getName(),
                                            attr.toArray());
                                } else {
                                    BeanUtils.setProperty(vo, f.getName(),
                                            ArrayHelper.createArray(attr,
                                                    attrType));
                                }
                            } else {
                                BeanUtils.setProperty(vo, f.getName(), attr);
                            }
                        }
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
        return vo;
    }
}
