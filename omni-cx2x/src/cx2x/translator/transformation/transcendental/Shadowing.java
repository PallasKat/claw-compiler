package cx2x.translator.transformation.transcendental;

import cx2x.translator.language.base.ClawLanguage;
import cx2x.translator.transformation.ClawTransformation;
import cx2x.translator.transformation.utility.CrClimHelper;
import cx2x.xcodeml.transformation.Transformation;
import cx2x.xcodeml.transformation.Transformer;
import cx2x.xcodeml.xnode.Xcode;
import cx2x.xcodeml.xnode.XcodeProgram;
import cx2x.xcodeml.xnode.Xnode;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Add import statement (USE) when given transcendental functions are used. This is for shadowing purpose.
 * The modules to import and functions to shadow are listed in a map declared as a class field. Hence for
 * now this information is hardcoded.
 */
public class Shadowing extends ClawTransformation {
    private Map<String,Set<String>> transcendentalMap = new HashMap<String,Set<String>>() {
        {
            put("trigo",          new HashSet<>(Arrays.asList("cos", "sin", "sind", "tan")));
            put("inversetrigo",   new HashSet<>(Arrays.asList("acos", "asin", "atan")));
            put("hyperbolic",     new HashSet<>(Arrays.asList("cosh", "sinh", "tanh")));
            put("transcendental", new HashSet<>(Arrays.asList("exp", "log", "log10", "sqrt")));
        }
    };

    private Map<String,String> reversedTranscendentalMap = new HashMap<>();

    public Shadowing(ClawLanguage directive) {
        super(directive);
        // reversing the map is done by pure laziness as the forward one declared as
        // a filed is "copied" from the old JSON config file
        for (Map.Entry<String, Set<String>> entry : transcendentalMap.entrySet()) {
            String moduleName = entry.getKey();
            Set<String> transcendentalNames = entry.getValue();
            for (String funcName : transcendentalNames) {
                reversedTranscendentalMap.put(funcName.toUpperCase(), moduleName);
            }
        }
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
    public void transform(XcodeProgram xcodeml, Transformer transformer, Transformation other) throws Exception {
        Map<Element, Set<String>> modModules = new HashMap<>();

        List<Xnode> fFuncCalls = xcodeml.matchAll(Xcode.FUNCTIONCALL);
        if (! fFuncCalls.isEmpty()) {
            for (Xnode fFCall : fFuncCalls) {
                String funcName = fFCall.matchDescendant(Xcode.NAME).value().toUpperCase();

                if (reversedTranscendentalMap.containsKey(funcName)) {
                    String moduleName = reversedTranscendentalMap.get(funcName);
                    Xnode currentModule = CrClimHelper.getModule(fFCall).get();
                    Element elem = currentModule.element();
                    if (! modModules.containsKey(elem)) {
                        modModules.put(elem, new HashSet<>());
                    }

                    if (! modModules.get(elem).contains(moduleName)) {
                        modModules.get(elem).add(moduleName);
                        CrClimHelper.addUseStatement(currentModule, moduleName, xcodeml);
                    }
                }
            }
        }
    }
}
