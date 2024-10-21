package org.khaleesicodes;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@SuppressWarnings("preview")
public interface MatildaCodeTransformer {
    Predicate<CodeElement> getTransformPredicate();
    CodeTransform getTransform(AtomicBoolean modified);
}
