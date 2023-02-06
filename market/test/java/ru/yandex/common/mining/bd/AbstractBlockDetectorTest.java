package ru.yandex.common.mining.bd;

import javax.annotation.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.yandex.common.util.DomUtils;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.XPathUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.html.HtmlUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

public class AbstractBlockDetectorTest {
    private TextGetter textGetter = new DefaultTextGetter();

    public boolean sameBlock(Document where, List<Block> blocks,
                             ElementLocator first, ElementLocator second) {
        Integer firstIndex = null, secondIndex = null;

        Element firstElement = first.getElement(where);
        Element secondElement = second.getElement(where);

        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            for (Element e : b.getTargets()) {
                if (firstElement == e) firstIndex = i;
                if (secondElement == e) secondIndex = i;
            }
        }
        return firstIndex == null ? secondIndex == null :
                firstIndex.equals(secondIndex);
    }

    public boolean differentBlock(Document where, List<Block> blocks,
                                  ElementLocator first, ElementLocator second) {
        return !sameBlock(where, blocks, first, second);
    }

    public boolean existsBlockContaining(Document where, List<Block> blocks,
                                         ElementLocator... locators) {
        return null != findBlockContaining(where, blocks, locators);
    }

    public Block findBlockContaining(Document where, List<Block> blocks,
                                     ElementLocator... locators) {
        List<Element> elements = new ArrayList<Element>();
        for (ElementLocator loc : locators) {
            elements.add(loc.getElement(where));
        }

        for (Block b : blocks) {
            if (b.getTargets().containsAll(elements)) {
                return b;
            }
        }

        return null;
    }

    public boolean contains(Document where, Block b, ElementLocator loc) {
        List<Element> es = loc.getElements(where);
        for (Element e : es) {
            if (b.getTargets().contains(e))
                return true;
        }
        return false;
    }

    protected void setTextGetter(TextGetter textGetter) {
        this.textGetter = textGetter;
    }


    public AbstractElementLocator id(final String id) {
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                return XPathUtils.queryElementList("id('" + id + "')", root);
            }
        };
    }

    public AbstractElementLocator text(final String regex) {
        final Pattern p = Pattern.compile(regex);
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                List<Element> res = new ArrayList<Element>();
                for (Node e : getDescendantsOrSelf(root)) {
                    if (DomUtils.isElement(e) &&
                            p.matcher(textGetter.getText(e)).find())
                    {
                        res.add((Element)e);
                    }
                }
                return res;
            }
        };
    }

    private List<Node> getDescendantsOrSelf(Node root) {
        List<Node> res = new ArrayList<Node>();
        res.add(root);
        for(int p=0; p<res.size(); ++p) {
            NodeList nl = res.get(p).getChildNodes();
            for(int i=0; i<nl.getLength(); ++i) {
                res.add(nl.item(i));
            }
        }
        return res;
    }

    private Map<Pair<Node, String>, List<Element>> TAG_RESULT =
            new HashMap<Pair<Node, String>, List<Element>>();

    public AbstractElementLocator tag(final String tag) {
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                Pair<Node, String> key = new Pair<Node, String>(root, tag);
                List<Element> res = TAG_RESULT.get(key);
                if (res == null) {
                    res = XPathUtils.queryElementList(".//" + tag, root);
                    TAG_RESULT.put(key, res);
                }
                return res;
            }
        };
    }

    public AbstractElementLocator chain(final ElementLocator first, final ElementLocator second) {
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                List<Element> res = new ArrayList<Element>();
                for (Element element : first.getElements(root)) {
                    res.addAll(second.getElements(element));
                }
                return res;
            }
        };
    }

    public AbstractElementLocator filterOnPresence(final AbstractElementLocator outer, final ElementLocator inner) {
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                List<Element> res = new ArrayList<Element>();
                for (Element e : outer.getElements(root)) {
                    if (null != inner.getElement(e))
                        res.add(e);
                }
                return res;
            }
        };
    }

    public AbstractElementLocator filterOnAttribute(
            final AbstractElementLocator loc, final String attribute, final String value) {
        return new AbstractElementLocator() {
            public List<Element> getElements(Node root) {
                List<Element> res = new ArrayList<Element>();
                for (Element e : loc.getElements(root)) {
                    if (e.getAttribute(attribute).equals(value))
                        res.add(e);
                }
                return res;
            }
        };
    }


    public boolean disjoint(Block... bs) {
        Set<Element> seen = new HashSet<Element>();
        for (Block b : bs) {
            for (Element t : b.getTargets()) {
                if (!seen.add(t))
                    return false;
            }
        }
        return true;
    }

    public Block blockOf(ElementLocator loc, BlockDetector bd, Document doc) {
        return bd.findBlock(loc.getElement(doc), doc.getDocumentElement());
    }

    public Document load(String file) throws Exception {
        return load(file, Charset.defaultCharset().name());
    }

    public Document load(String file, String charset) throws IOException {
        try {
            return HtmlUtils.parse(IOUtils.readWholeFile(file, Charset.forName(charset)));
        } catch (Exception e) {
            throw IOUtils.wrapIO(e);
        }
    }

    public abstract class AbstractElementLocator implements ElementLocator {
        public Element getElement(Node root) {
            List<Element> es = getElements(root);
            return es.size() == 0 ? null : es.get(0);
        }

        public AbstractElementLocator id(String id) {
            return chain(this, AbstractBlockDetectorTest.this.id(id));
        }

        public AbstractElementLocator text(String text) {
            return chain(this, AbstractBlockDetectorTest.this.text(text));
        }

        public AbstractElementLocator tag(String tag) {
            return chain(this, AbstractBlockDetectorTest.this.tag(tag));
        }

        public AbstractElementLocator has(ElementLocator locator) {
            return filterOnPresence(this, locator);
        }

        public AbstractElementLocator atr(String attribute, String value) {
            return filterOnAttribute(this, attribute, value);
        }
    }

    public interface ElementLocator {
        @Nullable
        Element getElement(Node root);

        List<Element> getElements(Node root);
    }
}
