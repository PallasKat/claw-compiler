package cx2x.translator.transformation.transcendental;

import cx2x.translator.language.base.ClawLanguage;
import cx2x.translator.transformation.ClawTransformation;
import cx2x.translator.transformation.utility.CrClimHelper;
import cx2x.xcodeml.transformation.Transformation;
import cx2x.xcodeml.transformation.Transformer;
import cx2x.xcodeml.xnode.Xcode;
import cx2x.xcodeml.xnode.XcodeProgram;
import cx2x.xcodeml.xnode.Xnode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Add import statement (USE) when given transcendental functions are used. This is for shadowing purpose.
 * The modules to import and functions to shadow are listed in a map declared as a class field. Hence for
 * now this information is hardcoded.
 */
public class AddFuncPrefix extends ClawTransformation {
    private final String prefix;
    private final Set<String> functions;
    private final Set<String> modules;

    public AddFuncPrefix(
        ClawLanguage directive,
        String prefix,
        Set<String> functions,
        Set<String> modules
    ) {
        super(directive);
        this.prefix = prefix;
        this.functions = functions;
        this.modules = modules;
    }

    @Override
    public boolean analyze(XcodeProgram xcodeml, Transformer transformer) {
        return true;
    }

    @Override
    public boolean canBeTransformedWith(XcodeProgram xcodeml, Transformation other) {
        return false;
    }

    @Override
    public void transform(
        XcodeProgram xcodeml,
        Transformer transformer,
        Transformation other
    ) {
        Stream<Xnode> modProgFuncSub = xcodeml.matchAll(Xcode.FUNCTIONCALL).stream()
            .filter(
                x -> functions.contains(x.matchDescendant(Xcode.NAME).value())
            ).map(
                y -> {
                    Xnode nameName = y.matchDescendant(Xcode.NAME);
                    nameName.setValue(prefix + nameName.value());
                    return CrClimHelper.getModule(y);
                }
            ).filter(o -> o.isPresent()).map(o -> o.get());

        modProgFuncSub.collect(Collectors.toSet())
            .forEach(x -> CrClimHelper.addUseStatements(x, modules, xcodeml));
    }
}
