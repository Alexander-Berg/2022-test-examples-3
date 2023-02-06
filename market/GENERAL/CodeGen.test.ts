import CodeGen from './CodeGen';

describe('CodeGen', () => {
  it('should calculate relative imports', () => {
    expect(CodeGen.resolveRelativeImport('a/b', 'a/c')).toEqual('./c');
    expect(CodeGen.resolveRelativeImport('a/b', 'a/b/c')).toEqual('./b/c');
    expect(CodeGen.resolveRelativeImport('a/b/c/d', 'a/b/e')).toEqual('../e');
    expect(CodeGen.resolveRelativeImport('a/b/c/d', 'a/b/e/f')).toEqual('../e/f');
    expect(CodeGen.resolveRelativeImport('a/b/c', 'a/b')).toEqual('../b');
    expect(CodeGen.resolveRelativeImport('a/b/c/d', 'a/b')).toEqual('../../b');
    expect(CodeGen.resolveRelativeImport('a/b', 'a/b')).toEqual('./b');
    expect(CodeGen.resolveRelativeImport('a/b', 'a/c/d')).toEqual('./c/d');
  });
});
