import styles from './module3.css';
import module2, {moduleConst} from './module2';

export * from './module1';

export default {
    module: 'module3',
    module2: [module2, moduleConst],
    styles,
};
