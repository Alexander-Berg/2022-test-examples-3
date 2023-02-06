import { MBTTest } from '../../mbt/mbt-test'

export interface MBTTestGenerator {

  generateTests(): MBTTest[]

}
