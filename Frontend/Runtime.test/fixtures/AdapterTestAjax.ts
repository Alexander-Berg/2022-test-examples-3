import { IContext, IRuntimeSelectOptions, ISnippet } from '../../../typings';
import { Adapter } from '../../..';

interface ITestSnippet extends ISnippet {
    prop: string;
}

type IExpName = 'flag1' | 'flag2';
type IExpVal = string;
type IExpFlag = Record<IExpName, IExpVal>

/**
 * Адаптер только с "ajax"
 */
export class AdapterTestAjax extends Adapter<
    IExpFlag, IExpName, IExpVal, IContext<IExpFlag>, ITestSnippet
> {
    static getSnippetForAjax<IExpFlag>(options: IRuntimeSelectOptions<IExpFlag>): ITestSnippet | null {
        // Условие используется в тесте, когда AdapterTestAjax должен возвращать null
        if (options.preventAjaxMethod !== undefined) {
            return null;
        }

        return { prop: 'ajax' };
    }

    ajax(): string {
        return this.snippet.prop;
    }
}

export function adapterTestAjax(Base: typeof AdapterTestAjax) {
    return class AdapterTestAjax extends Base {
        ajax() {
            return 'exp-' + super.ajax();
        }
    };
}
