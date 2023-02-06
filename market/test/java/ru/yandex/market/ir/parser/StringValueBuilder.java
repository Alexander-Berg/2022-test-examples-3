package ru.yandex.market.ir.parser;

import ru.yandex.market.ir.parser.matcher.ComplexMatchResult;
import ru.yandex.market.ir.parser.matcher.MatchResult;
import ru.yandex.market.ir.parser.matcher.ValueMatchResult;
import ru.yandex.utils.string.indexed.IndexedString;

/**
 * @author Andrey Styskin, <a href="mailto:styskin@yandex-team.ru"/>
 */
public class StringValueBuilder implements ValueBuilder<String> {
    @Override
    public String build(ParsedEntity parsedEntity, IndexedString indexedString) {
        StringBuilder sb = new StringBuilder();
        makeString(parsedEntity, indexedString, sb, 1);
        return sb.toString();
    }

    private void makeString(ParsedEntity parsedEntity, IndexedString indexedString, StringBuilder sb, int level) {
        for (MatchResult mr : parsedEntity.getMatchResults()) {
            if (mr instanceof ComplexMatchResult) {
                ComplexMatchResult cmr = (ComplexMatchResult) mr;
                if (level == 2) {
                    sb.append(parsedEntity.getEntity().getName()).append(":@\t");
                }
                String delimiter = " ";
                switch (level) {
                    case 1:
                        delimiter = ";\n";
                        break;
                    case 2:
                        delimiter = " - ";
                        break;
                    default:
                        break;
                }
                boolean was = false;
                for (ParsedEntity pe : cmr.getParsedWithin()) {
                    if (pe.isMatched()) {
                        was = true;
                        makeString(pe, indexedString, sb, level + 1);
                        sb.append(delimiter);
                    }
                }
                if (was) {
                    sb.delete(sb.length() - delimiter.length(), sb.length());
                }
            } else if (mr instanceof ValueMatchResult) {
                ValueMatchResult vmr = (ValueMatchResult) mr;
                String entryString = vmr.getEntryString(indexedString);
                sb.append(entryString).append("(").append(vmr.getPosition()).append(")");
            }
        }
    }


}
