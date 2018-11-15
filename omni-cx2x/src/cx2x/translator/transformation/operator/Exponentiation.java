package cx2x.translator.transformation.operator;

import cx2x.translator.language.base.ClawLanguage;
import cx2x.translator.transformation.ClawTransformation;
import cx2x.translator.transformation.utility.CrClimHelper;
import cx2x.xcodeml.exception.IllegalTransformationException;
import cx2x.xcodeml.helper.XnodeUtil;
import cx2x.xcodeml.transformation.Transformation;
import cx2x.xcodeml.transformation.Transformer;
import cx2x.xcodeml.xnode.*;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Replace the usage of the exponentiation operator (**) by a function call and add the USE statement
 * to import the module containing the function. The name of the function and the module are a class
 * fields.
 *
 * Created by Christophe Charpilloz on 05.07.17.
 */
public class Exponentiation extends ClawTransformation {
    private final String usageModuleName;
    private final String powFunctionName ;

    public static Exponentiation defaultPow(ClawLanguage directive) {
        return new Exponentiation(directive, "exponentiation", "portable_pow");
    }

    public Exponentiation(ClawLanguage directive, String moduleName, String funcName) {
        super(directive);
        usageModuleName = moduleName;
        powFunctionName = funcName;
    }

    @Override
    public boolean analyze(XcodeProgram xcodeml, Transformer transformer) {
        return true;
    }

    @Override
    public boolean canBeTransformedWith(XcodeProgram xcodeml, Transformation other) {
        return false;
    }

    /**
     * Replace the usage of the exponentiation operator node by a function call node.
     *
     * @param fPow    the ** operator node.
     * @param xcodeml the context.
     * @param fctType a dummy function type.
     * @throws IllegalTransformationException
     */
    private void replaceExponentiation(
        Xnode fPow,
        XcodeProgram xcodeml,
        String fctType
    ) throws IllegalTransformationException {
        int nChildren = fPow.children().size();
        if (nChildren != 2)
            throw new IllegalTransformationException(
                    "Unexpected number of arguments: " + nChildren, fPow.lineNo()
            );

        Xnode functionCall = xcodeml.createFctCall(
                Xname.TYPE_F_REAL,
                powFunctionName,
                fctType
        );
        Xnode argument = functionCall.matchDescendant(Xcode.ARGUMENTS);
        argument.append(fPow.child(0), true);
        argument.append(fPow.child(1), true);
        fPow.insertAfter(functionCall);
        XnodeUtil.safeDelete(fPow);
    }

    @Override
    public void transform(
        XcodeProgram xcodeml,
        Transformer transformer,
        Transformation other
    ) throws IllegalTransformationException {
        Set<Element> modModules = new HashSet<>();

        // get the exponentiation operator (**) usage
        List<Xnode> fPowers = xcodeml.matchAll(Xcode.FPOWEREXPR);
        // if there is at least one usage we try to replace it
        if (! fPowers.isEmpty()) {
            // get dummy function types in case we need to replace an
            // operator by a function call
            String fctType = CrClimHelper.addDummyFctType(xcodeml);
            for (Xnode fPow : fPowers) {
                Optional<Xnode> optModule = CrClimHelper.getModule(fPow);
                if (optModule.isPresent()) {
                    Xnode module = optModule.get();
                    replaceExponentiation(fPow, xcodeml, fctType);
                    if (!modModules.contains(module.element())) {
                        // add the use statement
                        modModules.add(module.element());
                        CrClimHelper.addUseStatement(module, usageModuleName, xcodeml);
                    }
                } else {
                    throw new IllegalTransformationException(
                        "Impossible to find program, module, function or subroutine"
                    );
                }
            }
        }
    }
}
